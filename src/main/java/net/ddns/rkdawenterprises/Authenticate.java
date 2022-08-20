
package net.ddns.rkdawenterprises;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

@WebServlet( name = "Authenticate",
             description = "Log in authentication servlet",
             urlPatterns = { "/authenticate" } )
public class Authenticate extends HttpServlet
{
    public static final Duration DELAY_AFTER_INVALID_ATTEMPT = Duration.ofSeconds( 5 );

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        JSONObject response_JSON = new JSONObject();

        try
        {
            int key_length = 1024;
            String[] key_pair_base64 = Utilities.generate_RSA_key_pair_base64( key_length );

            response_JSON.put( "algorithm",
                               "RSA" );
            response_JSON.put( "key_length",
                               key_length );
            response_JSON.put( "public_key_base64",
                               key_pair_base64[1] );
            response_JSON.put( "success",
                               "true" );

            session.setAttribute( "private_key_base64",
                                  key_pair_base64[0] );
        }
        catch( NoSuchAlgorithmException exception )
        {
            response_JSON.put( "success",
                               "false" );
            String exception_string = exception.toString()
                                               .split( "\\r?\\n",
                                                       1 )[0];
            if( exception_string.contains( ": " ) )
            {
                exception_string = exception_string.split( ": " )[1];
            }

            response_JSON.put( "comment",
                               exception_string );

            session.invalidate();
        }

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( response_JSON );
        out.close();
    }

    @Override
    protected void doPost( HttpServletRequest request,
                           HttpServletResponse response )
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        // TODO: Add timer for 3 log in attempts per hour?

        JSONObject response_JSON = new JSONObject();

        if( !request.getContentType()
                    .equalsIgnoreCase( "application/json" ) )
        {
            response_JSON.put( "success",
                               "false" );
            response_JSON.put( "comment",
                               "User information must be JSON string (application/json)" );
        }
        else
        {
            String form_data_as_JSON_string = IOUtils.toString( request.getInputStream(),
                                                                "UTF-8" );
            JSONObject form_data_as_JSON_object = new JSONObject( form_data_as_JSON_string );
            String username = form_data_as_JSON_object.getString( "username" );
            String password_encrypted_base64 = form_data_as_JSON_object.getString( "password_encrypted" );

            String private_key_base64 = (String)session.getAttribute( "private_key_base64" );

            String password = "";
            User user = new User();
            try
            {
                password = Utilities.decrypt_RSA_base64( password_encrypted_base64,
                                                         private_key_base64 );

                String logged_in = (String)session.getAttribute( "logged_in" );
                if( ( logged_in != null ) && logged_in.equals( "true" ) )
                {
                    if( !session.getAttribute( "username" )
                                .equals( username ) )
                    {
                        String username_in_db = (String)session.getAttribute( "username" );
                        response_JSON.put( "success",
                                           "already" );
                        response_JSON.put( "comment",
                                           "User \"" + username_in_db + "\" already logged in" );
                        response_JSON.put( "username",
                                           username_in_db );
                        response_JSON.put( "last_log_in",
                                           session.getAttribute( "last_log_in" ) );
                    }
                    else
                    {
                        String username_in_db = (String)session.getAttribute( "username" );
                        response_JSON.put( "success",
                                           "current" );
                        response_JSON.put( "comment",
                                           "User \"" + username_in_db + "\" currently logged in" );
                        response_JSON.put( "username",
                                           username_in_db );
                        response_JSON.put( "last_log_in",
                                           session.getAttribute( "last_log_in" ) );
                    }
                }
                else
                {
                    try
                    {
                        Utilities.authenticate( getServletContext(),
                                                username,
                                                password,
                                                user );

                        response_JSON.put( "success",
                                           "true" );
                        session.setAttribute( "logged_in",
                                              "true" );
                        response_JSON.put( "comment",
                                           "User \"" + user.username + "\" logged in" );

                        response_JSON.put( "username",
                                           user.username );
                        response_JSON.put( "last_log_in",
                                           user.last_log_in );
                        response_JSON.put( "email",
                                           user.email );
                        response_JSON.put( "created_at",
                                           user.created_at );
                        response_JSON.put( "last_invalid_attempt",
                                           user.last_invalid_attempt );
                        response_JSON.put( "invalid_attempts",
                                           user.invalid_attempts );

                        session.setAttribute( "id",
                                              user.id );
                        session.setAttribute( "username",
                                              user.username );
                        session.setAttribute( "email",
                                              user.email );
                        session.setAttribute( "created_at",
                                              user.created_at );
                        session.setAttribute( "last_log_in",
                                              user.last_log_in );
                        session.setAttribute( "last_invalid_attempt",
                                              user.last_invalid_attempt );
                        session.setAttribute( "invalid_attempts",
                                              user.invalid_attempts );

                        Instant current_log_in = Utilities.update_last_log_in( getServletContext(),
                                                                               user.id );

                        response_JSON.put( "current_log_in",
                                           current_log_in );
                        session.setAttribute( "current_log_in",
                                              current_log_in );
                    }
                    catch( SQLException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException
                            | DateTimeException | IllegalArgumentException exception )
                    {
                        System.out.println( exception.toString() );

                        if( ( user.id != -1 ) && ( user.username != null ) )
                        {
                            // System.out.printf( "Known user %s, last_invalid_attempt: %s%n",
                            // user.username, user.last_invalid_attempt );

                            try
                            {
                                ++user.invalid_attempts;
                                /* Instant current_invalid_attempt = */
                                Utilities.update_last_invalid_attempt( getServletContext(),
                                                                       user.id,
                                                                       user.invalid_attempts );
                                // System.out.printf( " current_invalid_attempt: %s, invalid_attempts: %d%n",
                                // current_invalid_attempt, user.invalid_attempts );
                            }
                            catch( ClassNotFoundException | SQLException another_exception )
                            {
                                System.out.println( another_exception.toString() );
                            }
                        }
                        else
                        {
                            System.out.printf( "Unknown user %s%n",
                                               username );
                        }

                        response_JSON.put( "success",
                                           "false" );

                        String exception_string = exception.toString()
                                                           .split( "\\r?\\n",
                                                                   1 )[0];
                        if( exception_string.contains( ": " ) )
                        {
                            exception_string = exception_string.split( ": " )[1];
                        }

                        response_JSON.put( "comment",
                                           exception_string );

                        session.invalidate();
                        Utilities.sleep( DELAY_AFTER_INVALID_ATTEMPT );
                    }
                }
            }
            catch( InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                    | NoSuchPaddingException | InvalidKeySpecException exception )
            {
                System.out.println( exception.toString() );

                response_JSON.put( "success",
                                   "false" );

                String exception_string = exception.toString()
                                                   .split( "\\r?\\n",
                                                           1 )[0];
                if( exception_string.contains( ": " ) )
                {
                    exception_string = exception_string.split( ": " )[1];
                }

                response_JSON.put( "comment",
                                   exception_string );

                session.invalidate();
                Utilities.sleep( DELAY_AFTER_INVALID_ATTEMPT );
            }
        }

        response.setContentType( "application/json" );

        PrintWriter out = response.getWriter();
        out.print( response_JSON );
        out.close();
    }
}
