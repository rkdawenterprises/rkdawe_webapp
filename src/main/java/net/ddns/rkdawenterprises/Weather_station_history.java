
package net.ddns.rkdawenterprises;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet( name = "Weather_station_history",
             description = "Returns weather data history in a zip file",
             urlPatterns = { "/weather_station_history" } )
public class Weather_station_history extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
        try
        {
            Weather_station_access.get_instance().send_compressed_weather_history( response );
            return;
        }
        catch( Exception exception )
        {
            throw new ServletException( "Failed to get weather history" );
        }
    }
}
