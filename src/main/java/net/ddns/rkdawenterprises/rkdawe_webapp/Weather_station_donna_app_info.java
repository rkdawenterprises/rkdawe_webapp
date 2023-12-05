
package net.ddns.rkdawenterprises.rkdawe_webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ddns.rkdawenterprises.rkdawe_api_common.Server_utilities;

@WebServlet( name = "Weather_station_donna_app_info",
             description = "Returns information regarding the Weather Station Donna App",
             urlPatterns = { "/weather_station_donna_app_info" } )
public class Weather_station_donna_app_info extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
            try
            {
                String app_info_as_string = Server_utilities.resource_file_to_string( "weather_station_donna_app_info.html",
                                                                                      getServletContext() );

                response.setContentType( "text/html" );

                PrintWriter out = response.getWriter();
                out.print( app_info_as_string );
                out.close();

                return;
            }
            catch( Exception exception )
            {
                System.err.format( "Weather_station_donna_app_info.doGet: Issue getting app info file, %s%n",
                                   exception );
            }

        Server_utilities.reload_application( getServletContext() );

        throw new ServletException( "Failed to get app information file" );
    }
}
