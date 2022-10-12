
package net.ddns.rkdawenterprises;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EEPROM
{
    public static byte read_EEPROM( DataInputStream in,
                                    DataOutputStream out,
                                    int address )
            throws IOException, WSD_exception
    {
        String command = String.format( "EERD %02X 01\n",
                                        address );
        byte[] message = command.getBytes( StandardCharsets.US_ASCII );
        out.write( message );

        int expected_response_size = 10;
        byte[] receive_buffer = new byte[32];

        if( ( in.read( receive_buffer,
                       0,
                       expected_response_size ) != expected_response_size )
                || ( receive_buffer[0] != '\n' ) || ( receive_buffer[1] != '\r' ) || ( receive_buffer[2] != 'O' )
                || ( receive_buffer[3] != 'K' ) || ( receive_buffer[4] != '\n' ) || ( receive_buffer[5] != '\r' )
                || ( receive_buffer[8] != '\n' ) || ( receive_buffer[9] != '\r' ) )
        {
            throw new WSD_exception( "EERD command failed" );
        }

        byte data_string[] = { receive_buffer[6], receive_buffer[7] };
        return (byte)Integer.parseInt( new String( data_string,
                                                   StandardCharsets.US_ASCII ) );
    }

    public static byte[] read_EEPROM( DataInputStream in,
                                      DataOutputStream out,
                                      int address,
                                      int size )
            throws IOException, WSD_exception
    {
        String command = String.format( "EEBRD %02X %02X\n",
                                        address,
                                        size );
        byte[] message = command.getBytes( StandardCharsets.US_ASCII );
        out.write( message );

        int expected_response_size = 3 + size;
        byte[] receive_buffer = new byte[32];

        int size_read = in.read( receive_buffer,
                                 0,
                                 expected_response_size );

        if( ( size_read != expected_response_size ) || ( receive_buffer[0] != 0x06 ) )
        {
            throw new WSD_exception( "EEBRD command failed" );
        }

        Check_CRC.check_CRC_16( receive_buffer,
                                1,
                                expected_response_size - 1 );
        return Arrays.copyOfRange( receive_buffer,
                                   1,
                                   size_read - 2 );
    }

    public static void write_EEPROM( DataInputStream in,
                                     DataOutputStream out,
                                     int address,
                                     byte data )
            throws IOException, WSD_exception
    {
        String command = String.format( "EEWR %02X %02X\n",
                                        address,
                                        data );
        byte[] message = command.getBytes( StandardCharsets.US_ASCII );
        out.write( message );

        int expected_response_size = 6;
        byte[] receive_buffer = new byte[32];

        if( ( in.read( receive_buffer,
                       0,
                       expected_response_size ) != expected_response_size )
                || ( receive_buffer[0] != '\n' ) || ( receive_buffer[1] != '\r' ) || ( receive_buffer[2] != 'O' )
                || ( receive_buffer[3] != 'K' ) || ( receive_buffer[4] != '\n' ) || ( receive_buffer[5] != '\r' ) )
        {
            throw new WSD_exception( "EEWR command failed" );
        }
    }

    public static void write_EEPROM( DataInputStream in,
                                     DataOutputStream out,
                                     int address,
                                     int size,
                                     byte[] data )
            throws IOException, WSD_exception
    {
        if( ( size < 1 ) || ( size > 255 ) ) throw new WSD_exception( "EEBWR invalid argument size (1-255)" );

        String command = String.format( "EEBWR %02X %02X\n",
                                        address,
                                        size );
        byte[] message = command.getBytes( StandardCharsets.US_ASCII );
        out.write( message );

        int expected_response_size = 1;
        byte[] receive_buffer = new byte[32];

        int size_read = in.read( receive_buffer,
                                 0,
                                 expected_response_size );

        if( ( size_read != expected_response_size ) || ( receive_buffer[0] != 0x06 ) )
        {
            throw new WSD_exception( "EEBWR (1) command failed" );
        }

        short a_CRC = Check_CRC.calculate_CRC_16( data,
                                                  0,
                                                  size );
        byte[] data_CRC = { (byte)( ( a_CRC >> 8 ) & 0xFF ), (byte)( a_CRC & 0xFF ) };
        byte[] transmit_buffer = new byte[data.length + data_CRC.length];
        System.arraycopy( data,
                          0,
                          transmit_buffer,
                          0,
                          data.length );
        System.arraycopy( data_CRC,
                          0,
                          transmit_buffer,
                          data.length,
                          data_CRC.length );
        out.write( transmit_buffer );

        size_read = in.read( receive_buffer,
                             0,
                             expected_response_size );

        if( ( size_read != expected_response_size ) || ( receive_buffer[0] != 0x06 ) )
        {
            throw new WSD_exception( "EEBWR (2) command failed" );
        }
    }

    public static void set_MANUAL_OR_AUTO( DataInputStream in,
                                           DataOutputStream out,
                                           byte value )
            throws IOException, WSD_exception
    {
        int address = 0x12;
        byte data = read_EEPROM( in,
                                 out,
                                 address );
        if( ( data > 1 ) || ( data < 0 ) ) throw new WSD_exception( "Received bad MANUAL_OR_AUTO value" );
        if( data != value )
        {
            write_EEPROM( in,
                          out,
                          address,
                          value );
        }
    }

    public static byte get_DAYLIGHT_SAVINGS( DataInputStream in,
                                             DataOutputStream out )
            throws IOException, WSD_exception
    {
        int address = 0x13;
        byte data = read_EEPROM( in,
                                 out,
                                 address );
        if( ( data > 1 ) || ( data < 0 ) ) throw new WSD_exception( "Received bad DAYLIGHT_SAVINGS value" );
        return data;
    }

    public static void set_DAYLIGHT_SAVINGS( DataInputStream in,
                                             DataOutputStream out,
                                             byte value )
            throws IOException, WSD_exception
    {
        byte data = get_DAYLIGHT_SAVINGS( in,
                                          out );
        if( data != value )
        {
            int address = 0x13;
            write_EEPROM( in,
                          out,
                          address,
                          value );
        }
    }

    public static void set_GMT_OR_ZONE( DataInputStream in,
                                        DataOutputStream out,
                                        byte value )
            throws IOException, WSD_exception
    {
        int address = 0x16;
        byte data = read_EEPROM( in,
                                 out,
                                 address );
        if( ( data > 1 ) || ( data < 0 ) ) throw new WSD_exception( "Received bad GMT_OR_ZONE value" );
        if( data != value )
        {
            write_EEPROM( in,
                          out,
                          address,
                          value );
        }
    }

    public static int get_GMT_OFFSET( DataInputStream in,
                                      DataOutputStream out )
            throws IOException, WSD_exception
    {
        int address = 0x14;
        int size = 2;
        byte[] data = read_EEPROM( in,
                                   out,
                                   address,
                                   size );

        int offset_seconds = ( (int)Weather_data.bytes_to_short( data,
                                                                 0 ) )
                * 36;

        if( ( offset_seconds > ( 12 * 3600 ) ) || ( offset_seconds < ( -12 * 3600 ) ) )
        {
            throw new WSD_exception( "Received bad GMT_OFFSET value" );
        }

        return offset_seconds;
    }

    public static void set_GMT_OFFSET( DataInputStream in,
                                       DataOutputStream out,
                                       long value )
            throws IOException, WSD_exception
    {
        int offset_seconds = get_GMT_OFFSET( in,
                                             out );
        if( offset_seconds != value )
        {
            long offset = value / 36;
            byte[] data = { (byte)( offset & 0xFF ), (byte)( ( offset >> 8 ) & 0xFF ) };
            int address = 0x14;
            int size = 2;
            write_EEPROM( in,
                          out,
                          address,
                          size,
                          data );
        }
    }
}
