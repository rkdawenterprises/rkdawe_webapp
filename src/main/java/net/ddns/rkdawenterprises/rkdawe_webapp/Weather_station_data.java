
package net.ddns.rkdawenterprises.rkdawe_webapp;

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

import net.ddns.rkdawenterprises.rkdawe_api_common.Server_utilities;
import net.ddns.rkdawenterprises.rkdawe_api_common.Weather_data;

@WebServlet( name = "Weather_station_data",
             description = "Returns weather data",
             urlPatterns = { "/weather_station_data", API_paths.WEATHER_STATION_DATA_PATH } )
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
                weather_data = Weather_station_access.get_instance().get_weather_data();
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

                JSONObject response_JSON = new JSONObject();
                response_JSON.put( "weather_data",
                                   weather_data_json );

                response_JSON.put( "success",
                                   "true" );

                response.setContentType( "application/json" );

                PrintWriter out = response.getWriter();
                out.print( response_JSON );
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

        Server_utilities.reload_application( getServletContext() );

        throw new ServletException( "Failed to get weather data" );
    }
}
