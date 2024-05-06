
package net.ddns.rkdawenterprises.rkdawe_webapp;

public class API_paths
{
    public transient static final String API_PATH = "/v1";

    public static final String AUTHENTICATE_PATH = API_PATH + "/authenticate";
    public static final String GET_LOGGED_IN_PATH = API_PATH + "/get_logged_in";
    public static final String INITIALIZE_WEATHER_STATION_PATH = API_PATH + "/initialize_weather_station";
    public static final String LOG_OUT_PATH = API_PATH + "/log_out";
    public static final String WEATHER_STATION_DATA_PATH = API_PATH + "/weather_station_data";
    public static final String WEATHER_STATION_DONNA_APP_INFO_PATH = API_PATH + "/weather_station_donna_app_info";
    public transient static final String WEBAUTHN_API_PATH = API_PATH + "/webauthn";
    public static final String WEBAUTHN_VERSION_PATH = WEBAUTHN_API_PATH + "/version";
    public static final String WEBAUTHN_REGISTER_PATH = WEBAUTHN_API_PATH + "/register";
    public static final String WEBAUTHN_DEREGISTER_PATH = WEBAUTHN_API_PATH + "/deregister";
    public static final String WEBAUTHN_DELETE_PATH = WEBAUTHN_API_PATH + "/delete";
    public static final String WEBAUTHN_AUTHENTICATE_PATH = WEBAUTHN_API_PATH + "/authenticate";
}
