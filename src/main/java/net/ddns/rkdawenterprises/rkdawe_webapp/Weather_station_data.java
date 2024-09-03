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

                String forecast_location = request.getParameter( "forecast_location" );                   
                if( forecast_location != null )
                {
                    System.out.printf( "Getting data for forecast_location = %s%n",
                                       forecast_location );

                    Weather_gov_data weather_gov_data = Forecast_weather_gov.get_forecast(forecast_location);

                    weather_data.period_1_forecast_icon = weather_gov_data.gridpoints_forecast.properties.periods.get(0).icon;
                    weather_data.period_1_short_forecast = weather_gov_data.gridpoints_forecast.properties.periods.get(0).shortForecast;
                }        
                
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
