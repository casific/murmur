package org.denovogroup.murmur.backend;


import org.apache.log4j.Logger;

/**
 * Created by Daniel Kopitchinski (danielk.jobs@gmail.com) on 12/6/2014.
 * http://www.tunityapp.com
 */
public class Log4JExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler previous;
    private final static Logger log = Logger.getLogger("Log4JExceptionHandler");
    public Log4JExceptionHandler(Thread.UncaughtExceptionHandler prevHandler) {
        previous = prevHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        log.fatal("Uncaught exception!", ex);
        if(previous != null)
            previous.uncaughtException(thread, ex);
    }
}
