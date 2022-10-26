
package net.ddns.rkdawenterprises.rkdawe_webapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.management.InvalidAttributeValueException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.primitives.Ints;

import net.ddns.rkdawenterprises.rkdawe_api_common.Directory_listing;
import net.ddns.rkdawenterprises.rkdawe_api_common.WSD_exception;
import net.ddns.rkdawenterprises.rkdawe_api_common.Weather_data;

public class Weather_history
{
    /**
     * @param weather_data
     * @throws FileAlreadyExistsException
     * @throws AccessDeniedException
     * @throws InvalidAttributeValueException
     * @throws UnsupportedOperationException
     * @throws SecurityException
     * @throws IOException
     * @throws WSD_exception
     */
    static void save_weather_record( Weather_data weather_data )
            throws FileAlreadyExistsException, AccessDeniedException, InvalidAttributeValueException,
            UnsupportedOperationException, SecurityException, IOException, WSD_exception
    {
        synchronized( Directory_listing.WEATHER_HISTORY_PATH )
        {
            try( BufferedWriter writer = new BufferedWriter( new FileWriter( Directory_listing.WEATHER_HISTORY_PATH,
                                                                             true ) ) )
            {
                writer.append( weather_data.get_history_record() );
            }

            weather_record_file_maintenance();
        }
    }

    /**
     * Storing the weather history as CSV text lines. Synchronize on the file path
     * for all history file operations.
     * 
     * Index 0 is always the latest active history data file.
     * 
     * The previous history data files will start at index 1 for the oldest, up to
     * the max number of history files. The highest number will be the newest, and
     * index 0 will be moved to the next highest number. The oldest will
     * periodically be deleted when max number of files is exceeded, so the index
     * for the oldest file will increase from 1.
     * 
     * !!!TODO: The WEATHER_HISTORY_DIRECTORY needs to be created before enabling
     * Tomcat, and owner/group set to tomcat:tomcat as well as 775 permissions (or
     * 755 if no group write/modification needed)!!!
     */
    static void weather_record_file_maintenance() throws IOException, UnsupportedOperationException,
            FileAlreadyExistsException, SecurityException, AccessDeniedException, InvalidAttributeValueException
    {
        synchronized( Directory_listing.WEATHER_HISTORY_PATH )
        {
            Path primary_path = Paths.get( Directory_listing.WEATHER_HISTORY_PATH );
            if( Files.exists( primary_path ) )
            {
                if( Files.size( primary_path ) > ( Directory_listing.MAX_HISTORY_FILE_SIZE_KiB * 1024 ) )
                {
                    int[] indicies = get_file_indicies();
                    int lowest_index = indicies[0];
                    int highest_index = indicies[1];
                    int index_length = ( highest_index - lowest_index ) + 1;

                    // Make room for file move.
                    if( index_length > ( Directory_listing.MAX_HISTORY_FILES - 1 ) )
                    {
                        String lowest_index_path_string = Directory_listing.WEATHER_HISTORY_PATH.replace( "0",
                                                                                        String.valueOf( lowest_index ) );
                        Path lowest_index_path = Path.of( lowest_index_path_string );
                        Files.delete( lowest_index_path );
                    }

                    // Copy primary to next index.
                    String next_path_as_string = Directory_listing.WEATHER_HISTORY_PATH.replace( "0",
                                                                               String.valueOf( highest_index + 1 ) );
                    Path next_path = Paths.get( next_path_as_string );
                    Files.move( primary_path,
                                next_path,
                                StandardCopyOption.REPLACE_EXISTING );

                    create_history_file();

                    return;
                }
            }
            else
            {
                create_history_file();
            }
        }
    }

    /**
     * Scans the history directory and determines the lowest and highest indicies,
     * excluding 0.
     * 
     * @return An integer array with the {lowest, highest} file indicies, or {0, 0}
     *         for error or no files.
     * @throws IOException
     */
    private static int[] get_file_indicies() throws IOException
    {
        synchronized( Directory_listing.WEATHER_HISTORY_PATH )
        {
            File f = new File( Directory_listing.WEATHER_HISTORY_DIRECTORY );
            String[] paths = f.list();

            int lowest_index = Integer.MAX_VALUE;
            int highest_index = Integer.MIN_VALUE;

            final Pattern pattern = Pattern.compile( Directory_listing.WEATHER_HISTORY_FILENAME_REGEX );
            for( String pathname : paths )
            {
                final Matcher matcher = pattern.matcher( pathname );
                if( matcher.find() )
                {
                    if( matcher.groupCount() == 1 )
                    {
                        int index = Optional.ofNullable( matcher.group( 1 ) )
                                            .map( Ints::tryParse )
                                            .orElse( Integer.MIN_VALUE );
                        if( index == 0 ) continue;
                        if( index < lowest_index ) lowest_index = index;
                        if( index > highest_index ) highest_index = index;
                    }
                }
            }

            if( ( lowest_index == Integer.MAX_VALUE ) || ( highest_index == Integer.MIN_VALUE ) )
            {
                return new int[] { 0, 0 };
            }
            else
            {
                return new int[] { lowest_index, highest_index };
            }
        }
    }

    private static void create_history_file() throws IOException, UnsupportedOperationException,
            FileAlreadyExistsException, SecurityException, AccessDeniedException, InvalidAttributeValueException
    {
        synchronized( Directory_listing.WEATHER_HISTORY_PATH )
        {
            Path primary_path = Paths.get( Directory_listing.WEATHER_HISTORY_PATH );
            Path directory_path = Paths.get( Directory_listing.WEATHER_HISTORY_DIRECTORY );
            if( !Files.exists( directory_path ) || !Files.isDirectory( directory_path )
                    || !Files.isWritable( directory_path ) || !Files.isReadable( directory_path ) )
            {
                throw new InvalidAttributeValueException( "History directory missing or wrong permissions" );
            }

            Set< PosixFilePermission > permissions = PosixFilePermissions.fromString( "rw-rw-r--" );
            FileAttribute< Set< PosixFilePermission > > file_attributes = PosixFilePermissions.asFileAttribute( permissions );
            Files.createFile( primary_path,
                              file_attributes );
            Files.setPosixFilePermissions( primary_path,
                                           permissions );
            if( !Files.exists( primary_path ) || !Files.isWritable( primary_path )
                    || !Files.isReadable( primary_path ) )
            {
                throw new InvalidAttributeValueException( "Unable to create or there are wrong permissions for history file" );
            }

            BufferedWriter writer = new BufferedWriter( new FileWriter( Directory_listing.WEATHER_HISTORY_PATH,
                                                                        true ) );
            writer.append( Weather_data.get_history_record_columns() )
                  .close();
        }
    }

    /**
     * The given type parameter (?type=) indicates the requested files including
     * "all", or "dir", or a CSV (%2C) list of file indicies. The "all" or "list of"
     * files will be compressed into a zip file. The "dir" directory listing will
     * be JSON text.
     * 
     * @param response
     * @param type
     * @throws IOException
     */
    static void send_compressed_weather_history( HttpServletResponse response,
                                                 String type )
            throws IOException
    {
        synchronized( Directory_listing.WEATHER_HISTORY_PATH )
        {
            if( type.equalsIgnoreCase( "all" ) )
            {
                send_all( response );
            }
            else if( type.equalsIgnoreCase( "dir" ) )
            {
                send_directory_listing( response );
            }
            else
            {
                String[] indicies = type.split( "," );
                if( indicies.length > 0 )
                {
                    List< File > file_list = new ArrayList< File >( indicies.length );
                    try
                    {
                        for( String index_string : indicies )
                        {
                            int index = Integer.parseInt( index_string );
                            String path_string = Directory_listing.WEATHER_HISTORY_PATH.replace( "0",
                                                                               String.valueOf( index ) );
                            file_list.add( new File( path_string ) );
                        }
                    }
                    catch( NumberFormatException e )
                    {
                        response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
                        return;
                    }

                    send_files( response,
                                file_list );
                }
            }
        }
    }

    private static void send_files( HttpServletResponse response,
                                    List< File > files )
            throws IOException
    {
        byte[] buffer = new byte[4096];
        int length;

        File compressed_file = File.createTempFile( Directory_listing.COMPRESSED_WEATHER_HISTORY_FILENAME,
                                                    ".zip" );
        try( FileOutputStream file_output_stream = new FileOutputStream( compressed_file );
                ZipOutputStream zip_output_stream = new ZipOutputStream( file_output_stream ) )
        {
            for( File file : files )
            {
                if( file.exists() && file.canRead() && ( file.length() > Weather_data.get_history_record_columns()
                                                                                     .length() ) )
                {
                    try( FileInputStream file_input_stream = new FileInputStream( file ) )
                    {
                        ZipEntry zip_entry = new ZipEntry( file.getName() );
                        zip_output_stream.putNextEntry( zip_entry );

                        while( ( length = file_input_stream.read( buffer ) ) >= 0 )
                        {
                            zip_output_stream.write( buffer,
                                                     0,
                                                     length );
                        }
                    }
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                    return;
                }
            }
        }

        response.setContentType( "application/zip" );
        response.addHeader( "Content-Disposition",
                            "attachment; filename=\"" + Directory_listing.COMPRESSED_WEATHER_HISTORY_FILENAME + ".zip\"" );
        response.setContentLength( (int)compressed_file.length() );

        try( FileInputStream file_input_stream = new FileInputStream( compressed_file );
                ServletOutputStream servlet_output_stream = response.getOutputStream() )
        {
            while( ( length = file_input_stream.read( buffer ) ) >= 0 )
            {
                servlet_output_stream.write( buffer,
                                             0,
                                             length );
            }
        }
        finally
        {
            compressed_file.delete();
        }
    }

    private static void send_directory_listing( HttpServletResponse response ) throws IOException
    {
        Directory_listing directory_listing = new Directory_listing();

        File directory = new File( Directory_listing.WEATHER_HISTORY_DIRECTORY );
        File[] files = directory.listFiles( ( dir,
                                              name ) -> name.matches( Directory_listing.WEATHER_HISTORY_FILENAME_REGEX ) );
        for( File file : files )
        {
            directory_listing.add_file( file );
        }

        String directory_listing_JSON_string = directory_listing.serialize_to_JSON();
        JSONObject directory_listing_JSON = new JSONObject( directory_listing_JSON_string );

        JSONObject json_response = new JSONObject();
        json_response.put( "directory_listing",
                           directory_listing_JSON );

        json_response.put( "success",
                           "true" );

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( json_response );
        out.close();

        return;
    }

    private static void send_all( HttpServletResponse response ) throws IOException
    {
        byte[] buffer = new byte[4096];
        int length;

        File directory = new File( Directory_listing.WEATHER_HISTORY_DIRECTORY );
        File[] files = directory.listFiles( ( dir,
                                              name ) -> name.matches( Directory_listing.WEATHER_HISTORY_FILENAME_REGEX ) );

        File compressed_file = File.createTempFile( Directory_listing.COMPRESSED_WEATHER_HISTORY_FILENAME,
                                                    ".zip" );
        try( FileOutputStream file_output_stream = new FileOutputStream( compressed_file );
                ZipOutputStream zip_output_stream = new ZipOutputStream( file_output_stream ) )
        {
            for( File file : files )
            {
                try( FileInputStream file_input_stream = new FileInputStream( file ) )
                {
                    ZipEntry zip_entry = new ZipEntry( file.getName() );
                    zip_output_stream.putNextEntry( zip_entry );

                    while( ( length = file_input_stream.read( buffer ) ) >= 0 )
                    {
                        zip_output_stream.write( buffer,
                                                 0,
                                                 length );
                    }
                }
            }
        }

        response.setContentType( "application/zip" );
        response.addHeader( "Content-Disposition",
                            "attachment; filename=\"" + Directory_listing.COMPRESSED_WEATHER_HISTORY_FILENAME + ".zip\"" );
        response.setContentLength( (int)compressed_file.length() );

        try( FileInputStream file_input_stream = new FileInputStream( compressed_file );
                ServletOutputStream servlet_output_stream = response.getOutputStream() )
        {
            while( ( length = file_input_stream.read( buffer ) ) >= 0 )
            {
                servlet_output_stream.write( buffer,
                                             0,
                                             length );
            }
        }
        finally
        {
            compressed_file.delete();
        }
    }
}
