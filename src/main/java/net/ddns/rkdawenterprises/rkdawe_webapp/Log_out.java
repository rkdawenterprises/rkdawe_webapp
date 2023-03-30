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

@WebServlet( name = "Log_out",
             description = "Invalidates the current session",
             urlPatterns = { "/log_out" } )
public class Log_out extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response ) throws ServletException, IOException
    {
        HttpSession session = request.getSession();
        session.invalidate();

        JSONObject json_response = new JSONObject();
        json_response.put( "logged_in", "false" );
        json_response.put( "success", "true" );

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( json_response );
        out.close();
    }
}
