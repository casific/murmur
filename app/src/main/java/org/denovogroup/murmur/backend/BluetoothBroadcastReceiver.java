/*
 * Copyright (c) 2014, De Novo Group
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
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.R;


/**
 * Receives events about Bluetooth.
 */
public class BluetoothBroadcastReceiver extends BroadcastReceiver {

  /** Included in Android log messages. */
  public static final String TAG = "BtBroadcastReceiver";

  /** A default value to be returned when getting extras. */
  private static final int DEFAULT_INT = -500;

  private static final Logger log = Logger.getLogger(TAG);

  /**
   * @param context A context, from which to access the Bluetooth subsystem.
   */
  public BluetoothBroadcastReceiver(Context context) {
    super();

    // Register to receive events when the Bluetooth status changes
    // between ON/OFF/TURNING_ON/TURNING_OFF.
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
    intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
    intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    intentFilter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
    intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
    intentFilter.addAction(BluetoothDevice.ACTION_UUID);
      log.info( "Registering Bluetooth receiver");
    context.registerReceiver(this, intentFilter);
  }

  /**
   * Called when broadcasts matching this receiver's intent filter are raised,
   * which include a number of actions related to Bluetooth.
   *
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
   * android.content.Intent)
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
      onBluetoothActionStateChanged(context, intent);
    } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
      onBluetoothConnected(context, intent);
    } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
      onBluetoothDisconnected(context, intent);
    } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
      onBluetoothDisconnectRequested(context, intent);
    } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
      onBluetoothBondStateChange(context, intent);
    } else if (BluetoothDevice.ACTION_CLASS_CHANGED.equals(action)) {
      onBluetoothRemoteDeviceClassChange(context, intent);
    } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
      onBluetoothPeerFound(context, intent);
    } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
      onBluetoothRemoteNameChange(context, intent);
    } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
      onBluetoothBroadcastRemoteUUID(context, intent);
    } else {
      // TODO(lerner): This shouldn't happen, exception?
      log.debug( "Received an event we weren't expecting: " + action);
    }
  }

  /**
   * Handler for events broadcast when the Bluetooth radio is turning on/off,
   * or has successfully turned on or off.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothActionStateChanged(Context context, Intent intent) {
    int currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, DEFAULT_INT);
    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, DEFAULT_INT);
    String currentStateString = getBTStateStringFromCode(currentState);
    String previousStateString = getBTStateStringFromCode(previousState);
      log.debug(String.format("BT state change. %s (%d) to %s (%d) ", previousStateString,
            previousState,
            currentStateString,
            currentState));

      switch(currentState){
          case BluetoothAdapter.STATE_TURNING_OFF:
          case BluetoothAdapter.STATE_OFF:
              showNoBluetoothNotification(context);
              break;
          case BluetoothAdapter.STATE_TURNING_ON:
          case BluetoothAdapter.STATE_ON:
              dismissNoBluetoothNotification(context);
              break;
      }

  }

    /** create and display a dialog prompting the user about the enabled
     * state of the bluetooth service.
     */
    public void showNoBluetoothNotification(Context context){

        if(MurmurService.CONSOLIDATE_ERRORS) {
            ServiceWatchDog.getInstance().notifyHardwareStateChanged();
            return;
        }


        if(context == null) return;

        int notificationId = R.string.notification_no_bluetooth_message;

        Intent notificationIntent = new Intent(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // create large icon
        Resources res = context.getResources();
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

        Intent onIntent = new Intent();
        onIntent.setAction(MurmurService.ACTION_ONBT);
        PendingIntent pendingOnIntent = PendingIntent.getBroadcast(context, -1, onIntent, 0);

        Intent offIntent = new Intent();
        offIntent.setAction(MurmurService.ACTION_TURNOFF);
        PendingIntent pendingOffIntent = PendingIntent.getBroadcast(context, -1, offIntent, 0);

        Notification notification = new Notification.Builder(context).setContentTitle(context.getText(R.string.notification_no_bluetooth_title))
                .setContentText(context.getText(R.string.notification_no_bluetooth_message))
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_error)
                .addAction(R.drawable.blank_square, context.getString(R.string.error_notification_action_turnon_bt), pendingOnIntent)
                .addAction(R.drawable.blank_square, context.getString(R.string.error_notification_action_off_service), pendingOffIntent)
                .build();
        mNotificationManager.notify(notificationId, notification);
    }

    /** dismiss the no bluetooth notification if showing
     */
    public void dismissNoBluetoothNotification(Context context){

        if(MurmurService.CONSOLIDATE_ERRORS) {
            ServiceWatchDog.getInstance().notifyHardwareStateChanged();
            return;
        }

        int notificationId = R.string.notification_no_bluetooth_message;

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }

  /**
   * Called to handle events raised when the Bluetooths system broadcasts the
   * UUID of a device fetched over SDP. Note that this is probably a device
   * we're not interested in, since we're not using SDP to discover services.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothBroadcastRemoteUUID(Context context, Intent intent) {
    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
    for (Parcelable uuid : uuids) {
      // Log.d(TAG, String.format("Remote device (%s); UUID: %s ", device, ((ParcelUuid) uuid).toString()));
    }
      log.info( "Bluetooth broadcast: remoteUUID");
  }

  /**
   * Called to handle events raised when the name of another Bluetooth
   * device we're interacting with changes.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothRemoteNameChange(Context context, Intent intent) {
    log.debug( "Remote device's name changed.");
  }

  /**
   * Called to handle events raised when Bluetooth scanning finds a peer.
   * Note that we don't use the peer seeking system of Bluetooth, so this
   * probably isn't a peer we're interested in!
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothPeerFound(Context context, Intent intent) {
    // TODO(lerner): murmur may want to stop doing its thing if other apps
    // are causing seeking?
    log.debug( "Peer found broadcast received.");
  }

  /**
   * Called to handle events raised when the device class of another Bluetooth
   * device we're interacting with changes.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothRemoteDeviceClassChange(Context context, Intent intent) {
    log.debug("Remote device's class changed.");
  }

  /**
   * Called to handle events raised indicating that our bond state with
   * another Bluetooth device (i.e. whether we're paired) has changed.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothBondStateChange(Context context, Intent intent) {
    // TODO(lerner): murmur may want to stop doing its thing if other apps
    // are causing pairing and such?
    log.debug("Bond state change notification received.");
  }

  /**
   * This method is called when a low level event broadcasts that a request
   * to disconnect from the currently connected Bluetooth device has been
   * made and we will soon be disconnected.
   *
   * @param context The context received from the broadcast of this event.
   * @param intent The intent associated with the broadcast of this event.
   */
  private void onBluetoothDisconnectRequested(Context context, Intent intent) {
    BluetoothDevice device;
    device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    log.info( "Bluetooth broadcast: disconnected! (from " + device + ")");
  }

  /**
   * This method is called when an event is broadcast indicating that we are 
   * now disconnected to another Bluetooth device.
   */
  private void onBluetoothDisconnected(Context context, Intent intent) {
    BluetoothDevice device;
    device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    log.info( "Bluetooth broadcast: disconnected! (from " + device + ")");
  }

  /**
   * This method is called when an event is broadcast indicating that we are 
   * now connected to another Bluetooth device.
   */
  private void onBluetoothConnected(Context context, Intent intent) {
    BluetoothDevice device;
    device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      log.info( "Bluetooth broadcast: connected! (to " + device + ")");
  }

  /**
   * Utility method that converts int constants indicating Bluetooth system
   * state into Strings based on their meanings.
   */
  private String getBTStateStringFromCode(int code) {
    if (code == BluetoothAdapter.STATE_OFF) { 
      return "STATE_OFF";
    } else if (code == BluetoothAdapter.STATE_ON) {
      return "STATE_ON";
    } else if (code == BluetoothAdapter.STATE_TURNING_ON) {
      return "STATE_TURNING_ON";
    } else if (code == BluetoothAdapter.STATE_TURNING_OFF) {
      return "STATE_TURNING_OFF";
    } else {
      return "INVALID";
    }
  }
}
