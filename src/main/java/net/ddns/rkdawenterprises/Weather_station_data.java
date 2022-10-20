
package net.ddns.rkdawenterprises;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
                String latest_parameter = request.getParameter( "latest" );
                if( latest_parameter == null ) latest_parameter = "false";
                boolean latest = latest_parameter.equalsIgnoreCase("true");

                weather_data = Weather_station_access.get_instance().get_weather_data( latest );
                if( weather_data == null )
                {
                    retries--;
                    continue;
                }

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
