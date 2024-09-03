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
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import net.ddns.rkdawenterprises.rkdawe_api_common.API_paths;

@WebServlet( name = "Get_logged_in",
             description = "Returns session logged-in status",
             urlPatterns = { "/get_logged_in", API_paths.GET_LOGGED_IN_PATH } )
public class Get_logged_in extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response ) throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        JSONObject response_JSON = new JSONObject();

        String logged_in = (String)session.getAttribute( "logged_in" );
        if( ( logged_in != null ) && logged_in.equals( "true" ) )
        {
            response_JSON.put( "logged_in", logged_in );
            response_JSON.put( "username", session.getAttribute( "username" ) );
        }
        else
        {
            response_JSON.put( "logged_in", "false" );
        }

        response_JSON.put( "success", "true" );

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( response_JSON );
        out.close();
    }
}
