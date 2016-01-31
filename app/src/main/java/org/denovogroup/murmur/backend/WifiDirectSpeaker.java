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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.R;
import org.denovogroup.murmur.ui.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * This class handles interactions with the Android WifiP2pManager and the rest
 * of the OS Wifi Direct framework. It acts as a layer of abstraction between
 * the Murmur application's notion of a "peer" and the actual network
 * communication necessary to manage and communicate with Wifi Direct peers. It
 * searches for peers, manages connections to those peers, and sends and
 * receives packets from those peers. 
 *
 * Currently connected peers "ping" each other with short UDP packets
 * (these are not actual ICMP pings).
 *
 * TODO(lerner): Implement the ability to send messages that aren't just
 * pings to other devices, from higher levels of the app.
 */
public class WifiDirectSpeaker {
  /** 
   * A default int value to be returned when getIntExtra fails to find
   * the requested key.
   */
  public static final int DEFAULT_EXTRA_INT = -1;

  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private WifiP2pManager mWifiP2pManager;

  /** Communication link to the Wifi Direct framework. */
  private Channel mWifiP2pChannel;

  /** Murmur peer manager instance. */
  private PeerManager mPeerManager;

  /** BluetoothSpeaker used to get BluetoothDevices for peers. */
  private BluetoothSpeaker mBluetoothSpeaker;

  /** Context to retrieve a WifiP2pManager from the Wifi Direct subsystem. */
  private Context mContext;

  /** 
   * The looper that runs the onReceive() loop to handle Wifi Direct framework
   * events.
   */
  private Looper mLooper;

  /** 
   * Flag to remember whether we're seeking peers. This flag is used to know
   * whether we should start seeking peers again in seekPeers(). It reflects
   * whether a request has been made, to scan for peers, regardless of whether
   * that request has succeeded yet.
   *
   * Failed requests and stopping peer seeking set this to false.
   */
  private boolean mSeeking = false;

  /** Synchronized setter for mSeeking. */
  private synchronized void setSeeking(boolean seeking) {
    this.mSeeking = seeking;
  }

  /** Synchronized getter for mSeeking. */
  public synchronized boolean getSeeking() {
    return mSeeking;
  }

  /** Whether the rest of the app would like us to look for peers. */
  private boolean mSeekingDesired = false;

  /** The WifiP2pDevice representing THIS device. */
  private WifiP2pDevice mLocalDevice;

  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "WifiDirectSpeaker";
    private static final Logger log = Logger.getLogger(TAG);

    private static WifiDirectSpeaker instance;
    private boolean initialized = false;

    public WifiDirectSpeaker(){}

    public static synchronized WifiDirectSpeaker getInstance(){
        if(instance == null){
            instance = new WifiDirectSpeaker();
            instance.initialized = false;
        }
        return instance;
    }

  /**
   * @param context A context, from which to access the Wifi Direct subsystem.
   * @param peerManager The app's PeerManager instance.
   * @param frameworkGetter The WifiDirectFrameworkGetter to be used to 
   * retrieve an instance of WifiP2pSpeaker.
   */
  public void init(Context context, PeerManager peerManager,
                           BluetoothSpeaker bluetoothSpeaker,
                           WifiDirectFrameworkGetter frameworkGetter) {
    //super();

    this.mContext = context;
    this.mBluetoothSpeaker = bluetoothSpeaker;

    // Here we add a layer of apparently unnecessary indirection in order to
    // allow testing frameworks to provide a framewok getter that returns a 
    // mock version of WifiP2pManager.
    this.mWifiP2pManager = frameworkGetter.getWifiP2pManagerInstance(context);
      
    // TODO(lerner): Create our own looper that doesn't run in the main thread.
    // I don't understand loopers very well, but I'm pretty sure we want to
    // create a separate looper here for initializing the P2P framework, since
    // I believe that the main looper runs in the app's thread. That might be
    // wrong if the context passed in is, say, the Murmur Service's context.
    // This is a place where I don't understand Android very well where I
    // should.
    this.mLooper = context.getMainLooper();
    log.info( "Initializing Wifi P2P Channel...");
    this.mWifiP2pChannel = mWifiP2pManager.initialize(context, mLooper, mChannelListener);
    log.info( "Finished initializing Wifi P2P Channel.");
    this.mPeerManager = peerManager;

      // Register WifiDirectSpeaker to receive various events from the OS
    // Wifi Direct subsystem. When these events arrive, the onReceive method
    // of this class is called, which dispatches them to other instance methods,
    // one per event.
    /*IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
    context.registerReceiver(this, intentFilter);*/

      WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
      if (!wifi.isWifiEnabled()){
          showNoWifiNotification(context);
      }

      mSeeking = false;
    log.debug( "Finished creating WifiDirectSpeaker.");
      initialized = true;
  }

  /**
   * Handle incoming messages. This class handles broadcasts sent by WifiP2pManager.
   * We handle them by calling other methods in the class as appropriate to handle
   * each type of event. One specific method is called for each type of event
   * and handles all the logic related to that event.
   *
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
   * android.content.Intent)
   */
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

      if(!initialized){
          Log.d("peerDebug", "received action:"+action+", but speaker not yet initialized, ignoring transmission");
          log.warn("received action:"+action+", but speaker not yet initialized, ignoring transmission");
          return;
      }

    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
      onWifiP2pStateChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
      onWifiP2pPeersChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
      onWifiP2pConnectionChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
      onWifiP2pThisDeviceChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
      onWifiP2pDiscoveryChanged(context, intent);
    } else {
      // TODO(lerner): This shouldn't happen, exception?
      log.error( "Received an event we weren't expecting: " + action);
      Log.d("peerDebug", "Received an event we weren't expecting: " + action);
    }
  }


  /**
   * Receives events indicating whether Wifi Direct is enabled or disabled.
   */
  private void onWifiP2pStateChanged(Context context, Intent intent) {
    // Since int is a simple type, we have to provide a default value
    // in case the requested key isn't contained as an extra.
    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 
                                   DEFAULT_EXTRA_INT);
    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
      log.debug( "Wifi Direct enabled");
        Log.d("peerDebug", "Wifi Direct enabled");
      // Wifi Direct mode is enabled
        dismissNoWifiNotification();
    } else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
        showNoWifiNotification(context);
      log.debug("Wifi Direct disabled");
        Log.d("peerDebug", "Wifi Direct disabled");
      // Wifi Direct mode is disabled
    } else if (state == DEFAULT_EXTRA_INT) {
      log.error( "Wifi P2P state changed event handled, but the intent " +
                 "doesn't include an int to tell whether it's enabled or " +
                 "disabled!");
        Log.d("peerDebug",  "Wifi P2P state changed event handled, but the intent " +
                "doesn't include an int to tell whether it's enabled or " +
                "disabled!");
    } else {
        log.error("Wifi P2P state changed to an unknown state:"+state);
        Log.d("peerDebug","Wifi P2P state changed to an unknown state:"+state);
    }
  }

  /**
   * Called when the WifiP2pManager notifies the Speaker that new peers are
   * available. This method extracts the actual list of peers from the 
   * intent, creates or retrieves canonical Peer objects for each, and 
   * then adds those peers to the PeerManager.
   *
   * @param context Context passed to onReceive, forwarded to this method.
   * @param intent An intent containing the list of new Wifi Direct devices as
   * an extra.
   */
  private void onWifiP2pPeersChanged(Context context, Intent intent) {
    log.info("WifiDirectSpeaker called onWifiP2pPeersChanged");
      Log.d("peerDebug", "WifiDirectSpeaker called onWifiP2pPeersChanged");
    // Temp used merely for readability (avoiding very long line/weird indent).
    Parcelable temp = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
    WifiP2pDeviceList peerDevices = (WifiP2pDeviceList) temp;

    for (WifiP2pDevice device : peerDevices.getDeviceList()) {
      if (device.deviceName != null && device.deviceName.startsWith(MurmurService.RSVP_PREFIX)) {
        String bluetoothAddress = device.deviceName.replace(MurmurService.RSVP_PREFIX, "");
        log.info( "Found Murmur peer " + device.deviceName + " with address " + bluetoothAddress);
          Log.d("peerDebug",  "Found Murmur peer " + device.deviceName + " with address " + bluetoothAddress);
        if (BluetoothSpeaker.looksLikeBluetoothAddress(bluetoothAddress) &&
            !BluetoothSpeaker.isReservedMACAddress(bluetoothAddress)) {
          BluetoothDevice bluetoothDevice = mBluetoothSpeaker.getDevice(bluetoothAddress);
          if (bluetoothDevice != null) {
            Peer peer = getCanonicalPeerByDevice(bluetoothDevice);
            log.debug( "Adding peer " + peer);
              Log.d("peerDebug", "Adding peer " + peer);
            mPeerManager.addPeer(peer);
          } else {
            log.error( "Address " + bluetoothAddress + " got a null bluetooth device, not adding as peer.");
              Log.d("peerDebug",  "Address " + bluetoothAddress + " got a null bluetooth device, not adding as peer.");
          }
        }
        else {
          log.warn( "Address from peer doesn't look like BT address or is reserved: " + bluetoothAddress);
            Log.d("peerDebug", "Address from peer doesn't look like BT address or is reserved: " + bluetoothAddress);
        }
      } else {
          if(device != null){
              log.info( "Found device? "+device);
              Log.d("peerDebug", "Found device? "+device);
          } else {
              log.info( "Got null device");
              Log.d("peerDebug", "Got null device");
          }
      }
    }
    log.info( "P2P peers changed "+peerDevices.getDeviceList().size());
      Log.d("peerDebug", "P2P peers changed "+peerDevices.getDeviceList().size());

      ExchangeHistoryTracker.getInstance().cleanHistory(mPeerManager.getPeers());
  }

  /**
   * Called when the status of a Wifi Direct connection with a peer changes.
   * Updates the speaker's information on connection status.
   */
  private void onWifiP2pConnectionChanged(Context context, Intent intent) {
    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

    if (info.isConnected()) {
      log.info( "Wifi P2P connected");
        Log.d("peerDebug","Wifi P2P connected");
    } else {
      log.info( "Wifi P2P disconnected");
        Log.d("peerDebug", "Wifi P2P disconnected");
    }
  }
  
  /**
   * This handles events that notify us that the WifiP2pDevice object
   * representing the local device has changed.
   */
  private void onWifiP2pThisDeviceChanged(Context context, Intent intent) {
    mLocalDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    log.info( "Wifi P2P this device changed action received; local device is now: " + mLocalDevice);
      Log.d("peerDebug", "Wifi P2P this device changed action received; local device is now: " + mLocalDevice);
  }

  /**
   * Receive events noting when Android has started or stopped looking
   * for Wifi P2P peers.
   */
  private void onWifiP2pDiscoveryChanged(Context context, Intent intent) {
    int discoveryStateCode = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
    if (discoveryStateCode == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
      log.debug( "Device is seeking Wifi Direct peers.");
        Log.d("peerDebug", "Device is seeking Wifi Direct peers.");
      setSeeking(true);
    } else if (discoveryStateCode == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
      log.debug( "Device is NOT seeking Wifi Direct peers.");
        Log.d("peerDebug", "Device is NOT seeking Wifi Direct peers.");
      setSeeking(false);
    } else {
      log.error( "Discovery changed event didn't have an EXTRA_DISCOVERY_STATE?!");
        Log.d("peerDebug", "Discovery changed event didn't have an EXTRA_DISCOVERY_STATE?!");
    }
  }


  /**
   * Respond to events raised by loss of communication with the Wifi Direct
   * framework in the Android OS.
   */
  private ChannelListener mChannelListener = new ChannelListener() {
    @Override
    public void onChannelDisconnected() {
      log.warn( "Communication with WifiP2pManager framework lost!");
        Log.d("peerDebug", "Communication with WifiP2pManager framework lost!");
      // TODO(lerner): Respond to this fact with some ameliorating action, probably
      // ceasing to take other actions that won't work without the framework.
    }
  };

  /**
   * Receives requested peer list from the OS Wifi Direct framework and
   * forwards those peers to the PeerManager.
   */
  private PeerListListener mPeerListListener = new PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerDevices) {
      log.debug( "New wifi direct peer devices available" + peerDevices);
        Log.d("peerDebug", "New wifi direct peer devices available" + peerDevices);
      // Actual handling of these peers is performed directly when the
      // peers changed event is raised, rather than indirectly here after
      // a request an a callback.
    }
  };

  /**
   * Determine the canonical Peer for a given Bluetooth device. The canonical Peer for
   * a device is the instance of Peer located in PeerManager's peer list, if any.
   *
   * @param device The Bluetooth device we want to learn the Peer for.
   * @return The canonical Peer instance for the peer reachable at the given 
   * device, or null if no such peer exists.
   */
  private Peer getCanonicalPeerByDevice(BluetoothDevice device) {
    return mPeerManager.getCanonicalPeer(new Peer(new BluetoothPeerNetwork(device), device.getAddress()));
  }

  /**
   * The "main loop" for WifiDirectSpeaker. This method is called each time
   * the MurmurService runs its backgroundTasks() method, which happens
   * periodically over time. returning true if proper wifi handling is possible,
   * or false otherwise
   */
  public boolean tasks() {
      log.info("Starting WifiDirectSpeaker tasks (mSeekingDesired:" + mSeekingDesired + ")");

      WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    if (mSeekingDesired && wifiManager.isWifiEnabled()) {
      seekPeers();
    } else {
      stopSeekingPeers();
      return false;
    }
      log.info("finished WifiDirectSpeaker");
      return true;
  }

  /** 
   * Call this method to start or stop peer discovery from outside the speaker.
   *
   * @param seekingDesired True if peer discovery is permitted.
   */
  public void setmSeekingDesired(boolean seekingDesired) {
    this.mSeekingDesired = seekingDesired;
  }

  /**
   * Set the time we last asked for a Wifi Direct Scan to now.
   */
  private void touchLastSeekingTime() {
    lastSeekingTime = new Date();
  }

  /** Time in milliseconds between requests to discoverPeers(). */
  private long LONG_AGO_THRESHOLD = 1 * 60 * 1000; // 1 minute.

  /** Time at which we last called discoverPeers(). */
  private Date lastSeekingTime = null; 

  /**
   * Check whether it's been LONG_AGO_THRESHOLD time since the last time we
   * called discoverPeers().
   *
   */
  public synchronized boolean lastSeekingWasLongAgo() {
    if (lastSeekingTime == null) {
      return true;
    }
    return (new Date()).getTime() - lastSeekingTime.getTime() > LONG_AGO_THRESHOLD;
  }

  /**
   * Issue a request to the WifiP2pManager to start discovering peers.
   * This is an internal method. To turn on/off peer discovery from higher
   * level application code, call setSeekingDesired(true/false).
   */
  private void seekPeers() {
      Log.d("peerDebug", "seekPeer (" + (!getSeeking()) + "," + lastSeekingWasLongAgo() + ")");
    if (!getSeeking() || lastSeekingWasLongAgo()) {
        log.info("seeking peers");
        Log.d("peerDebug", "request seekPeer");
      setSeeking(true);
      touchLastSeekingTime();
      mWifiP2pManager.stopPeerDiscovery(mWifiP2pChannel, new ActionListener() {
          @Override
          public void onSuccess() {
              mWifiP2pManager.discoverPeers(mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                  @Override
                  public void onSuccess() {
                      log.debug("Discovery initiated");
                  }

                  @Override
                  public void onFailure(int reasonCode) {
                      log.debug("Discovery failed: " + reasonCode);
                      setSeeking(false);
                      stopSeekingPeers();
                  }
              });
          }

          @Override
          public void onFailure(int reason) {
            onSuccess();
          }
      });
    } else {
      log.info("Attempted to seek peers while already seeking, not doing it. (seeking:"+getSeeking()+", seeking was long time ago:"+lastSeekingWasLongAgo()+")");
    }

  }

  /**
   * Issue a request to the WifiP2pManager to stop discovering peers.
   * This is an internal method. To turn on/off peer discovery from higher
   * level application code, call setSeekingDesired(true/false).
   */
  private void stopSeekingPeers() {
      if(getSeeking()) {
          log.info("Stopped seeking peers");
          Log.d("peerDebug","Stopped seeking peers");
          mWifiP2pManager.stopPeerDiscovery(mWifiP2pChannel, new WifiP2pManager.ActionListener() {
              @Override
              public void onSuccess() {
                  log.debug("Discovery stopped successfully.");
                  setSeeking(false);
              }

              @Override
              public void onFailure(int reasonCode) {
                  log.debug("Failed to stop peer discovery? Reason: " + reasonCode);
              }
          });
      }
  }

  /**
   * Set the user-friendly name broadcast by the Wifi Direct subsystem to 
   * the given value. This is used to broadcast some value (e.g. our Bluetooth
   * MAC address) to devices which can scan for us.
   */
  public void setWifiDirectUserFriendlyName(String name) {
    if (mWifiP2pManager == null || mWifiP2pChannel == null) {
        log.debug( "setWifiDirectUserFriendlyName failed: WifiP2pManager or WifiP2pChannel are null");
      return;
    }
    try {
      // The WifiP2pManager.setDeviceName() method is hidden but accessible through
      // reflection. The only place I've ever seen it used normally is in the settings
      // panel of Android, under Wifi -> ... -> Wifi Direct to allow the user to 
      // change the Wifi Direct name of their device.
      Method method = mWifiP2pManager.getClass().getMethod("setDeviceName", Channel.class, String.class, ActionListener.class);
      method.invoke(mWifiP2pManager, mWifiP2pChannel, name, null);
      log.info( "Device name changed to:"+name);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      log.error( "Reflection found no such method as setDeviceName");
    } catch (IllegalAccessException e) {
      log.error( "Illegal access exception: " + e);
    } catch (IllegalArgumentException e) {
      log.error( "Illegal argument exception: " + e);
    } catch (InvocationTargetException e) {
      log.error( "Invocation target exception: " + e);
    }
  }

    /** create and display a notification prompting the user to the enable
     * state of the wifi service.
     */
    private void showNoWifiNotification(Context context){

        if(MurmurService.CONSOLIDATE_ERRORS) {
            ServiceWatchDog.getInstance().notifyHardwareStateChanged();
            return;
        }

        if(mContext == null) return;

        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_FILE, Context.MODE_PRIVATE);
        if(!pref.getBoolean(MainActivity.IS_APP_ENABLED, true)){
            return;
        }


        int notificationId = R.string.notification_no_wifi_message;

        Intent notificationIntent = new Intent(new Intent(Settings.ACTION_WIFI_SETTINGS));;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

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
        onIntent.setAction(MurmurService.ACTION_ONWIFI);
        PendingIntent pendingOnIntent = PendingIntent.getBroadcast(context, -1, onIntent, 0);

        Intent offIntent = new Intent();
        offIntent.setAction(MurmurService.ACTION_TURNOFF);
        PendingIntent pendingOffIntent = PendingIntent.getBroadcast(context, -1, offIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(mContext).setContentTitle(mContext.getText(R.string.notification_no_wifi_title))
                .setContentText(mContext.getText(R.string.notification_no_wifi_message))
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_error)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.blank_square, context.getString(R.string.error_notification_action_onwifi), pendingOnIntent)
                .addAction(R.drawable.blank_square, context.getString(R.string.error_notification_action_off_service), pendingOffIntent)
                .build();
        mNotificationManager.notify(notificationId, notification);
    }

    /** dismiss the no wifi notification if showing
     */
    public void dismissNoWifiNotification(){

        if(MurmurService.CONSOLIDATE_ERRORS) {
            ServiceWatchDog.getInstance().notifyHardwareStateChanged();
            return;
        }

        if(mContext == null) return;

        int notificationId = R.string.notification_no_wifi_message;

        NotificationManager mNotificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }
}
