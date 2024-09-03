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

import net.ddns.rkdawenterprises.rkdawe_api_common.API_paths;

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
