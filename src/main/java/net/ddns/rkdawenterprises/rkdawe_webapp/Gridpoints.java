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

public class Gridpoints
{
    public List< String > context = null;
    public String id = null;
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
        public String id = null;
        public String type = null;
        public String updateTime = null;
        public String validTimes = null;
        public Elevation elevation = null;
        public String forecastOffice = null;
        public String gridId = null;
        public String gridX = null;
        public String gridY = null;
        public Temperature temperature = null;
        public Dewpoint dewpoint = null;
        public MaxTemperature maxTemperature = null;
        public MinTemperature minTemperature = null;
        public RelativeHumidity relativeHumidity = null;
        public ApparentTemperature apparentTemperature = null;
        public WetBulbGlobeTemperature wetBulbGlobeTemperature = null;
        public HeatIndex heatIndex = null;
        public WindChill windChill = null;
        public SkyCover skyCover = null;
        public WindDirection windDirection = null;
        public WindSpeed windSpeed = null;
        public WindGust windGust = null;
        public Weather weather = null;
        public Hazards hazards = null;
        public ProbabilityOfPrecipitation probabilityOfPrecipitation = null;
        public QuantitativePrecipitation quantitativePrecipitation = null;
        public IceAccumulation iceAccumulation = null;
        public SnowfallAmount snowfallAmount = null;
        public SnowLevel snowLevel = null;
        public CeilingHeight ceilingHeight = null;
        public Visibility visibility = null;
        public TransportWindSpeed transportWindSpeed = null;
        public TransportWindDirection transportWindDirection = null;
        public MixingHeight mixingHeight = null;
        public HainesIndex hainesIndex = null;
        public LightningActivityLevel lightningActivityLevel = null;
        public TwentyFootWindSpeed twentyFootWindSpeed = null;
        public TwentyFootWindDirection twentyFootWindDirection = null;
        public WaveHeight waveHeight = null;
        public WavePeriod wavePeriod = null;
        public WaveDirection waveDirection = null;
        public PrimarySwellHeight primarySwellHeight = null;
        public PrimarySwellDirection primarySwellDirection = null;
        public SecondarySwellHeight secondarySwellHeight = null;
        public SecondarySwellDirection secondarySwellDirection = null;
        public WavePeriod2 wavePeriod2 = null;
        public WindWaveHeight windWaveHeight = null;
        public DispersionIndex dispersionIndex = null;
        public Pressure pressure = null;
        public ProbabilityOfTropicalStormWinds probabilityOfTropicalStormWinds = null;
        public ProbabilityOfHurricaneWinds probabilityOfHurricaneWinds = null;
        public PotentialOf15mphWinds potentialOf15mphWinds = null;
        public PotentialOf25mphWinds potentialOf25mphWinds = null;
        public PotentialOf35mphWinds potentialOf35mphWinds = null;
        public PotentialOf45mphWinds potentialOf45mphWinds = null;
        public PotentialOf20mphWindGusts potentialOf20mphWindGusts = null;
        public PotentialOf30mphWindGusts potentialOf30mphWindGusts = null;
        public PotentialOf40mphWindGusts potentialOf40mphWindGusts = null;
        public PotentialOf50mphWindGusts potentialOf50mphWindGusts = null;
        public PotentialOf60mphWindGusts potentialOf60mphWindGusts = null;
        public GrasslandFireDangerIndex grasslandFireDangerIndex = null;
        public ProbabilityOfThunder probabilityOfThunder = null;
        public DavisStabilityIndex davisStabilityIndex = null;
        public AtmosphericDispersionIndex atmosphericDispersionIndex = null;
        public LowVisibilityOccurrenceRiskIndex lowVisibilityOccurrenceRiskIndex = null;
        public Stability stability = null;
        public RedFlagThreatIndex redFlagThreatIndex = null;
    }

    public class Elevation
    {
        public String unitCode = null;
        public BigDecimal value = null;
    }

    public class Temperature
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class Dewpoint
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class MaxTemperature
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class MinTemperature
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class RelativeHumidity
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class ApparentTemperature
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WetBulbGlobeTemperature
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class HeatIndex
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WindChill
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class SkyCover
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WindDirection
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WindSpeed
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WindGust
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class Weather
    {
        public List< Value_weather > values = null;
    }

    public class Hazards
    {
        public List< Value_hazards > values = null;
    }

    public class ProbabilityOfPrecipitation
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class QuantitativePrecipitation
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class IceAccumulation
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class SnowfallAmount
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class SnowLevel
    {
        public List< Object > values = null;
    }

    public class CeilingHeight
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class Visibility
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class TransportWindSpeed
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class TransportWindDirection
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class MixingHeight
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class HainesIndex
    {
        public List< Value > values = null;
    }

    public class LightningActivityLevel
    {
        public List< Object > values = null;
    }

    public class TwentyFootWindSpeed
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class TwentyFootWindDirection
    {
        public String uom = null;
        public List< Value > values = null;
    }

    public class WaveHeight
    {
        public List< Object > values = null;
    }

    public class WavePeriod
    {
        public List< Object > values = null;
    }

    public class WaveDirection
    {
        public List< Object > values = null;
    }

    public class PrimarySwellHeight
    {
        public List< Object > values = null;
    }

    public class PrimarySwellDirection
    {
        public List< Object > values = null;
    }

    public class SecondarySwellHeight
    {
        public List< Object > values = null;
    }

    public class SecondarySwellDirection
    {
        public List< Object > values = null;
    }

    public class WavePeriod2
    {
        public List< Object > values = null;
    }

    public class WindWaveHeight
    {
        public List< Object > values = null;
    }

    public class DispersionIndex
    {
        public List< Object > values = null;
    }

    public class Pressure
    {
        public List< Object > values = null;
    }

    public class ProbabilityOfTropicalStormWinds
    {
        public List< Object > values = null;
    }

    public class ProbabilityOfHurricaneWinds
    {
        public List< Object > values = null;
    }

    public class PotentialOf15mphWinds
    {
        public List< Object > values = null;
    }

    public class PotentialOf20mphWindGusts
    {
        public List< Object > values = null;
    }

    public class PotentialOf25mphWinds
    {
        public List< Object > values = null;
    }

    public class PotentialOf30mphWindGusts
    {
        public List< Object > values = null;
    }

    public class PotentialOf35mphWinds
    {
        public List< Object > values = null;
    }

    public class PotentialOf40mphWindGusts
    {
        public List< Object > values = null;
    }

    public class PotentialOf45mphWinds
    {
        public List< Object > values = null;
    }

    public class PotentialOf50mphWindGusts
    {
        public List< Object > values = null;
    }

    public class PotentialOf60mphWindGusts
    {
        public List< Object > values = null;
    }

    public class GrasslandFireDangerIndex
    {
        public List< Object > values = null;
    }

    public class ProbabilityOfThunder
    {
        public List< Object > values = null;
    }

    public class DavisStabilityIndex
    {
        public List< Value > values = null;
    }

    public class AtmosphericDispersionIndex
    {
        public List< Object > values = null;
    }

    public class LowVisibilityOccurrenceRiskIndex
    {
        public List< Value > values = null;
    }

    public class Stability
    {
        public List< Value > values = null;
    }

    public class RedFlagThreatIndex
    {
        public List< Object > values = null;
    }

    public class Value
    {
        public String validTime = null;
        public String value = null;
    }

    public class Value_weather
    {
        public String validTime = null;
        public List< Value > value = null;

        public class Value
        {
            public String coverage = null;
            public String weather = null;
            public String intensity = null;
            public Visibility visibility = null;
            public List< Object > attributes = null;

            public class Visibility
            {
                public String unitCode = null;
                public Object value = null;
            }
        }
    }

    public class Value_hazards
    {
        public String validTime = null;
        public List< Value > value = null;

        public class Value
        {
            public String phenomenon = null;
            public String significance = null;
            public Object eventNumber = null;
        }
    }

    public static final Gson s_GSON = new GsonBuilder().disableHtmlEscaping()
                                                       .setPrettyPrinting()
                                                       .create();

    public static String serialize_to_JSON( Gridpoints object )
    {
        return s_GSON.toJson( object );
    }

    public static Gridpoints deserialize_from_JSON( String string_JSON )
        throws JsonSyntaxException
    {
        Gridpoints object = null;
        try
        {
            object = s_GSON.fromJson( string_JSON,
                                      Gridpoints.class );
        }
        catch( JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for Gridpoints: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }
}
