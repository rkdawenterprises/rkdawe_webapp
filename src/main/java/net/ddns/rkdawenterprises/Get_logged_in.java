package net.ddns.rkdawenterprises;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

@WebServlet( name = "Get_logged_in", description = "Returns session logged-in status", urlPatterns = { "/get_logged_in" })
public class Get_logged_in extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response ) throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        JSONObject json_response = new JSONObject();

        String logged_in = (String)session.getAttribute( "logged_in" );
        if( ( logged_in != null ) && logged_in.equals( "true" ) )
        {
            json_response.put( "logged_in", logged_in );
            json_response.put( "username", session.getAttribute( "username" ) );
        }
        else
        {
            json_response.put( "logged_in", "false" );
        }

        json_response.put( "success", "true" );

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( json_response );
        out.close();
    }
}
