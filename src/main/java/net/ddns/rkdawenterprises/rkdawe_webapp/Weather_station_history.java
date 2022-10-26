
package net.ddns.rkdawenterprises.rkdawe_webapp;

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
            String type = request.getParameter( "type" );
            if( type == null ) type = "all";
            Weather_history.send_compressed_weather_history( response,
                                                             type );
            return;
        }
        catch( Exception exception )
        {
            throw new ServletException( "Failed to get weather history" );
        }
    }
}
