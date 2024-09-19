/*
 * Copyright (c) 2023-2024 RKDAW Enterprises and Ralph Williamson.
 *       email: rkdawenterprises@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ddns.rkdawenterprises.rkdawe_webapp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ddns.rkdawenterprises.rkdawe_api_common.Check_CRC;
import net.ddns.rkdawenterprises.rkdawe_api_common.Utilities;
import net.ddns.rkdawenterprises.rkdawe_api_common.WSD_exception;
import net.ddns.rkdawenterprises.rkdawe_api_common.Weather_data;
import net.ddns.rkdawenterprises.rkdawe_api_common.Utilities.tm;

/**
 * Singleton with synchronized access.
 */
public final class Weather_station_access
{
    public static final Duration MAX_TIME_DELTA = Duration.ofSeconds( 5 );

    private static Weather_station_access s_instance = null;

    /**
     * To prevent threading issues, always synchronize on this object for accessing weather station.
     */
    private Discovery_info m_discovery = null;

    /**
     * Discovery information from the weather station.
     */
    public static final class Discovery_info
    {
        public String host;
        public int port;
        public byte[] discovery_data;
        public String DID;

        public Discovery_info()
        {
        }

        public Discovery_info( String host,
                               int port,
                               byte[] discovery_data,
                               String DID )
        {
            this.host = host;
            this.port = port;
            this.discovery_data = discovery_data;
            this.DID = DID;
        }

        public static String serialize_to_JSON( Discovery_info object )
        {
            Gson gson = new GsonBuilder().disableHtmlEscaping()
                                         .setPrettyPrinting()
                                         .create();
            return gson.toJson( object );
        }

        public static Discovery_info deserialize_from_JSON( String string_JSON )
        {
            Discovery_info object = null;
            try
            {
                Gson gson = new GsonBuilder().disableHtmlEscaping()
                                             .setPrettyPrinting()
                                             .create();
                object = gson.fromJson( string_JSON,
                                        Discovery_info.class );
            }
            catch( com.google.gson.JsonSyntaxException exception )
            {
                System.out.println( "Bad data format for Discovery_info: " + exception );
                System.out.println( ">>>" + string_JSON + "<<<" );
            }

            return object;
        }

        public String serialize_to_JSON()
        {
            return serialize_to_JSON( this );
        }
    }

    private Weather_station_access()
    {
    }

    public static Weather_station_access get_instance()
    {
        if( s_instance == null )
        {
            s_instance = new Weather_station_access();
            s_instance.m_discovery = new Discovery_info();
            try
            {
                // Weather_history.weather_record_file_maintenance();
            }
            catch( Exception exception )
            {
                System.out.println( "Issue with weather history file. Verify path exists and has tomcat:tomcat ownership and proper permissions (including selinux): \n"
                        + exception );
            }
        }

        return s_instance;
    }

    public void initialize() throws SocketException, UnknownHostException, WSD_exception, IOException, InterruptedException
    {
        synchronized( m_discovery )
        {
            m_discovery.host = null;
            find( m_discovery );

            if( ( m_discovery.host != null ) && ( m_discovery.host.length() > 0 ) )
            {
                System.out.println( "Testing weather station..." );
                test_verify( m_discovery );
                System.out.println( "Verifying and updating weather station time..." );
                verify_update_time( m_discovery );
                System.out.println( "Get first weather data sample..." );
                get_weather_data();
                System.out.println( "Weather station initialization finished." );
            }
        }
    }

    public Weather_data get_weather_data() throws IOException, InterruptedException, WSD_exception
    {
        return get_weather_data( m_discovery );
    }

    /**
     * Finds a weather station. Assumes there is only one.
     * 
     * @param discovery Updated with discovered weather station.
     * 
     * @param shutdown
     *
     * @return void
     *
     * @throws WSD_exception
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IOException
     */
    private void find( Discovery_info discovery )
            throws WSD_exception, SocketException, UnknownHostException, IOException
    {
        int port = 22222;
        byte[] discovery_bytes = "discoverwlip".getBytes( StandardCharsets.US_ASCII );
        byte[] response_bytes = "discoverwlip".getBytes( StandardCharsets.US_ASCII );

        byte[] receive_buffer = new byte[64];
        DatagramPacket receive_datagram_packet = new DatagramPacket( receive_buffer,
                                                                     receive_buffer.length );

        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        do
        {
            try
            {
                System.out.format( "Find: Sending discovery broadcast...%n" );
                Utilities.send_UDP_broadcast( discovery_bytes,
                                              port,
                                              receive_datagram_packet );
            }
            catch( SocketTimeoutException exception )
            {
                retries--;
                System.err.format( "Find: Timeout waiting for response, retry %d of %d...%n",
                                   -( retries - Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT ),
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT );
                continue;
            }

            byte[] receive_data = receive_datagram_packet.getData();

            if( ( Bytes.indexOf( receive_data,
                                 response_bytes ) != 0 )
                    || ( receive_data.length < ( response_bytes.length + 6 ) ) )
            {
                retries--;
                System.err.format( "Find: Invalid response, retry %d of %d...%n",
                                   -( retries - Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT ),
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT );
                continue;
            }

            int device_DID_index = discovery_bytes.length;
            byte[] device_DID_bytes = Utilities.subbytearray( receive_data,
                                                              device_DID_index,
                                                              device_DID_index + 6 );
            StringBuilder string_builder = new StringBuilder();
            for( byte b : device_DID_bytes )
            {
                string_builder.append( String.format( "%02X:",
                                                      b ) );
            }
            string_builder.setLength( Math.max( string_builder.length() - 1,
                                                0 ) );
            String device_DID = new String( string_builder );

            discovery.host = receive_datagram_packet.getAddress()
                                                    .getHostAddress();
            discovery.port = receive_datagram_packet.getPort();
            discovery.DID = device_DID;
            discovery.discovery_data = discovery_bytes;

            System.out.printf( "Found weather station at %s:%d, ID=%s.%n",
                               discovery.host,
                               discovery.port,
                               discovery.DID );

            return;
        }
        while( retries > 0 );

        System.err.format( "Find: Error count exceeded.%n" );

        discovery.host = null;

        throw new WSD_exception( "Could not find a weather station" );
    }

    private void test_verify( Discovery_info discovery ) throws WSD_exception
    {
        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        do
        {
            try
            {
                SocketAddress socketAddress = new InetSocketAddress( discovery.host,
                                                                     discovery.port );
                try( Socket socket = new Socket() )
                {
                    socket.connect( socketAddress,
                                    (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );
                    try( DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                            DataInputStream in = new DataInputStream( new BufferedInputStream( socket.getInputStream() ) ); )
                    {
                        socket.setSoTimeout( (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );

                        test_verify( in,
                                     out );
                    }
                }

                return;
            }
            catch( Exception exception )
            {
                retries--;
                System.err.format( "Configure: Issue validating weather station, retry %d of %d...%n",
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT - retries,
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT );
                continue;
            }
        }
        while( retries > 0 );

        throw new WSD_exception( "Could not configure weather station" );
    }

    private void verify_update_time( Discovery_info discovery ) throws WSD_exception
    {
        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        do
        {
            try
            {
                SocketAddress socketAddress = new InetSocketAddress( discovery.host,
                                                                     discovery.port );
                try( Socket socket = new Socket() )
                {
                    socket.connect( socketAddress,
                                    (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );
                    try( DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                            DataInputStream in = new DataInputStream( new BufferedInputStream( socket.getInputStream() ) ); )
                    {
                        socket.setSoTimeout( (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );

                        verify_update_time( in,
                                            out );
                    }
                }

                return;
            }
            catch( Exception exception )
            {
                retries--;
                System.err.format( "Configure: Issue configuring weather station time, retry %d of %d...%n",
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT - retries,
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT );
                continue;
            }
        }
        while( retries > 0 );

        throw new WSD_exception( "Could not configure weather station" );
    }

    /**
     * Gets the weather data with a pre-connect and post-disconnect to the given
     * weather station.
     * 
     * @param discovery The weather station to obtain the data from.
     * 
     * @return The weather data.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws WSD_exception
     */
    private Weather_data get_weather_data( Discovery_info discovery )
            throws IOException, InterruptedException, WSD_exception
    {
        SocketAddress socketAddress = new InetSocketAddress( discovery.host,
                                                             discovery.port );
        try( Socket socket = new Socket() )
        {
            socket.connect( socketAddress,
                            (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );
            try( DataOutputStream out = new DataOutputStream( socket.getOutputStream() );
                    DataInputStream in = new DataInputStream( new BufferedInputStream( socket.getInputStream() ) ); )
            {
                socket.setSoTimeout( (int)Utilities.DEFAULT_NETWORK_TIMEOUT.toMillis() );
                Weather_data weather_data = get_weather_data( in,
                                                              out );
                weather_data.DID = discovery.DID;
                return weather_data;
            }
        }
    }

    /**
     * Get various tests and information queries from the station.
     * 
     * @param in  Active inbound stream from weather station.
     * @param out Active outbound stream to weather station.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws WSD_exception
     */
    private void test_verify( DataInputStream in,
                              DataOutputStream out )
            throws IOException, InterruptedException, WSD_exception
    {
        wake( in,
              out );
        test( in,
              out );
        wrd( in,
             out );
        rxcheck( in,
                 out );
        rxtest( in,
                out );
        ver( in,
             out );
        nver( in,
              out );
    }

    private void verify_update_time( DataInputStream in,
                                     DataOutputStream out )
            throws UnknownHostException, IOException, InterruptedException, WSD_exception
    {
        wake( in,
              out );

        configure_time( in,
                        out );

        Duration difference = get_time_difference( in,
                                                   out ).abs();
        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        while( ( MAX_TIME_DELTA.compareTo( difference ) <= 0 ) && retries > 0 )
        {
            retries--;
            synchronize_time( in,
                              out );
            Utilities.sleep( Duration.ofMillis( 1200 ) );
            difference = get_time_difference( in,
                                              out ).abs();
        }

        if( ( MAX_TIME_DELTA.compareTo( difference ) <= 0 ) && retries <= 0 )
        {
            throw new WSD_exception( "Unable to correct station time" );
        }
    }

    private Duration get_time_difference( DataInputStream in,
                                          DataOutputStream out )
            throws IOException, WSD_exception
    {
        ZonedDateTime system_now = ZonedDateTime.now();
        ZonedDateTime station_now = get_station_time( in,
                                                      out );
        Duration difference = Duration.between( station_now,
                                                system_now );

        System.out.printf( "System time = %s,%n Station time = %s,%n difference = %ds%n",
                           system_now,
                           station_now,
                           (int)difference.getSeconds() );

        return difference;
    }

    private ZonedDateTime get_station_time( DataInputStream in,
                                            DataOutputStream out )
            throws IOException, WSD_exception
    {
        tm station_now = new tm();
        get_time( in,
                  out,
                  station_now );
        double offset = (double)station_now.tm_gmtoff / 3600;

        String time_string = String.format( "%04d-%02d-%02dT%02d:%02d:%02d%c%02d:%02d[%s]",
                                            station_now.tm_year + 1900,
                                            station_now.tm_mon + 1,
                                            station_now.tm_mday,
                                            station_now.tm_hour,
                                            station_now.tm_min,
                                            station_now.tm_sec,
                                            ( offset > 0 ) ? '+' : '-',
                                            (int)Math.abs( offset ),
                                            (int)( ( Math.abs( offset ) % 1 ) * 60 ),
                                            station_now.tm_zone );
        return ZonedDateTime.parse( time_string );
    }

    private void configure_time( DataInputStream in,
                                 DataOutputStream out )
            throws UnknownHostException, IOException, InterruptedException, WSD_exception
    {
        tm system_now = Utilities.get_local_tm();

        EEPROM.set_MANUAL_OR_AUTO( in,
                                   out,
                                   (byte)1 );
        EEPROM.set_DAYLIGHT_SAVINGS( in,
                                     out,
                                     (byte)( system_now.tm_isdst ? 1 : 0 ) );
        EEPROM.set_GMT_OR_ZONE( in,
                                out,
                                (byte)1 );
        EEPROM.set_GMT_OFFSET( in,
                               out,
                               system_now.tm_gmtoff );

        tm station_now = new tm();
        get_time( in,
                  out,
                  station_now );

        // System.out.printf( "System time = %d-%d-%d %02d:%02d:%02d %s(%.1f) %s%n",
        // system_now.tm_year + 1900,
        // system_now.tm_mon + 1,
        // system_now.tm_mday,
        // system_now.tm_hour,
        // system_now.tm_min,
        // system_now.tm_sec,
        // system_now.tm_zone,
        // (double)system_now.tm_gmtoff / 3600,
        // ( system_now.tm_isdst ) ? "DST" : "ST" );

        // System.out.printf( "Station time = %d-%d-%d %02d:%02d:%02d %s(%.1f) %s%n",
        // station_now.tm_year + 1900,
        // station_now.tm_mon + 1,
        // station_now.tm_mday,
        // station_now.tm_hour,
        // station_now.tm_min,
        // station_now.tm_sec,
        // system_now.tm_zone,
        // (double)station_now.tm_gmtoff / 3600,
        // ( station_now.tm_isdst ) ? "DST" : "ST" );
    }

    private void synchronize_time( DataInputStream in,
                                   DataOutputStream out )
            throws IOException
    {
        System.out.printf( "System time and Station time differ by %s or more, so updating the station time...%n",
                           Utilities.duration_to_string( MAX_TIME_DELTA ) );

        ZonedDateTime system_now = ZonedDateTime.now();

        set_time( in,
                  out,
                  system_now );
    }

    private int s_wrd = Integer.MAX_VALUE;
    private int s_total_packets_received = Integer.MAX_VALUE;
    private int s_total_packets_missed = Integer.MAX_VALUE;
    private int s_number_of_resynchronizations = Integer.MAX_VALUE;
    private int s_largest_number_packets_received_in_a_row = Integer.MAX_VALUE;
    private int s_number_of_CRC_errors_detected = Integer.MAX_VALUE;
    private String s_firmware_date_code = "N/A";
    private String s_firmware_version = "N/A";

    /**
     * Weather station wake-up procedure as per VantageSerialProtocalDocs_v261.pdf,
     * section V.
     *
     * @param in  Active inbound stream from weather station.
     * @param out Active outbound stream to weather station.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws WSD_exception
     */
    private void wake( DataInputStream in,
                       DataOutputStream out )
            throws IOException, InterruptedException, WSD_exception
    {
        for( int i = 0; i < Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT; i++ )
        {
            out.write( "\n".getBytes( StandardCharsets.US_ASCII ) );

            int expected_response_size = 2;
            byte[] receive_buffer = new byte[32];

            if( ( in.read( receive_buffer,
                           0,
                           expected_response_size ) != expected_response_size )
                    || ( receive_buffer[0] != '\n' ) || ( receive_buffer[1] != '\r' ) )
            {
                Utilities.sleep( Duration.ofMillis( 1200 ) );
                continue;
            }

            return;
        }

        throw new WSD_exception( "Failed to wake up station" );
    }

    private void test( DataInputStream in,
                       DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "TEST\n".getBytes( StandardCharsets.US_ASCII ) );

        byte[] expected_response = "\n\rTEST\n\r".getBytes( StandardCharsets.US_ASCII );
        int expected_response_size = expected_response.length;
        byte[] receive_buffer = new byte[32];

        if( ( in.read( receive_buffer,
                       0,
                       expected_response_size ) != expected_response_size )
                || ( Bytes.indexOf( receive_buffer,
                                    expected_response ) != 0 ) )
        {
            throw new WSD_exception( "TEST command failed" );
        }
    }

    private void wrd( DataInputStream in,
                      DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "WRD".getBytes( StandardCharsets.US_ASCII ) );
        out.write( 0x12 );
        out.write( 0x4D );
        out.write( "\n".getBytes( StandardCharsets.US_ASCII ) );

        int expected_response_size = 2;
        byte[] receive_buffer = new byte[32];

        if( ( in.read( receive_buffer,
                       0,
                       expected_response_size ) != expected_response_size )
                || ( receive_buffer[0] != 0x06 ) || ( receive_buffer[1] != 17 ) )
        {
            throw new WSD_exception( "WRD command failed" );
        }

        s_wrd = receive_buffer[1];
    }

    private void rxcheck( DataInputStream in,
                          DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "RXCHECK\n".getBytes( StandardCharsets.US_ASCII ) );

        int minimum_response_size = 17;
        byte[] receive_buffer = new byte[64];

        Arrays.fill( receive_buffer,
                     0,
                     minimum_response_size * 2,
                     (byte)0 );
        if( in.read( receive_buffer,
                     0,
                     receive_buffer.length ) < minimum_response_size )
        {
            throw new WSD_exception( "RXCHECK (1) command failed" );
        }

        String rxcheck_result = new String( receive_buffer,
                                            StandardCharsets.US_ASCII );
        String regex = "^\\n\\rOK\\n\\r" + "(\\d+)\\s(\\d+)\\s(\\d+)\\s(\\d+)\\s(\\d+)" + "\\n\\r";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( rxcheck_result );
        if( matcher.find() && matcher.groupCount() == 5 )
        {
            s_total_packets_received = Integer.valueOf( matcher.group( 1 ) );
            s_total_packets_missed = Integer.valueOf( matcher.group( 2 ) );
            s_number_of_resynchronizations = Integer.valueOf( matcher.group( 3 ) );
            s_largest_number_packets_received_in_a_row = Integer.valueOf( matcher.group( 4 ) );
            s_number_of_CRC_errors_detected = Integer.valueOf( matcher.group( 5 ) );
        }
        else
        {
            throw new WSD_exception( "RXCHECK (2) command failed" );
        }
    }

    private void rxtest( DataInputStream in,
                         DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "RXTEST\n".getBytes( StandardCharsets.US_ASCII ) );

        byte[] expected_response = "\n\rOK\n\r".getBytes( StandardCharsets.US_ASCII );
        int expected_response_size = expected_response.length;
        byte[] receive_buffer = new byte[32];

        if( ( in.read( receive_buffer,
                       0,
                       expected_response_size ) != expected_response_size )
                || ( Bytes.indexOf( receive_buffer,
                                    expected_response ) != 0 ) )
        {
            throw new WSD_exception( "RXTEST command failed" );
        }
    }

    private void ver( DataInputStream in,
                      DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "VER\n".getBytes( StandardCharsets.US_ASCII ) );

        int minimum_response_size = 18;
        byte[] receive_buffer = new byte[64];

        Arrays.fill( receive_buffer,
                     0,
                     minimum_response_size * 2,
                     (byte)0 );
        if( in.read( receive_buffer,
                     0,
                     receive_buffer.length ) < minimum_response_size )
        {
            throw new WSD_exception( "VER (1) command failed" );
        }

        String result = new String( receive_buffer,
                                    StandardCharsets.US_ASCII );
        String regex = "^\\n\\rOK\\n\\r" + "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s{1,2}"
                + "(\\d+)\\s(\\d+)" + "\\n\\r";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( result );
        if( matcher.find() && matcher.groupCount() == 3 )
        {
            s_firmware_date_code = matcher.group( 1 ) + " " + matcher.group( 2 ) + " " + matcher.group( 3 );
        }
        else
        {
            throw new WSD_exception( "VER (2) command failed" );
        }
    }

    private void nver( DataInputStream in,
                       DataOutputStream out )
            throws IOException, WSD_exception
    {
        out.write( "NVER\n".getBytes( StandardCharsets.US_ASCII ) );

        int minimum_response_size = 11;
        byte[] receive_buffer = new byte[64];

        Arrays.fill( receive_buffer,
                     0,
                     minimum_response_size * 2,
                     (byte)0 );
        if( in.read( receive_buffer,
                     0,
                     receive_buffer.length ) < minimum_response_size )
        {
            throw new WSD_exception( "NVER (1) command failed" );
        }

        String result = new String( receive_buffer,
                                    StandardCharsets.US_ASCII );
        String regex = "^\\n\\rOK\\n\\r" + "(\\d+)\\.(\\d+)" + "\\n\\r";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( result );
        if( matcher.find() && matcher.groupCount() == 2 )
        {
            s_firmware_version = matcher.group( 1 ) + "." + matcher.group( 2 );
        }
        else
        {
            throw new WSD_exception( "NVER (2) command failed" );
        }
    }

    /**
     * Gets the time information from the weather station. Updates the given GNU
     * Broken-down Time structure.
     * 
     * @param in        Active inbound stream from weather station.
     * @param out       Active outbound stream to weather station.
     * @param time_zone The time zone to put into the time structure.
     * @param time      The GNU Broken-down Time structure to update.
     * 
     * @throws IOException
     * @throws WSD_exception
     */
    private void get_time( DataInputStream in,
                           DataOutputStream out,
                           String time_zone,
                           tm time )
            throws IOException, WSD_exception
    {
        out.write( "GETTIME\n".getBytes( StandardCharsets.US_ASCII ) );

        int minimum_response_size = 9;
        byte[] receive_buffer = new byte[32];

        int size_read = in.read( receive_buffer,
                                 0,
                                 receive_buffer.length );

        if( ( size_read < minimum_response_size ) || ( receive_buffer[0] != 0x06 ) )
        {
            throw new WSD_exception( "GETTIME command failed" );
        }

        Check_CRC.check_CRC_16( receive_buffer,
                                1,
                                8 );

        int seconds = receive_buffer[1];
        int minutes = receive_buffer[2];
        int hour = receive_buffer[3];
        int day = receive_buffer[4];
        int month = receive_buffer[5] - 1;
        int year = receive_buffer[6];

        time.tm_sec = seconds;
        time.tm_min = minutes;
        time.tm_hour = hour;
        time.tm_mday = day;
        time.tm_mon = month;
        time.tm_year = year;

        time.tm_gmtoff = EEPROM.get_GMT_OFFSET( in,
                                                out );
        time.tm_isdst = EEPROM.get_DAYLIGHT_SAVINGS( in,
                                                     out ) == 1;
        time.tm_zone = time_zone;
    }

    /**
     * Gets the time information from the weather station. Updates the given GNU
     * Broken-down Time structure with the time information. Puts the host's current
     * time zone into the structure.
     * 
     * @param in   Active inbound stream from weather station.
     * @param out  Active outbound stream to weather station.
     * @param time The GNU Broken-down Time structure to update.
     * 
     * @throws IOException
     * @throws WSD_exception
     */
    private void get_time( DataInputStream in,
                           DataOutputStream out,
                           tm time )
            throws IOException, WSD_exception
    {
        ZonedDateTime system_now = ZonedDateTime.now();
        get_time( in,
                  out,
                  system_now.getZone()
                            .getId(),
                  time );
    }

    private void set_time( DataInputStream in,
                           DataOutputStream out,
                           ZonedDateTime time )
            throws IOException
    {
        out.write( "SETTIME\n".getBytes( StandardCharsets.US_ASCII ) );

        int expected_response_size = 1;
        byte[] receive_buffer = new byte[32];

        int size_read = in.read( receive_buffer,
                                 0,
                                 expected_response_size );

        if( ( receive_buffer[0] != 0x06 ) || ( size_read != expected_response_size ) )
        {
            throw new IllegalArgumentException( "SETTIME (1) command failed." );
        }

        receive_buffer[0] = (byte)time.getSecond();
        receive_buffer[1] = (byte)time.getMinute();
        receive_buffer[2] = (byte)time.getHour();
        receive_buffer[3] = (byte)time.getDayOfMonth();
        receive_buffer[4] = (byte)time.getMonthValue();
        receive_buffer[5] = (byte)( time.getYear() - 1900 );

        short crc = Check_CRC.calculate_CRC_16( receive_buffer,
                                                0,
                                                6 );

        receive_buffer[6] = (byte)( crc >> 8 );
        receive_buffer[7] = (byte)crc;

        out.write( receive_buffer,
                   0,
                   8 );

        size_read = in.read( receive_buffer,
                             0,
                             expected_response_size );

        if( ( receive_buffer[0] != 0x06 ) || ( size_read != expected_response_size ) )
        {
            throw new IllegalArgumentException( "SETTIME (2) command failed." );
        }
    }

    private Weather_data get_weather_data( DataInputStream in,
                                           DataOutputStream out )
            throws IOException, InterruptedException, WSD_exception
    {
        wake( in,
              out );

        Weather_data weather_data = new Weather_data();

        {
            out.write( "LPS 2 1\n".getBytes( StandardCharsets.US_ASCII ) );

            byte[] receive_buffer = new byte[1024];
            int received_length = in.read( receive_buffer,
                                           0,
                                           receive_buffer.length );

            if( receive_buffer[0] != 0x06 )
            {
                throw new IllegalArgumentException( "Invalid packet data" );
            }

            byte[] packet = Arrays.copyOfRange( receive_buffer,
                                                1,
                                                received_length );

            weather_data.parse_packet( Weather_data.Type.LOOP2,
                                       packet,
                                       received_length - 1 );
        }

        {
            out.write( "LPS 1 1\n".getBytes( StandardCharsets.US_ASCII ) );

            byte[] receive_buffer = new byte[1024];
            int received_length = in.read( receive_buffer,
                                           0,
                                           receive_buffer.length );

            if( receive_buffer[0] != 0x06 )
            {
                throw new IllegalArgumentException( "Invalid packet data" );
            }

            byte[] packet = Arrays.copyOfRange( receive_buffer,
                                                1,
                                                received_length );

            weather_data.parse_packet( Weather_data.Type.LOOP,
                                       packet,
                                       received_length - 1 );
        }

        {
            out.write( "HILOWS\n".getBytes( StandardCharsets.US_ASCII ) );

            byte[] receive_buffer = new byte[1024];
            int received_length = in.read( receive_buffer,
                                           0,
                                           receive_buffer.length );

            if( receive_buffer[0] != 0x06 )
            {
                throw new IllegalArgumentException( "Invalid packet data" );
            }

            byte[] packet = Arrays.copyOfRange( receive_buffer,
                                                1,
                                                received_length );

            weather_data.parse_packet( Weather_data.Type.HILOWS,
                                       packet,
                                       received_length - 1 );
        }

        weather_data.time = DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss'Z'" )
                                             .format( ZonedDateTime.now( ZoneId.of( "UTC" ) ) );
        weather_data.wrd = s_wrd;
        weather_data.total_packets_received = s_total_packets_received;
        weather_data.total_packets_missed = s_total_packets_missed;
        weather_data.number_of_resynchronizations = s_number_of_resynchronizations;
        weather_data.largest_number_packets_received_in_a_row = s_largest_number_packets_received_in_a_row;
        weather_data.number_of_CRC_errors_detected = s_number_of_CRC_errors_detected;
        weather_data.firmware_date_code = s_firmware_date_code;
        weather_data.firmware_version = s_firmware_version;

        return weather_data;
    }
}
