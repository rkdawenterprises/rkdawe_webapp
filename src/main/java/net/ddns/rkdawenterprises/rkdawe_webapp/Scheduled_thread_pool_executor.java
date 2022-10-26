
package net.ddns.rkdawenterprises.rkdawe_webapp;

import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * For added visibility logging exceptions.
 */
public class Scheduled_thread_pool_executor extends ScheduledThreadPoolExecutor implements ThreadFactory
{
    public Scheduled_thread_pool_executor()
    {
        this( 8 );
    }

    public Scheduled_thread_pool_executor( int corePoolSize )
    {
        super( corePoolSize );
        setThreadFactory( this );
    }

    public Scheduled_thread_pool_executor( int corePoolSize,
                                           RejectedExecutionHandler handler )
    {
        super( corePoolSize,
               handler );
        setThreadFactory( this );
    }

    public Scheduled_thread_pool_executor( int corePoolSize,
                                           ThreadFactory threadFactory )
    {
        super( corePoolSize,
               threadFactory );
    }

    public Scheduled_thread_pool_executor( int corePoolSize,
                                           ThreadFactory threadFactory,
                                           RejectedExecutionHandler handler )
    {
        super( corePoolSize,
               threadFactory,
               handler );
    }

    @Override
    protected void afterExecute( Runnable r,
                                 Throwable t )
    {
        super.afterExecute( r,
                            t );

        // This will log every scheduled tasks...
        // if( ( t == null ) && ( r instanceof Future< ? > ) )
        // {
        //     System.out.println( r.toString() );
        // }

        if( t != null )
        {
            System.out.println( t.toString() );
        }
    }

    Thread.UncaughtExceptionHandler uncaught_exception_handler = new Thread.UncaughtExceptionHandler()
    {
        @Override
        public void uncaughtException( Thread thread,
                                       Throwable exception )
        {
            System.out.println( exception );
        }
    };

    @Override
    public Thread newThread( Runnable r )
    {
        Thread thread = Executors.defaultThreadFactory()
                                 .newThread( r );
        thread.setUncaughtExceptionHandler( uncaught_exception_handler );
        return thread;
    }
}
