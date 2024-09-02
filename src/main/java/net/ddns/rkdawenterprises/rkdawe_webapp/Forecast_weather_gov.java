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
import java.math.BigDecimal;
import java.math.RoundingMode;
import retrofit2.Call;
import retrofit2.Response;
import com.google.gson.JsonSyntaxException;

public class Forecast_weather_gov
{
    public static Weather_gov_data get_forecast( String location_string ) throws IOException, JsonSyntaxException
    {
        Points points = get_points( location_string );

        Gridpoints gridpoints = get_gridpoints( points.properties.cwa,
                                                points.properties.gridX,
                                                points.properties.gridY );

        Gridpoints_forecast gridpoints_forecast = get_gridpoints_forecast( points.properties.cwa,
                                                                           points.properties.gridX,
                                                                           points.properties.gridY );

        return Weather_gov_data.with(points.properties.relativeLocation.properties.city,
                                     points.properties.relativeLocation.properties.state,
                                     gridpoints,
                                     gridpoints_forecast );
    }

    /**
     * Formats the forecast location setting string into lat/long strings with 4
     * decimal digit precision.
     *
     * @param location_string CSV string with latitude and longitude in decimal
     *                        degrees.
     * 
     * @throws NumberFormatException
     *
     * @return String array with [0] latitude and [1] longitude.
     */
    private static String[] process_location_for_get_points( String location_string )
    {
        String[] location = location_string.split( "," );
        if( location.length == 2 )
        {
            BigDecimal latitude = new BigDecimal( location[0].trim() ).setScale( 4,
                                                                                 RoundingMode.HALF_EVEN );
            BigDecimal longitude = new BigDecimal( location[1].trim() ).setScale( 4,
                                                                                  RoundingMode.HALF_EVEN );
            String[] result = { latitude.toString(), longitude.toString() };
            return result;
        }

        throw( new NumberFormatException( "Bad location string" ) );
    }

    private static Points get_points( String location_string ) throws IOException, JsonSyntaxException
    {
        String[] location = process_location_for_get_points( location_string );
        String latitude = location[0];
        String longitude = location[1];

        Call< String > call = Weather_gov_API.s_weather_gov_API_service.get_points( latitude,
                                                                                    longitude );
        Response< String > response = call.execute();
        String points_JSON = response.body();

        Points points = Points.deserialize_from_JSON( points_JSON );
        return points;
    }

    private static Gridpoints get_gridpoints( String wfo,
                                              Integer x,
                                              Integer y )
            throws IOException, JsonSyntaxException
    {
        Call< String > call = Weather_gov_API.s_weather_gov_API_service.get_gridpoints( wfo,
                                                                                        Integer.toString( x ),
                                                                                        Integer.toString( y ) );
        Response< String > response = call.execute();
        String gridpoints_JSON = response.body();

        Gridpoints gridpoints = Gridpoints.deserialize_from_JSON( gridpoints_JSON );
        return gridpoints;
    }

    private static Gridpoints_forecast get_gridpoints_forecast( String wfo,
                                                                Integer x,
                                                                Integer y )
            throws IOException, JsonSyntaxException
    {
        Call< String > call = Weather_gov_API.s_weather_gov_API_service.get_gridpoints_forecast( wfo,
                                                                                                 Integer.toString( x ),
                                                                                                 Integer.toString( y ) );
        Response< String > response = call.execute();
        String gridpoints_forecast_JSON = response.body();

        Gridpoints_forecast gridpoints_forecast = Gridpoints_forecast.deserialize_from_JSON( gridpoints_forecast_JSON );
        return gridpoints_forecast;
    }
}
