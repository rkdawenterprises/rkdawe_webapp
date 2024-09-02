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

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Weather_gov_API_service
{
    @Headers( "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)" )
    @GET( "points/{latitude},{longitude}" )
    Call< String > get_points( @Path( "latitude" ) String latitude,
                               @Path( "longitude" ) String longitude );

    @Headers( "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)" )
    @GET( "gridpoints/{wfo}/{x},{y}" )
    Call< String > get_gridpoints( @Path( "wfo" ) String wfo,
                                   @Path( "x" ) String x,
                                   @Path( "y" ) String y );

    @Headers( { "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)",
            "Feature-Flags: forecast_temperature_qv,forecast_wind_speed_qv" } )
    @GET( "gridpoints/{wfo}/{x},{y}/forecast" )
    Call< String > get_gridpoints_forecast( @Path( "wfo" ) String wfo,
                                            @Path( "x" ) String x,
                                            @Path( "y" ) String y );

    @Headers( { "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)",
            "Feature-Flags: forecast_temperature_qv,forecast_wind_speed_qv" } )
    @GET( "gridpoints/{wfo}/{x},{y}/forecast/hourly" )
    Call< String > get_gridpoints_forecast_hourly( @Path( "wfo" ) String wfo,
                                                   @Path( "x" ) String x,
                                                   @Path( "y" ) String y );

    @Headers( "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)" )
    @GET( "gridpoints/{wfo}/{x},{y}/stations" )
    Call< String > get_gridpoints_stations( @Path( "wfo" ) String wfo,
                                            @Path( "x" ) String x,
                                            @Path( "y" ) String y );

    @Headers( "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)" )
    @GET( "stations/{station_id}/observations/latest" )
    Call< String > get_stations_observations_latest( @Path( "station_id" ) String wfo );

    @Headers( "User-Agent: (Weather Station Donna Android App,rkdawenterprises@gmail.com)" )
    @GET( "alerts/active" )
    Call< String > get_alerts_active( @Query( "point" ) String point );
}
