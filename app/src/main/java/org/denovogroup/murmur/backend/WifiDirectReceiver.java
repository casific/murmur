package org.denovogroup.murmur.backend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.log4j.Logger;

/**
 * Created by Liran on 1/5/2016.
 */
public class WifiDirectReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiDirectReceiver";
    private static final Logger log = Logger.getLogger(TAG);

    public WifiDirectReceiver() {
        //default constructor so it may be declared statically in the manifest for better chance of intercepting
        //the transmiation
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("peerDebug","WifiDirect boardcast received, passing to speaker");
        log.debug("WifiDirect boardcast received, passing to speaker");
        WifiDirectSpeaker.getInstance().onReceive(context, intent);
    }
}
