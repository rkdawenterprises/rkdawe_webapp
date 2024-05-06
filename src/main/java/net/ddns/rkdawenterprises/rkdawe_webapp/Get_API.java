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

@WebServlet( name = "Get_API",
             description = "Returns URL paths for the RKDAWE API",
             urlPatterns = { API_paths.API_PATH } )
public class Get_API extends HttpServlet
{
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response ) throws ServletException, IOException
    {
        GsonBuilder gsonBuilder  = new GsonBuilder();
        gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
        Gson gson = gsonBuilder.create();
        String paths_string = gson.toJson(new API_paths());
        JSONObject paths = new JSONObject(paths_string);

        JSONObject response_JSON = new JSONObject();
        
        response_JSON.put( "paths", paths );
        response_JSON.put( "success", "true" );

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        if( request.getParameter( "pretty" ) != null )
        {
            out.print( response_JSON.toString( 4 ) );
        }
        else
        {
            out.print( response_JSON );
        }
        out.close();
    }
}
