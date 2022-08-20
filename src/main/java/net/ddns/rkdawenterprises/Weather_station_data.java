
package net.ddns.rkdawenterprises;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import net.ddns.rkdawenterprises.Find.DISCOVERY;

@WebServlet( name = "Weather_station_data",
             description = "Returns weather data",
             urlPatterns = { "/weather_station_data" } )
public class Weather_station_data extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
        DISCOVERY discovery = (DISCOVERY)getServletContext().getAttribute( "identity" );

        Weather_data weather_data;
        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        do
        {
            try
            {
                weather_data = get_weather_data( discovery );

                System.out.printf( "Weather data aquired @ %s%n",
                                   weather_data.time );

                Gson gson = new GsonBuilder().create();
                String weather_data_json_string = gson.toJson( weather_data );
                JSONObject weather_data_json = new JSONObject( weather_data_json_string );

                JSONObject json_response = new JSONObject();
                json_response.put( "weather_data",
                                   weather_data_json );

                json_response.put( "success",
                                   "true" );

                response.setContentType( "application/json" );

                PrintWriter out = response.getWriter();
                out.print( json_response );
                out.close();

                return;
            }
            catch( Exception exception )
            {
                retries--;
                System.err.format( "Weather_station_data.doGet: Issue getting data from weather station, retry %d of %d:%s%n",
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT - retries,
                                   Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT,
                                   exception );
                continue;
            }
        }
        while( retries > 0 );

        Utilities.reload_application( getServletContext() );

        throw new ServletException( "Failed to get weather data" );
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
     */
    public static Weather_data get_weather_data( DISCOVERY discovery ) throws IOException, InterruptedException
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
                Weather_data weather_data = Commands.get_weather_data( in,
                                                                       out );
                weather_data.DID = discovery.DID;
                return weather_data;
            }
        }
    }
}
