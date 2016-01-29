package org.denovogroup.murmur.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import org.denovogroup.murmur.R;

/**
 * Created by Liran on 1/15/2016.
 */
public class SplashActivity extends Activity {

    private static final int SPLASH_DURATION = 2000;

    boolean isFirstLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_activity);

        String prefName = "initializes";
        String prefProperty = "initializes";

        SharedPreferences prefFile = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        if(prefFile.contains(prefProperty)){
            isFirstLaunch = false;
        } else {
            prefFile.edit().putBoolean(prefProperty,true).commit();
            isFirstLaunch = true;
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, isFirstLaunch ? WelcomeActivity.class : MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }, SPLASH_DURATION);
    }
}
