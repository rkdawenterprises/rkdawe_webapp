package net.ddns.rkdawenterprises;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class Directory_listing
{
    public static final class File_info
    {
        public String m_name;
        public long m_size;
        public ZonedDateTime m_last_modified_time;
        public ZonedDateTime m_creation_time;
        public ZonedDateTime m_last_access_time;

        File_info( File file ) throws IOException
        {
            BasicFileAttributes attributes = Files.readAttributes( file.toPath(),
                                                                   BasicFileAttributes.class );
            m_name = file.getName();
            m_size = attributes.size();
            m_last_modified_time = attributes.lastModifiedTime()
                                             .toInstant()
                                             .atZone( ZoneId.systemDefault() )
                                             .withZoneSameInstant( ZoneId.of( "UTC" ) );
            m_creation_time = attributes.creationTime()
                                        .toInstant()
                                        .atZone( ZoneId.systemDefault() )
                                        .withZoneSameInstant( ZoneId.of( "UTC" ) );
            m_last_access_time = attributes.lastAccessTime()
                                           .toInstant()
                                           .atZone( ZoneId.systemDefault() )
                                           .withZoneSameInstant( ZoneId.of( "UTC" ) );
        }
    }

    List< Directory_listing.File_info > m_file_list = new ArrayList< Directory_listing.File_info >( 16 );

    public void add_file( File file ) throws IOException
    {
        m_file_list.add( new File_info( file ) );
    }

    public static final Gson m_GSON = new GsonBuilder().registerTypeAdapter( ZonedDateTime.class,
                                                                             new TypeAdapter< ZonedDateTime >()
                                                                             {
                                                                                 @Override
                                                                                 public void write( JsonWriter out,
                                                                                                    ZonedDateTime value )
                                                                                         throws IOException
                                                                                 {
                                                                                     out.value( value.toString() );
                                                                                 }

                                                                                 @Override
                                                                                 public ZonedDateTime read( JsonReader in )
                                                                                         throws IOException
                                                                                 {
                                                                                     return ZonedDateTime.parse( in.nextString() );
                                                                                 }
                                                                             } )
                                                       .enableComplexMapKeySerialization()
                                                       .disableHtmlEscaping()
                                                       .setPrettyPrinting()
                                                       .create();

    public static String serialize_to_JSON( Directory_listing object )
    {
        return m_GSON.toJson( object );
    }

    public static Directory_listing deserialize_from_JSON( String string_JSON )
    {
        Directory_listing object = null;
        try
        {
            object = m_GSON.fromJson( string_JSON,
                                      Directory_listing.class );
        }
        catch( com.google.gson.JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for File_info: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }
}