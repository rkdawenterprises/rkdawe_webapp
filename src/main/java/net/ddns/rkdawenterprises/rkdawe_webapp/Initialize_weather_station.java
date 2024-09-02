/*
 * Copyright (c) 2023-2024 RKDAW Enterprises and Ralph Williamson.
 *       email: rkdawenterprises@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import net.ddns.rkdawenterprises.rkdawe_api_common.Server_utilities;
import net.ddns.rkdawenterprises.rkdawe_api_common.WSD_exception;

@WebServlet( name = "Initialize_weather_station",
             description = "Initialize weather station at server startup and upon request",
             urlPatterns = { "/initialize_weather_station", API_paths.INITIALIZE_WEATHER_STATION_PATH },
             loadOnStartup = 1 )
public class Initialize_weather_station extends HttpServlet
{
    public static final int DEFAULT_MAX_RETRY_COUNT = 5;
    private String m_application_information = null;
    public static final int RESET_COUNTDOWN_MINUTES = 60;

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

        m_application_information = Server_utilities.get_pom_properties( getServletContext() );
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

        JSONObject response_JSON = new JSONObject();

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
                response_JSON.put( "error",
                                   e.toString() );
                response_JSON.put( "success",
                                   "false" );

                response.setContentType( "application/json" );

                PrintWriter out = response.getWriter();
                out.print( response_JSON );
                out.close();
                return;
            }

            response_JSON.put( "success",
                               "true" );

            String[] application_information = m_application_information.split( System.lineSeparator() );
            System.out.print( Arrays.toString( application_information ) );
            for( String string : application_information )
            {
                if( string.startsWith( "SCM-Revision: " ) )
                {
                    String[] version_pair = string.split( ":" );
                    response_JSON.put( "version",
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
        out.print( response_JSON );
        out.close();
    }

    private void initialize()
            throws SocketException, UnknownHostException, WSD_exception, IOException, InterruptedException
    {
        Weather_station_access.get_instance()
                              .initialize();
    }
}
