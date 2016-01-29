package org.denovogroup.murmur.ui;

import android.app.Application;
import android.content.Context;

import org.denovogroup.murmur.backend.ConfigureLog4J;
import org.denovogroup.murmur.backend.Log4JExceptionHandler;

/**
 * Created by Liran on 9/1/2015.
 */
public class MurmurApplication extends Application {

    private static String TAG = "MurmurApplication";
    private static  Context context;

    @Override
    public final void onCreate() {
        super.onCreate();
        context = this;

        ConfigureLog4J.configure(false);
        Thread.setDefaultUncaughtExceptionHandler(new Log4JExceptionHandler(Thread.getDefaultUncaughtExceptionHandler()));

    }

    public static Context getContext(){
        return context;
    }
}
