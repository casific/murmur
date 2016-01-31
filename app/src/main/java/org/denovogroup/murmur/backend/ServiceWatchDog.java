/*
* Copyright (c) 2016, De Novo Group
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from this
* software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRES S OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package org.denovogroup.murmur.backend;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.R;
import org.denovogroup.murmur.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 1/22/2016.
 *
 * A class used to monitor the health state of the MurmurService and restart it if it cannot
 * function anymore
 */
public class ServiceWatchDog {

    Timer timer;

    private WeakReference<MurmurService> serviceWeakReference;

    private Date lastExchange;

    private final static String TAG = "ServiceWatchDog";
    private static final Logger log = Logger.getLogger(TAG);

    private static ServiceWatchDog instance;

    private static final int WAIT_BEFORE_RESTART = 5000;
    private static final long SUSSPECIOUS_TIME_BETWEEN_EXCHANGES = TimeUnit.MINUTES.toMillis(20);

    public static ServiceWatchDog getInstance(){
        if(instance == null){
            instance = new ServiceWatchDog();
        }
        return instance;
    }

    private ServiceWatchDog() {
    }

    public void init(MurmurService service){
        serviceWeakReference = new WeakReference<>(service);
    }

    public void notifyLastExchange(){

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        lastExchange = new Date();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    restartService();
                } catch (Exception e){}
            }
        }, SUSSPECIOUS_TIME_BETWEEN_EXCHANGES);
    }

    /** notify big error to the watchdog so it may help recover */
    public void notifyError(Exception e){
        restartService();
    }


    private void restartService(){
        log.debug("attempting recovery");

        if(serviceWeakReference == null){
            log.error("Watchdog service reference not been initialized");
            return;
        }

        MurmurService service = serviceWeakReference.get();

        if(service == null){
            log.error("Watchdog cannot recover service, service reference is null");
            return;
        }

        if(!service.getSharedPreferences(MainActivity.PREF_FILE, Context.MODE_PRIVATE).getBoolean(MainActivity.IS_APP_ENABLED, true)){
            service.stopSelf();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);

        Intent restartIntent = new Intent(service.getApplicationContext(), MurmurService.class);
        PendingIntent pendingRestart = PendingIntent.getService(service.getApplicationContext(), -1, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + WAIT_BEFORE_RESTART, pendingRestart);

        log.debug("restarting service");
        service.stopSelf();
    }

    /** notify the WatchDog that controlled service stop had occurred to prevent timed checkups
     * from happening
     */
    public void notifyServiceDestroy(){
        if(timer != null) timer.cancel();
        timer = null;

        notifyHardwareStateChanged();

        serviceWeakReference = null;
    }

    public void notifyHardwareStateChanged(){

        if(serviceWeakReference == null || !MurmurService.CONSOLIDATE_ERRORS) return;

        MurmurService service = serviceWeakReference.get();

        if(service == null) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        boolean bluetoothEnabled = (adapter != null && adapter.isEnabled());

        WifiManager manager = (WifiManager) service.getSystemService(Context.WIFI_SERVICE);

        boolean wifiEnabled = (manager != null && manager.isWifiEnabled());


        int notificationId = R.string.notification_connection_error_title;

        //Intent notificationIntent = new Intent(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));;
        //PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        boolean serviceIsOn = service.getSharedPreferences(MainActivity.PREF_FILE,Context.MODE_PRIVATE).getBoolean(MainActivity.IS_APP_ENABLED, true);

        if((wifiEnabled && bluetoothEnabled) || !serviceIsOn){
            mNotificationManager.cancel(notificationId);
            return;
        }

        // create large icon
        Resources res = service.getResources();
        BitmapDrawable largeIconDrawable;
        if(Build.VERSION.SDK_INT >= 21){
            largeIconDrawable = (BitmapDrawable) res.getDrawable(R.mipmap.ic_launcher, null);
        } else {
            largeIconDrawable = (BitmapDrawable) res.getDrawable(R.mipmap.ic_launcher);
        }
        Bitmap largeIcon = largeIconDrawable.getBitmap();

        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);

        /*Intent onIntent = new Intent();
        onIntent.setAction(MurmurService.ACTION_ONBT);
        PendingIntent pendingOnIntent = PendingIntent.getBroadcast(service, -1, onIntent, 0);*/

        Intent offIntent = new Intent();
        offIntent.setAction(MurmurService.ACTION_TURNOFF);
        PendingIntent pendingOffIntent = PendingIntent.getBroadcast(service, -1, offIntent, 0);

        Notification notification = new Notification.Builder(service).setContentTitle(service.getText(R.string.notification_connection_error_title))
                .setContentText(service.getText(R.string.notification_connection_error_message))
                .setLargeIcon(largeIcon)
                //.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_error)
                //.addAction(R.drawable.blank_square, context.getString(R.string.error_notification_action_turnon_bt), pendingOnIntent)
                .addAction(R.drawable.blank_square, service.getString(R.string.error_notification_action_off_service), pendingOffIntent)
                .build();
        mNotificationManager.notify(notificationId, notification);
    }
}
