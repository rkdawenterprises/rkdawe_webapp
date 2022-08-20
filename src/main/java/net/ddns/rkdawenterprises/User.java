
package net.ddns.rkdawenterprises;

import java.time.Instant;

public class User
{
    public enum AUTHENTICATED
    {
        TRUE,
        FALSE,
        DELAY
    }

    public User()
    {
        id = -1;
        username = null;
        email = null;
        created_at = null;
        last_log_in = null;
        last_invalid_attempt = null;
        invalid_attempts = -1;
        authenticated = AUTHENTICATED.FALSE;
    }

    public User( final int id,
                 final String username,
                 final String email,
                 final Instant created_at,
                 final Instant last_log_in,
                 final Instant last_invalid_attempt,
                 final int invalid_attempts,
                 final AUTHENTICATED authenticated )
    {
        this.id = id;
        this.username = username;
        this.email = email;
        this.created_at = created_at;
        this.last_log_in = last_log_in;
        this.last_invalid_attempt = last_invalid_attempt;
        this.invalid_attempts = invalid_attempts;
        this.authenticated = authenticated;
    }

    public static User with( final int id,
                             final String username,
                             final String email,
                             final Instant created_at,
                             final Instant last_log_in,
                             final Instant last_invalid_attempt,
                             final int invalid_attempts,
                             final AUTHENTICATED authenticated )
    {
        return new User( id,
                         username,
                         email,
                         created_at,
                         last_log_in,
                         last_invalid_attempt,
                         invalid_attempts,
                         authenticated );
    }

    public int id = -1;
    public String username = null;
    public String email = null;
    public Instant created_at = null;
    public Instant last_log_in = null;
    public Instant last_invalid_attempt = null;
    public int invalid_attempts = -1;
    public AUTHENTICATED authenticated = AUTHENTICATED.FALSE;
}
