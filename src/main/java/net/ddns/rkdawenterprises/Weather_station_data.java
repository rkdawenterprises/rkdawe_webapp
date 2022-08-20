
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
        Weather_data weather_data;
        int retries = Initialize_weather_station.DEFAULT_MAX_RETRY_COUNT;
        do
        {
            try
            {
                weather_data = get_weather_data();

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
}
