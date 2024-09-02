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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.math.BigDecimal;

public class Gridpoints_forecast
{
    public List< String > context = null;
    public String type = null;
    public Geometry geometry = null;
    public Properties properties = null;

    public class Geometry
    {
        public String type = null;
        public List< List< List< BigDecimal > > > coordinates = null;
    }

    public class Properties
    {
        public String units = null;
        public String forecastGenerator = null;
        public String generatedAt = null;
        public String updateTime = null;
        public String validTimes = null;
        public Elevation elevation = null;
        public List< Period > periods = null;
    }

    public class Elevation
    {
        public String unitCode = null;
        public BigDecimal value = null;
    }

    public class Period
    {
        public Integer number = null;
        public String name = null;
        public String startTime = null;
        public String endTime = null;
        public Boolean isDaytime = null;
        public Temperature temperature = null;
        public String temperatureTrend = null;
        public ProbabilityOfPrecipitation probabilityOfPrecipitation = null;
        public WindSpeed windSpeed = null;
        public Object windGust = null;
        public String windDirection = null;
        public String icon = null;
        public String shortForecast = null;
        public String detailedForecast = null;
    }

    public class Temperature
    {
        public String unitCode = null;
        public BigDecimal value = null;
    }

    public class ProbabilityOfPrecipitation
    {
        public String unitCode = null;
        public Integer value = null;
    }

    public class WindSpeed
    {
        public String unitCode = null;
        public BigDecimal maxValue = null;
        public BigDecimal minValue = null;
        public BigDecimal value = null;
    }

    public static final Gson s_GSON = new GsonBuilder().disableHtmlEscaping()
                                                       .setPrettyPrinting()
                                                       .create();

    public static String serialize_to_JSON( Gridpoints_forecast object )
    {
        return s_GSON.toJson( object );
    }

    public static Gridpoints_forecast deserialize_from_JSON( String string_JSON )
        throws JsonSyntaxException
    {
        Gridpoints_forecast object = null;
        try
        {
            object = s_GSON.fromJson( string_JSON,
                                      Gridpoints_forecast.class );
        }
        catch( JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for Gridpoints_forecast: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }
}
