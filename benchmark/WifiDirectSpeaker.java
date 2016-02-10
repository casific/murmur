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
package org.denovogroup.rangzen;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import org.apache.commons.lang3.time.StopWatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * This class handles interactions with the Android WifiP2pManager and the rest
 * of the OS Wifi Direct framework. It acts as a layer of abstraction between
 * the Rangzen application's notion of a "peer" and the actual network
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
public class WifiDirectSpeaker extends BroadcastReceiver {
  /** 
   * A default int value to be returned when getIntExtra fails to find
   * the requested key.
   */
  public static final int DEFAULT_EXTRA_INT = -1;

  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private WifiP2pManager mWifiP2pManager;

  /** Communication link to the Wifi Direct framework. */
  private Channel mWifiP2pChannel;

  /** Rangzen peer manager instance. */
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

  private StopWatch stopwatch;
  

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
  private synchronized boolean getSeeking() {
    return mSeeking;
  }

  /** Whether the rest of the app would like us to look for peers. */
  private boolean mSeekingDesired = false;

  /** The WifiP2pDevice representing THIS device. */
  private WifiP2pDevice mLocalDevice;

  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "WifiDirectSpeaker";

  /**
   * @param context A context, from which to access the Wifi Direct subsystem.
   * @param peerManager The app's PeerManager instance.
   * @param frameworkGetter The WifiDirectFrameworkGetter to be used to 
   * retrieve an instance of WifiP2pSpeaker.
   */
  public WifiDirectSpeaker(Context context, PeerManager peerManager, 
                           BluetoothSpeaker bluetoothSpeaker,
                           WifiDirectFrameworkGetter frameworkGetter) {
    super();

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
    // wrong if the context passed in is, say, the Rangzen Service's context.
    // This is a place where I don't understand Android very well where I
    // should.
    this.mLooper = context.getMainLooper();
    Log.i(TAG, "Initializing Wifi P2P Channel...");
    this.mWifiP2pChannel = mWifiP2pManager.initialize(context, mLooper, mChannelListener);
    Log.i(TAG, "Finished initializing Wifi P2P Channel.");
    this.mPeerManager = peerManager;

    // Register WifiDirectSpeaker to receive various events from the OS 
    // Wifi Direct subsystem. When these events arrive, the onReceive method
    // of this class is called, which dispatches them to other instance methods,
    // one per event.
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
    context.registerReceiver(this, intentFilter);

    stopwatch = new StopWatch();

    Log.d(TAG, "Finished creating WifiDirectSpeaker.");
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
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
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
      Log.wtf(TAG, "Received an event we weren't expecting: " + action);
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
      Log.d(TAG, "Wifi Direct enabled");
      // Wifi Direct mode is enabled
      // TODO(lerner): Do something since it's enabled?
    } else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
      Log.d(TAG, "Wifi Direct disabled");
      // Wifi Direct mode is disabled
      // TODO(lerner): Do something since it's disabled?
    } else if (state == DEFAULT_EXTRA_INT) {
      Log.e(TAG, "Wifi P2P state changed event handled, but the intent " +
                 "doesn't include an int to tell whether it's enabled or " +
                 "disabled!");
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
    // Temp used merely for readability (avoiding very long line/weird indent).
    Parcelable temp = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
    WifiP2pDeviceList peerDevices = (WifiP2pDeviceList) temp;

    boolean foundAnyRangzenPeers = false;

    for (WifiP2pDevice device : peerDevices.getDeviceList()) {
      if (device.deviceName != null && device.deviceName.startsWith(RangzenService.RSVP_PREFIX)) {
        String bluetoothAddress = device.deviceName.replace(RangzenService.RSVP_PREFIX, "");
        Log.i(TAG, "Found Rangzen peer " + device.deviceName + " with address " + bluetoothAddress);
        if (BluetoothSpeaker.looksLikeBluetoothAddress(bluetoothAddress) &&
            !BluetoothSpeaker.isReservedMACAddress(bluetoothAddress)) {
          BluetoothDevice bluetoothDevice = mBluetoothSpeaker.getDevice(bluetoothAddress);

          if (stopwatch.isStarted()) {
            stopwatch.stop();
          }
          float seconds = stopwatch.getNanoTime() / (float)(1000 * 1000 * 1000);
          float ms = stopwatch.getNanoTime() / (float)(1000 * 1000);
          Log.i(TAG, "Discovered a peer " + seconds + " seconds after discoverPeers call.");
          foundAnyRangzenPeers = true;

          if (bluetoothDevice != null) {
            Peer peer = getCanonicalPeerByDevice(bluetoothDevice);
            Log.d(TAG, "Adding peer " + peer);  
            mPeerManager.addPeer(peer);
          } else {
            Log.e(TAG, "Address " + bluetoothAddress + " got a null bluetooth device, not adding as peer.");
          }
        }
        else {
          Log.w(TAG, "Address from peer doesn't look like BT address or is reserved: " + bluetoothAddress);
        }

      }
    }

    // If we found any Rangzen peers, then we stop seeking a wait a while before
    // doing it again, in order to measure the time it takes to find peers.
    if (foundAnyRangzenPeers) {
      stopSeekingPeers();      
      touchLastSeekingTime();  // Touch the last seeking time to wait a while before seeking.
    }
    Log.v(TAG, "P2P peers changed");
  }

  /**
   * Called when the status of a Wifi Direct connection with a peer changes.
   * Updates the speaker's information on connection status.
   */
  private void onWifiP2pConnectionChanged(Context context, Intent intent) {
    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

    if (info.isConnected()) {
      Log.i(TAG, "Wifi P2P connected");
    } else {
      Log.i(TAG, "Wifi P2P disconnected");
    }
  }
  
  /**
   * This handles events that notify us that the WifiP2pDevice object
   * representing the local device has changed.
   */
  private void onWifiP2pThisDeviceChanged(Context context, Intent intent) {
    mLocalDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    Log.v(TAG, "Wifi P2P this device changed action received; local device is now: " + mLocalDevice);
  }

  /**
   * Receive events noting when Android has started or stopped looking
   * for Wifi P2P peers.
   */
  private void onWifiP2pDiscoveryChanged(Context context, Intent intent) {
    int discoveryStateCode = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
    if (discoveryStateCode == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
      Log.d(TAG, "Device is seeking Wifi Direct peers.");
      setSeeking(true);
    } else if (discoveryStateCode == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
      Log.d(TAG, "Device is NOT seeking Wifi Direct peers.");
      setSeeking(false);
    } else {
      Log.wtf(TAG, "Discovery changed event didn't have an EXTRA_DISCOVERY_STATE?!");
    }
  }


  /**
   * Respond to events raised by loss of communication with the Wifi Direct
   * framework in the Android OS.
   */
  private ChannelListener mChannelListener = new ChannelListener() {
    @Override
    public void onChannelDisconnected() {
      Log.w(TAG, "Communication with WifiP2pManager framework lost!");
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
      Log.d(TAG, "New wifi direct peer devices available" + peerDevices);
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
    return mPeerManager.getCanonicalPeer(new Peer(new BluetoothPeerNetwork(device)));
  }

  /**
   * The "main loop" for WifiDirectSpeaker. This method is called each time
   * the RangzenService runs its backgroundTasks() method, which happens
   * periodically over time.
   */
  public void tasks() {
    if (mSeekingDesired) {
      seekPeers();
    } else {
      stopSeekingPeers();
    }
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
   * @see LONG_AGO_THRESHOLD
   */
  private boolean lastSeekingWasLongAgo() {
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
    // DO NOT SUBMIT
    // Switched this to be &&
    if (!getSeeking() && lastSeekingWasLongAgo()) {
      setSeeking(true);
      touchLastSeekingTime();
      stopwatch.reset();
      stopwatch.start();
      mWifiP2pManager.discoverPeers(mWifiP2pChannel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Log.d(TAG, "Discovery initiated");
        }
      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Discovery failed: " + reasonCode);
        setSeeking(false);
        stopSeekingPeers();
      }
      });
    } else {
      Log.v(TAG, "Attempted to seek peers while already seeking, not doing it.");
    }

  }

  /**
   * Issue a request to the WifiP2pManager to stop discovering peers.
   * This is an internal method. To turn on/off peer discovery from higher
   * level application code, call setSeekingDesired(true/false).
   */
  private void stopSeekingPeers() {
    Log.i(TAG, "Stopping discovery...");
    mWifiP2pManager.stopPeerDiscovery(mWifiP2pChannel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "Discovery stopped successfully.");
        setSeeking(false);
      }
      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Failed to stop peer discovery? Reason: " + reasonCode);
      }
    });
  }

  /**
   * Set the user-friendly name broadcast by the Wifi Direct subsystem to 
   * the given value. This is used to broadcast some value (e.g. our Bluetooth
   * MAC address) to devices which can scan for us.
   */
  public void setWifiDirectUserFriendlyName(String name) {
    if (mWifiP2pManager == null || mWifiP2pChannel == null) {
      return;
    }
    try {
      // The WifiP2pManager.setDeviceName() method is hidden but accessible through
      // reflection. The only place I've ever seen it used normally is in the settings
      // panel of Android, under Wifi -> ... -> Wifi Direct to allow the user to 
      // change the Wifi Direct name of their device.
      Method method = mWifiP2pManager.getClass().getMethod("setDeviceName", Channel.class, String.class, ActionListener.class);
      method.invoke(mWifiP2pManager, mWifiP2pChannel, name, null);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      Log.e(TAG, "Reflection found no such method as setDeviceName");
    } catch (IllegalAccessException e) {
      Log.e(TAG, "Illegal access exception: " + e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Illegal argument exception: " + e);
    } catch (InvocationTargetException e) {
      Log.e(TAG, "Invocation target exception: " + e);
    }
  }
}
