
package net.ddns.rkdawenterprises.rkdawe_webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import net.ddns.rkdawenterprises.rkdawe_api_common.Utilities;
import net.ddns.rkdawenterprises.rkdawe_api_common.WSD_exception;

@WebServlet( name = "Initialize_weather_station",
             description = "Initialize weather station at server startup and upon request",
             urlPatterns = { "/initialize_weather_station" },
             loadOnStartup = 1 )
public class Initialize_weather_station extends HttpServlet
{
    public static final int DEFAULT_MAX_RETRY_COUNT = 5;

    private String m_application_information = null;

    public static final int RESET_COUNTDOWN_MINUTES = 60;
    private int m_reset_countdown_minutes;

    @Override
    public void init() throws ServletException
    {
        stop();

        try
        {
            System.out.format( "Initialize_weather_station: Initializing weather station...%n" );
            initialize();
        }
        catch( Exception exception )
        {
            System.err.println( "Initialize_weather_station: Failed!!! Error: " + exception.toString() );
            throw new ServletException( exception.toString() );
        }

        m_application_information = Utilities.get_pom_properties( getServletContext() );
        System.out.println( "Initialize_weather_station: Application information:\n" + m_application_information );
        System.out.format( "Initialize_weather_station: Finished initializing weather station.%n" );
    }

    private void stop()
    {
    }

    @Override
    public void destroy()
    {
        stop();
    }

    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        JSONObject json_response = new JSONObject();

        String logged_in = (String)session.getAttribute( "logged_in" );
        if( ( logged_in != null ) && logged_in.equals( "true" ) )
        {
            try
            {
                System.out.format( "Initialize_weather_station: Initializing weather station...%n" );
                stop();
                initialize();
            }
            catch( Exception e )
            {
                json_response.put( "error",
                                   e.toString() );
                json_response.put( "success",
                                   "false" );

                response.setContentType( "application/json" );

                PrintWriter out = response.getWriter();
                out.print( json_response );
                out.close();
                return;
            }

            json_response.put( "success",
                               "true" );

            String[] application_information = m_application_information.split( System.lineSeparator() );
            System.out.print(Arrays.toString(application_information));
            for( String string : application_information )
            {
                if( string.startsWith( "SCM-Revision: " ) )
                {
                    String[] version_pair = string.split( ":" );
                    json_response.put( "version",
                                       version_pair[1].trim() );
                    break;
                }
            }
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( json_response );
        out.close();
    }

    private void initialize() throws SocketException, UnknownHostException, WSD_exception, IOException, InterruptedException
    {
        Weather_station_access.get_instance()
                              .initialize();

        m_reset_countdown_minutes = RESET_COUNTDOWN_MINUTES;
    }
}
