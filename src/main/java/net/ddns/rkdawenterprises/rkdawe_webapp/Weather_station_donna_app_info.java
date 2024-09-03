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
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ddns.rkdawenterprises.rkdawe_api_common.Server_utilities;
import net.ddns.rkdawenterprises.rkdawe_api_common.API_paths;

@WebServlet( name = "Weather_station_donna_app_info",
             description = "Returns information regarding the Weather Station Donna App",
             urlPatterns = { "/weather_station_donna_app_info", API_paths.WEATHER_STATION_DONNA_APP_INFO_PATH } )
public class Weather_station_donna_app_info extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
            try
            {
                String app_info_as_string = Files.readString(Path.of("/opt/home/tomcat/weather_station_donna_app_info.html"));

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
