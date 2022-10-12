
package net.ddns.rkdawenterprises;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import net.ddns.rkdawenterprises.Utilities.ROUNDING_TYPE;

@WebServlet( name = "Initialize_weather_station",
             description = "Initialize weather station at server startup and upon request",
             urlPatterns = { "/initialize_weather_station" },
             loadOnStartup = 1 )
public class Initialize_weather_station extends HttpServlet
{
    public static final int DEFAULT_MAX_RETRY_COUNT = 5;

    private Scheduled_thread_pool_executor m_scheduled_thread_pool_executor = null;
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

        m_scheduled_thread_pool_executor = new Scheduled_thread_pool_executor();

        ZonedDateTime system_now = ZonedDateTime.now();

        ZonedDateTime next_top_of_minute_plus_40s = Utilities.round( system_now,
                                                                     ChronoField.MINUTE_OF_HOUR,
                                                                     1,
                                                                     ROUNDING_TYPE.UP )
                                                             .plusSeconds( 40 );
        Duration delay = Duration.between( system_now,
                                           next_top_of_minute_plus_40s );

        m_scheduled_thread_pool_executor.scheduleAtFixedRate( get_weather_data,
                                                              delay.toNanos(),
                                                              Duration.ofMinutes( 1 )
                                                                      .toNanos(),
                                                              TimeUnit.NANOSECONDS );

        m_application_information = Utilities.get_pom_properties( getServletContext() );
        System.out.println( "Initialize_weather_station: Application information: " + m_application_information );
        System.out.format( "Initialize_weather_station: Finished initializing weather station.%n" );
    }

    private void stop()
    {
        if( m_scheduled_thread_pool_executor != null )
        {
            m_scheduled_thread_pool_executor.shutdownNow();
            m_scheduled_thread_pool_executor.purge();
            m_scheduled_thread_pool_executor = null;
        }
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
            for( String string : application_information )
            {
                if( string.startsWith( "version=" ) )
                {
                    String[] version_pair = string.split( "=" );
                    json_response.put( version_pair[0],
                                       version_pair[1] );
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

    private void initialize() throws SocketException, UnknownHostException, WSD_exception, IOException
    {
        Weather_station_access.get_instance()
                              .initialize();

        m_reset_countdown_minutes = RESET_COUNTDOWN_MINUTES;
    }

    private Runnable get_weather_data = new Runnable()
    {
        @Override
        public void run()
        {
            m_reset_countdown_minutes--;

            try
            {
                if( m_reset_countdown_minutes <= 0 )
                {
                    initialize();
                }

                Weather_station_access.get_instance()
                                      .get_save_weather_record();
            }
            catch( Exception exception )
            {
                System.out.println( exception );
                Utilities.reload_application( getServletContext() );
            }
        }
    };
}
