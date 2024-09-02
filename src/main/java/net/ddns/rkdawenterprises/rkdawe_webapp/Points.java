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

public class Points
{
    public List< String > context;
    public String id;
    public String type;
    public Geometry geometry;
    public Properties properties;

    public class Geometry
    {
        public String type;
        public List< BigDecimal > coordinates;
    }

    public class Properties
    {
        public String id;
        public String type;
        public String cwa;
        public String forecastOffice;
        public String gridId;
        public Integer gridX;
        public Integer gridY;
        public String forecast;
        public String forecastHourly;
        public String forecastGridData;
        public String observationStations;
        public RelativeLocation relativeLocation;
        public String forecastZone;
        public String county;
        public String fireWeatherZone;
        public String timeZone;
        public String radarStation;
    }

    public class Distance
    {
        public String unitCode;
        public Double value;
    }

    public class Bearing
    {
        public String unitCode;
        public Integer value;
    }

    public class RelativeLocation
    {
        public class Geometry
        {
            public String type;
            public List< BigDecimal > coordinates;
        }

        public class Properties
        {
            public String city;
            public String state;
            public Distance distance;
            public Bearing bearing;
        }

        public String type;
        public Geometry geometry;
        public Properties properties;
    }

    public static final Gson s_GSON = new GsonBuilder().disableHtmlEscaping()
                                                       .setPrettyPrinting()
                                                       .create();

    public static String serialize_to_JSON( Points object )
    {
        return s_GSON.toJson( object );
    }

    public static Points deserialize_from_JSON( String string_JSON )
        throws JsonSyntaxException
    {
        Points object = null;
        try
        {
            object = s_GSON.fromJson( string_JSON,
                                      Points.class );
        }
        catch( JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for Points: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }
}
