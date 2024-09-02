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

public class Weather_gov_API
{
    // 34.571712, -92.999596, "cwa": "LZK", "gridX": 56, "gridY": 64

    private static final String s_base_URI = "https://api.weather.gov";

    //private val client: OkHttpClient = OkHttpClient.Builder()
    //    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    //    .build();
    
    private static Retrofit s_retrofit = new Retrofit.Builder()
        .baseUrl(s_base_URI)
    //    .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build();
        
    public static Weather_gov_API_service s_weather_gov_API_service = s_retrofit.create(Weather_gov_API_service.class);
}
