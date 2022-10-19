
package net.ddns.rkdawenterprises;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Utilities
{
    public static class Database_info
    {
        public final String AUTHENTICATION_DATABASE_HOST;
        public final String AUTHENTICATION_DATABASE_NAME;
        public final String AUTHENTICATION_DATABASE_USER;
        public final String AUTHENTICATION_DATABASE_PASS;

        public Database_info( String authentication_database_host,
                              String authentication_database_name,
                              String authentication_database_user,
                              String authentication_database_pass )
        {
            this.AUTHENTICATION_DATABASE_HOST = authentication_database_host;
            this.AUTHENTICATION_DATABASE_NAME = authentication_database_name;
            this.AUTHENTICATION_DATABASE_USER = authentication_database_user;
            this.AUTHENTICATION_DATABASE_PASS = authentication_database_pass;
        }

        public static String serialize_to_JSON( Database_info object )
        {
            Gson gson = new GsonBuilder().disableHtmlEscaping()
                                         .setPrettyPrinting()
                                         .create();
            return gson.toJson( object );
        }

        public static Database_info deserialize_from_JSON( String string_JSON )
        {
            Database_info object = null;
            try
            {
                Gson gson = new GsonBuilder().disableHtmlEscaping()
                                             .setPrettyPrinting()
                                             .create();
                object = gson.fromJson( string_JSON,
                                        Database_info.class );
            }
            catch( com.google.gson.JsonSyntaxException exception )
            {
                System.out.println( "Bad data format for Database_info: " + exception );
                System.out.println( ">>>" + string_JSON + "<<<" );
            }

            return object;
        }

        public String serialize_to_JSON()
        {
            return serialize_to_JSON( this );
        }
    }

    public static Database_info get_database_info( ServletContext servlet_context ) throws IllegalArgumentException
    {
        String database_info_JSON_as_string = resource_file_to_string( "database_info.json",
                                                                       servlet_context );
        return Database_info.deserialize_from_JSON( database_info_JSON_as_string );
    }

    /**
     * Authenticates user:password by matching it to a record in the authentication
     * database. If the username is found, and the hashed password matches the
     * hashed password in the database, the given User object is updated with the
     * information in the authentication database.
     * 
     * If the username is found but the hashed password does not match, the given
     * User object is also updated with the information in the authentication
     * database. But in this case an exception is thrown.
     *
     * The DATETIME values in the database are assumed to be UTC, so all times
     * returned are also UTC.
     * 
     * @param servlet_context The ServletContext for the current web application.
     * @param username
     * @param password
     * @param user            The User object to update with the database
     *                        information.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static void authenticate( ServletContext servlet_context,
                                     String username,
                                     String password,
                                     User user )
            throws SQLException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException,
            DateTimeException, IllegalArgumentException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info( servlet_context );
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            String query = "SELECT * FROM accounts WHERE username = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              username );

                ResultSet result_set = prepared_statement.executeQuery();
                if( result_set.next() )
                {
                    user.id = result_set.getInt( "id" );
                    user.username = result_set.getString( "username" );
                    user.email = result_set.getString( "email" );

                    LocalDateTime created_at_as_local_date_time = result_set.getObject( "created_at",
                                                                                        LocalDateTime.class );
                    ZonedDateTime created_at_as_zoned_date_time = ZonedDateTime.of( created_at_as_local_date_time,
                                                                                    ZoneId.of( "UTC" ) );
                    user.created_at = Instant.from( created_at_as_zoned_date_time );

                    LocalDateTime last_log_in_as_local_date_time = result_set.getObject( "last_log_in",
                                                                                         LocalDateTime.class );
                    if( last_log_in_as_local_date_time != null )
                    {
                        ZonedDateTime last_log_in_as_zoned_date_time = ZonedDateTime.of( last_log_in_as_local_date_time,
                                                                                         ZoneId.of( "UTC" ) );
                        user.last_log_in = Instant.from( last_log_in_as_zoned_date_time );
                    }

                    LocalDateTime last_invalid_attempt_as_local_date_time = result_set.getObject( "last_invalid_attempt",
                                                                                                  LocalDateTime.class );
                    if( last_invalid_attempt_as_local_date_time != null )
                    {
                        ZonedDateTime last_invalid_attempt_as_zoned_date_time = ZonedDateTime.of( last_invalid_attempt_as_local_date_time,
                                                                                                  ZoneId.of( "UTC" ) );
                        user.last_invalid_attempt = Instant.from( last_invalid_attempt_as_zoned_date_time );
                    }

                    user.invalid_attempts = result_set.getInt( "invalid_attempts" );
                    if( ( user.invalid_attempts > 5 ) && ( last_invalid_attempt_as_local_date_time != null ) )
                    {
                        ZonedDateTime last_invalid_attempt_as_zoned_date_time = ZonedDateTime.of( last_invalid_attempt_as_local_date_time,
                                                                                                  ZoneId.of( "UTC" ) );

                        ZonedDateTime system_now = ZonedDateTime.now( ZoneId.of( "UTC" ) );

                        // System.out.println( "last: " + last_invalid_attempt_as_zoned_date_time );
                        // System.out.println( "now: " + system_now );

                        Duration difference = Duration.between( last_invalid_attempt_as_zoned_date_time,
                                                                system_now );

                        // System.out.println( "difference: " + Utilities.duration_to_string( difference
                        // ) );

                        if( difference.compareTo( Duration.ofMinutes( 10 ) ) < 0 )
                        {
                            throw new IllegalArgumentException( "Too many attempts, temporarily blocked" );
                        }
                    }

                    String password_in_database = result_set.getString( "password" );
                    byte[] salt_bytes = result_set.getBytes( "salt" );
                    String password_hashed = hash_password( password,
                                                            salt_bytes );
                    if( password_in_database.equals( password_hashed ) )
                    {
                        user.authenticated = User.AUTHENTICATED.TRUE;
                        return;
                    }
                    else
                    {
                        user.authenticated = User.AUTHENTICATED.FALSE;
                    }
                }
            }
        }

        throw new IllegalArgumentException( "Could not validate username/password" );
    }

    /**
     * Updates the last log in time with now at UTC. Only meant to be called
     * immediately after a successful authentication with the database ID of the
     * user. The last invalid attempt and invalid attempts will be cleared as a
     * result.
     *
     * @param servlet_context The ServletContext for the current web application.
     * @param an_ID           The database identifier of the user record to update
     *
     * @return The now at UTC time.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Instant update_last_log_in( ServletContext servlet_context,
                                              int an_ID )
            throws SQLException, ClassNotFoundException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info( servlet_context );
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            Instant instant;
            instant = Instant.now()
                             .truncatedTo( ChronoUnit.SECONDS );
            String last_log_in = instant.toString()
                                        .replace( 'T',
                                                  ' ' )
                                        .replace( "Z",
                                                  "" );

            String query = "UPDATE `accounts` SET `last_log_in` = ?, `last_invalid_attempt` = ?, `invalid_attempts` = ? WHERE `accounts`.`id` = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              last_log_in );
                prepared_statement.setNull( 2,
                                            Types.NULL );
                prepared_statement.setInt( 3,
                                           0 );
                prepared_statement.setInt( 4,
                                           an_ID );
                prepared_statement.executeUpdate();
            }

            return instant;
        }
    }

    /**
     * Updates the last invalid attempted log in time with now at UTC. Only meant to
     * be called immediately after a failed authentication with the database ID of
     * the user. The given invalid attempts will be updated in the database.
     * 
     * Updates the last log in time with now at UTC. Only meant to be called
     * immediately after authenticate with the database ID of the user.
     * 
     * @param servlet_context  The ServletContext for the current web application.
     * @param an_ID            The database identifier of the user record to update
     * @param invalid_attempts The number of invalid attempts. This value will be
     *                         written to the database.
     * 
     * @return The now at UTC time.
     * 
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Instant update_last_invalid_attempt( ServletContext servlet_context,
                                                       int an_ID,
                                                       int invalid_attempts )
            throws SQLException, ClassNotFoundException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info( servlet_context );
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            Instant instant;
            instant = Instant.now()
                             .truncatedTo( ChronoUnit.SECONDS );
            String last_invalid_attempt = instant.toString()
                                                 .replace( 'T',
                                                           ' ' )
                                                 .replace( "Z",
                                                           "" );

            String query = "UPDATE `accounts` SET `last_invalid_attempt` = ?, `invalid_attempts` = ? WHERE `accounts`.`id` = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              last_invalid_attempt );
                prepared_statement.setInt( 2,
                                           invalid_attempts );
                prepared_statement.setInt( 3,
                                           an_ID );
                prepared_statement.executeUpdate();
            }

            return instant;
        }
    }

    public static String hash_password( String password,
                                        byte[] salt )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 10000;
        int key_length = 512;
        char[] password_chars = password.toCharArray();
        byte[] hashed_bytes = hash_PBKDF2( password_chars,
                                           salt,
                                           iterations,
                                           key_length );
        return Hex.encodeHexString( hashed_bytes );
    }

    public static byte[] hash_PBKDF2( char[] message,
                                      byte[] salt,
                                      int iterations,
                                      int key_length )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory secret_key_factory = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
        PBEKeySpec key_spec = new PBEKeySpec( message,
                                              salt,
                                              iterations,
                                              key_length );
        SecretKey key = secret_key_factory.generateSecret( key_spec );
        return key.getEncoded();
    }

    public static String resource_file_to_string( String path,
                                                  ServletContext servlet_context )
            throws IllegalArgumentException
    {
        try( InputStream input_stream = servlet_context.getResourceAsStream( "/WEB-INF/res/" + path );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            StringBuffer string_buffer = new StringBuffer();
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
            return string_buffer.toString();
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }
    }

    public static String get_pom_properties( ServletContext servlet_context ) throws IllegalArgumentException
    {
        StringBuffer string_buffer = new StringBuffer();

        try( InputStream input_stream = servlet_context.getResourceAsStream( "/META-INF/maven/net.ddns.rkdawenterprises/ROOT/pom.properties" );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }

        try( InputStream input_stream = servlet_context.getResourceAsStream( "/META-INF/MANIFEST.MF" );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }

        return string_buffer.toString();
    }

    /**
     *
     * @param key_length The length of the key to use.
     *
     * @return The public and private keys, Base64 encoded, in a string array. Index
     *         zero is the private key and index one is the public key.
     *
     * @throws NoSuchAlgorithmException
     */
    public static String[] generate_RSA_key_pair_base64( int key_length ) throws NoSuchAlgorithmException
    {
        KeyPairGenerator key_pair_generator = KeyPairGenerator.getInstance( "RSA" );
        SecureRandom secure_random = SecureRandom.getInstance( "SHA1PRNG" );
        key_pair_generator.initialize( key_length,
                                       secure_random );
        KeyPair key_pair = key_pair_generator.generateKeyPair();
        PrivateKey private_key = key_pair.getPrivate();
        PublicKey public_key = key_pair.getPublic();
        String[] return_pair = new String[2];
        return_pair[0] = Base64.getEncoder()
                               .encodeToString( private_key.getEncoded() );
        return_pair[1] = Base64.getEncoder()
                               .encodeToString( public_key.getEncoded() );
        return return_pair;
    }

    public static PrivateKey convert_base64_private_key_to_PKCS8( String base64PrivateKey )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec a_PKCS8_encoded_key_spec = new PKCS8EncodedKeySpec( Base64.getDecoder()
                                                                                      .decode( base64PrivateKey.getBytes() ) );
        return KeyFactory.getInstance( "RSA" )
                         .generatePrivate( a_PKCS8_encoded_key_spec );
    }

    public static String decrypt_RSA( byte[] data,
                                      PrivateKey privateKey )
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException
    {
        Cipher cipher = Cipher.getInstance( "RSA" );
        cipher.init( Cipher.DECRYPT_MODE,
                     privateKey );
        return new String( cipher.doFinal( data ) );
    }

    public static String decrypt_RSA_base64( String base64data,
                                             String base64PrivateKey )
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException
    {
        return decrypt_RSA( Base64.getDecoder()
                                  .decode( base64data.getBytes() ),
                            convert_base64_private_key_to_PKCS8( base64PrivateKey ) );
    }

    public static Date add_seconds( Date date,
                                    Integer seconds )
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( date );
        calendar.add( Calendar.SECOND,
                      seconds );

        return calendar.getTime();
    }

    public enum ROUNDING_TYPE
    {
        DOWN,
        UP,
        HALF_UP
    }

    public static ZonedDateTime round( ZonedDateTime zoned_date_time,
                                       TemporalField round_to_field,
                                       int rounding_increment )
    {
        return round( zoned_date_time,
                      round_to_field,
                      rounding_increment,
                      ROUNDING_TYPE.HALF_UP );
    }

    public static ZonedDateTime round( ZonedDateTime zoned_date_time,
                                       TemporalField round_to_field,
                                       int rounding_increment,
                                       ROUNDING_TYPE type )
    {
        long field_value = zoned_date_time.getLong( round_to_field );
        long r = field_value % rounding_increment;
        ZonedDateTime ceiling = zoned_date_time.plus( rounding_increment - r,
                                                      round_to_field.getBaseUnit() )
                                               .truncatedTo( round_to_field.getBaseUnit() );
        ZonedDateTime floor = zoned_date_time.plus( -r,
                                                    round_to_field.getBaseUnit() )
                                             .truncatedTo( round_to_field.getBaseUnit() );

        if( type == ROUNDING_TYPE.DOWN )
        {
            return floor;
        }
        else if( type == ROUNDING_TYPE.UP )
        {
            return ceiling;
        }
        else
        {
            Duration distance_to_floor = Duration.between( floor,
                                                           zoned_date_time );
            Duration distance_to_ceiling = Duration.between( zoned_date_time,
                                                             ceiling );
            ZonedDateTime rounded = distance_to_floor.compareTo( distance_to_ceiling ) < 0 ? floor : ceiling;
            return rounded;
        }
    }

    public static final Duration DEFAULT_NETWORK_TIMEOUT = Duration.ofSeconds( 5 );

    /**
     * Sends a UDP broadcast message and waits for a response. Only waits for a
     * single response from a single responder. Uses the
     * {@link #DEFAULT_NETWORK_TIMEOUT}.
     *
     * @param message         The message data to broadcast.
     * @param port            The UDP port to broadcast on.
     * @param datagram_packet Container for the response to the broadcast.
     *
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IOException
     * @throws SocketTimeoutException
     */
    public static void send_UDP_broadcast( byte[] message,
                                           int port,
                                           DatagramPacket receive_datagram_packet )
            throws SocketException, UnknownHostException, IllegalArgumentException, SecurityException, IOException,
            SocketTimeoutException
    {
        send_UDP_broadcast( message,
                            port,
                            receive_datagram_packet,
                            DEFAULT_NETWORK_TIMEOUT );
    }

    public static String get_local_IP_address() throws UnknownHostException
    {
        return InetAddress.getLocalHost()
                          .getHostAddress();
    }

    public static InetAddress get_broadcast_address( String local_IP_address )
            throws UnknownHostException, SocketException
    {
        InetAddress address = InetAddress.getByName( local_IP_address );
        NetworkInterface network_interface = NetworkInterface.getByInetAddress( address );

        if( network_interface.isUp() && !network_interface.isLoopback() )
        {
            List< InterfaceAddress > interface_addresses = network_interface.getInterfaceAddresses();
            for( InterfaceAddress interface_address : interface_addresses )
            {
                String host = interface_address.getAddress()
                                               .getHostAddress();
                InetAddress broadcast = interface_address.getBroadcast();
                if( host.equals( local_IP_address ) && broadcast != null )
                {
                    return broadcast;
                }
            }
        }

        return null;
    }

    /**
     * Sends a UDP broadcast message and waits for a response. Only waits for a
     * single response from a single responder.
     *
     * @param message         The message data to broadcast.
     * @param port            The UDP port to broadcast on.
     * @param timeout         The socket timeout in milliseconds.
     * @param datagram_packet Container for the response to the broadcast. Returns
     *                        the first response received that is not from
     *                        localhost.
     *
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IOException
     * @throws SocketTimeoutException
     */
    public static void send_UDP_broadcast( byte[] message,
                                           int port,
                                           DatagramPacket receive_datagram_packet,
                                           Duration timeout )
            throws SocketException, UnknownHostException, IllegalArgumentException, SecurityException, IOException,
            SocketTimeoutException
    {
        String local_IP_address = get_local_IP_address();
        InetAddress broadcast_address = get_broadcast_address( local_IP_address );
        if( broadcast_address == null ) throw new IllegalArgumentException( "Could not get broadcast address" );

        try( DatagramSocket dsock = new DatagramSocket( port ) )
        {
            dsock.setSoTimeout( (int)timeout.toMillis() );
            dsock.setBroadcast( true );
            DatagramPacket transmit_datagram_packet = new DatagramPacket( message,
                                                                          message.length,
                                                                          broadcast_address,
                                                                          port );
            dsock.send( transmit_datagram_packet );

            while( true )
            {
                dsock.receive( receive_datagram_packet );
                String received_from = receive_datagram_packet.getAddress()
                                                              .getHostAddress();
                if( !received_from.equals( local_IP_address ) ) break;
            }
        }
    }

    /**
     * Returns a new byte array that is a subbytearray of the given byte array. The
     * subbytearray begins at the specified begin_index and extends to the byte at
     * the index of ( end_index - 1 ). Thus the length of the subbytearray is (
     * end_index - begin_index ).
     *
     * @param begin_index The beginning index, inclusive.
     * @param end_index   The ending index, exclusive.
     *
     * @return The specified subbytearray.
     *
     * @throws IndexOutOfBoundsException If begin_index or end_index are negative,
     *                                   or if end_index is greater than source
     *                                   length, or if begin_index is greater than
     *                                   end_index.
     */
    public static byte[] subbytearray( byte[] source,
                                       int begin_index,
                                       int end_index )
            throws IndexOutOfBoundsException
    {
        if( ( begin_index < 0 ) || ( end_index < 0 ) || ( end_index > source.length ) || ( begin_index > begin_index ) )
        {
            throw new IndexOutOfBoundsException();
        }

        byte[] destination = new byte[end_index - begin_index];
        System.arraycopy( source,
                          begin_index,
                          destination,
                          0,
                          end_index - begin_index );
        return destination;
    }

    public static void print_buffer( byte[] buffer )
    {
        print_buffer( buffer,
                      buffer.length,
                      16 );
    }

    public static void print_buffer( byte[] buffer,
                                     int size )
    {
        print_buffer( buffer,
                      size,
                      16 );
    }

    public static void print_buffer( byte[] buffer,
                                     int size,
                                     int pitch )
    {
        if( size > 0xFFFF ) return;

        System.out.printf( ">> %d bytes:%n",
                           size );

        String line_chars = "";

        int i;
        for( i = 0; i < size; i++ )
        {
            if( i % pitch == 0 )
            {
                if( i != 0 )
                {
                    if( line_chars.length() > 0 ) System.out.printf( "\"%s\"%n",
                                                                     line_chars );
                    line_chars = "";
                }

                System.out.printf( "    %04X ",
                                   i );
            }

            System.out.printf( "%02X ",
                               buffer[i] );
            line_chars += Character.isISOControl( (char)buffer[i] ) ? "." : Character.toString( (char)buffer[i] );
        }

        int x = i % pitch;
        if( x != 0 )
        {
            if( line_chars.length() > 0 ) System.out.printf( "%s\"%s\"%n",
                                                             "   ".repeat( pitch - x ),
                                                             line_chars );
            line_chars = "";
        }
        else
        {
            if( line_chars.length() > 0 ) System.out.printf( "\"%s\"%n",
                                                             line_chars );
        }
    }

    /**
     * Currently does not work with negative durations.
     * 
     * @param duration
     * 
     * @return
     */
    public static String duration_to_string( Duration duration )
    {
        long seconds_per_year = 31556952;
        long years = duration.getSeconds() / seconds_per_year;
        duration = duration.minusSeconds( years * seconds_per_year );

        long seconds_per_month = 2629746;
        long months = duration.getSeconds() / seconds_per_month;
        duration = duration.minusSeconds( months * seconds_per_month );

        long seconds_per_week = 604800;
        long weeks = duration.getSeconds() / seconds_per_week;
        duration = duration.minusSeconds( weeks * seconds_per_week );

        long seconds_per_day = 86400;
        long days = duration.getSeconds() / seconds_per_day;
        duration = duration.minusSeconds( days * seconds_per_day );

        long seconds_per_hour = 3600;
        long hours = duration.getSeconds() / seconds_per_hour;
        duration = duration.minusSeconds( hours * seconds_per_hour );

        long seconds_per_minute = 60;
        long minutes = duration.getSeconds() / seconds_per_minute;
        duration = duration.minusSeconds( minutes * seconds_per_minute );

        long seconds = duration.getSeconds();
        int nanoseconds = duration.getNano();

        StringBuilder duration_HMS_string_builder = new StringBuilder();

        if( hours > 0 )
        {
            duration_HMS_string_builder.append( hours );
            duration_HMS_string_builder.append( "h" );
        }

        if( ( duration_HMS_string_builder.length() > 0 ) && ( minutes > 0 ) ) duration_HMS_string_builder.append( ":" );

        if( minutes > 0 )
        {
            duration_HMS_string_builder.append( minutes );
            duration_HMS_string_builder.append( "m" );
        }

        if( ( duration_HMS_string_builder.length() > 0 ) && ( ( seconds > 0 ) || ( nanoseconds > 0 ) ) )
            duration_HMS_string_builder.append( ":" );

        if( seconds > 0 )
        {
            duration_HMS_string_builder.append( seconds );
        }

        if( nanoseconds > 0 )
        {
            double seconds_fraction = (double)nanoseconds / 1000000000;
            DecimalFormat formatter = new DecimalFormat( "#.0########" );
            duration_HMS_string_builder.append( formatter.format( seconds_fraction ) );
        }

        if( ( seconds > 0 ) || ( nanoseconds > 0 ) ) duration_HMS_string_builder.append( "s" );

        StringBuilder duration_string_builder = new StringBuilder( 64 );

        if( years > 0 )
        {
            duration_string_builder.append( years );
            duration_string_builder.append( "y" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( months > 0 ) ) duration_string_builder.append( ", " );

        if( months > 0 )
        {
            duration_string_builder.append( months );
            duration_string_builder.append( "mo" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( weeks > 0 ) ) duration_string_builder.append( ", " );

        if( weeks > 0 )
        {
            duration_string_builder.append( weeks );
            duration_string_builder.append( "w" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( days > 0 ) ) duration_string_builder.append( ", " );

        if( days > 0 )
        {
            duration_string_builder.append( days );
            duration_string_builder.append( "d" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( duration_HMS_string_builder.length() > 0 ) )
            duration_string_builder.append( ", " );

        if( duration_HMS_string_builder.length() > 0 ) duration_string_builder.append( duration_HMS_string_builder );

        return duration_string_builder.toString();
    }

    public static void sleep( Duration duration )
    {
        try
        {
            Thread.sleep( duration.toMillis() );
        }
        catch( InterruptedException exception )
        {
        }
    }

    /**
     * An inelegant way of getting tomcat to reload the application.
     * 
     * @param servlet_context
     */
    public static void reload_application( ServletContext servlet_context )
    {
        System.out.println( "Reloading RKDAWE Web application..." );

        String context_path = servlet_context.getRealPath( "/" );
        File f = new File( context_path );
        String parent_path = f.getParent();
        try
        {
            FileUtils.touch( new File( parent_path + "/ROOT.war" ) );
        }
        catch( IOException exception )
        {
            System.out.println( exception );
        }
    }

    /**
     * Equivalent to GNU Broken-down Time structure.
     */
    public static final class tm
    {
        int tm_sec;
        int tm_min;
        int tm_hour;
        int tm_mday;
        int tm_mon;
        int tm_year;
        int tm_wday;
        int tm_yday;
        Boolean tm_isdst;
        long tm_gmtoff;
        String tm_zone;
    }

    public static tm get_local_tm()
    {
        ZonedDateTime system_now = ZonedDateTime.now();

        tm local_tm = new tm();
        local_tm.tm_gmtoff = system_now.getOffset()
                                       .getTotalSeconds();
        local_tm.tm_zone = system_now.getZone()
                                     .getId();
        local_tm.tm_isdst = system_now.getZone()
                                      .getRules()
                                      .isDaylightSavings( system_now.toInstant() );
        local_tm.tm_sec = system_now.getSecond();
        local_tm.tm_min = system_now.getMinute();
        local_tm.tm_hour = system_now.getHour();
        local_tm.tm_mday = system_now.getDayOfMonth();
        local_tm.tm_mon = system_now.getMonthValue() - 1;
        local_tm.tm_year = system_now.getYear() - 1900;
        local_tm.tm_wday = system_now.getDayOfWeek()
                                     .getValue();
        if( local_tm.tm_wday == 7 ) local_tm.tm_wday = 0;
        local_tm.tm_yday = system_now.getDayOfYear() - 1;

        return local_tm;
    }
}
