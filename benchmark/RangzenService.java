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

import au.com.bytecode.opencsv.CSVWriter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.System;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * Core service of the Rangzen app. Started at startup, remains alive
 * indefinitely to perform the background tasks of Rangzen.
 */
public class RangzenService extends Service {
    /** The running instance of RangzenService. */
    protected static RangzenService sRangzenServiceInstance;

    /** For app-local broadcast and broadcast reception. */
    private LocalBroadcastManager mLocalBroadcastManager;

    /** Executes the background thread periodically. */
    private ScheduledExecutorService mScheduleTaskExecutor;

    /** Cancellable scheduling of backgroundTasks. */
    private ScheduledFuture mBackgroundExecution;

    /** Handle to app's PeerManager. */
    private PeerManager mPeerManager;

    /** The time at which this instance of the service was started. */
    private Date mStartTime;

    /** Random number generator for picking random peers. */
    private Random mRandom = new Random();

    /** The number of times that backgroundTasks() has been called. */
    private int mBackgroundTaskRunCount = 0;

    /** Handle to Rangzen key-value storage provider. */
    private StorageBase mStore;

    /** Storage for friends. */
    private FriendStore mFriendStore;

    /** Wifi Direct Speaker used for Wifi Direct name based RSVP. */
    private WifiDirectSpeaker mWifiDirectSpeaker;

    /** Whether we're attempting a connection to another device over BT. */
    private boolean connecting = false;

    /** The BluetoothSpeaker for the app. */
    private static BluetoothSpeaker mBluetoothSpeaker;

    /** Message store. */
    private MessageStore mMessageStore; 
    /** Ongoing exchange. */
    private Exchange mExchange;

    /** Socket over which the ongoing exchange is taking place. */
    private BluetoothSocket mSocket;

    /** When announcing our address over Wifi Direct name, prefix this string to our MAC. */
    public final static String RSVP_PREFIX = "RANGZEN-";

    /** Key into storage to store the last time we had an exchange. */
    private static final String LAST_EXCHANGE_TIME_KEY = 
                            "org.denovogroup.rangzen.LAST_EXCHANGE_TIME_KEY";

    /** Time to wait between exchanges, in milliseconds. */
    private static final int TIME_BETWEEN_EXCHANGES_MILLIS = 10 * 1000;

    /** For recording the RTT of BT communications. */
    public long exchangeStartTimeMillis;

    /** Directory under <EXTERNAL DOCUMENTS> where benchmark data is stored. */
    private static final String BENCHMARK_DIR = "RangzenBenchmarks";

    /** Android Log Tag. */
    private final static String TAG = "RangzenService";

    /**
     * Called whenever the service is requested to start. If the service is
     * already running, this does /not/ create a new instance of the service.
     * Rather, onStartCommand is called again on the existing instance.
     * 
     * @param intent
     *            The intent passed to startService to start this service.
     * @param flags
     *            Flags about the request to start the service.
     * @param startid
     *            A unique integer representing this request to start.
     * @see android.app.Service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.i(TAG, "RangzenService onStartCommand.");

        // Returning START_STICKY causes Android to leave the service running
        // even when the foreground activity is closed.
        return START_STICKY;
    }

    /**
     * Called the first time the service is started.
     * 
     * @see android.app.Service
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "RangzenService created.");

        sRangzenServiceInstance = this;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mPeerManager = PeerManager.getInstance(this);
        mBluetoothSpeaker = new BluetoothSpeaker(this, mPeerManager);
        mPeerManager.setBluetoothSpeaker(mBluetoothSpeaker);

        mStartTime = new Date();

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        mFriendStore = new FriendStore(this, StorageBase.ENCRYPTION_DEFAULT);

        // Used for a live test.
        // TODO(lerner): Remove this after real tests for cryptographic exchange exist.
        // mFriendStore.addFriend(FriendStore.bytesToBase64(new byte[]{0, 0}));
        // mFriendStore.addFriend(FriendStore.bytesToBase64(new byte[]{0, 1}));
        // mFriendStore.addFriend(FriendStore.bytesToBase64(new byte[]{1, 0}));
        // mFriendStore.addFriend(FriendStore.bytesToBase64(new byte[]{1, 1}));

        mWifiDirectSpeaker = new WifiDirectSpeaker(this, 
                                                   mPeerManager, 
                                                   mBluetoothSpeaker,
                                                   new WifiDirectFrameworkGetter());

        mMessageStore = new MessageStore(RangzenService.this, StorageBase.ENCRYPTION_DEFAULT);


        String btAddress = mBluetoothSpeaker.getAddress();
        mWifiDirectSpeaker.setWifiDirectUserFriendlyName(RSVP_PREFIX + btAddress);
        mWifiDirectSpeaker.setmSeekingDesired(true);

        // Schedule the background task thread to run occasionally.
        mScheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        // TODO(lerner): Decide if 1 second is an appropriate time interval for
        // the tasks.
        mBackgroundExecution = mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                backgroundTasks();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Called when the service is destroyed.
     */
    public void onDestroy() {
      mBackgroundExecution.cancel(true);
      return;
    }


    /**
     * Check whether we can connect, according to our policies.
     * Currently, checks that we've waited TIME_BETWEEN_EXCHANGES_MILLIS 
     * milliseconds since the last exchange and that we're not already connecting.
     *
     * @return Whether or not we're ready to connect to a peer.
     * @see TIME_BETWEEN_EXCHANGES_MILLIS
     * @see getConnecting
     */
    private boolean readyToConnect() {
      long now = System.currentTimeMillis();
      long lastExchangeMillis = mStore.getLong(LAST_EXCHANGE_TIME_KEY, -1);

      boolean timeSinceLastOK;
      if (lastExchangeMillis == -1) {
        timeSinceLastOK = true;
      } else if (now - lastExchangeMillis < TIME_BETWEEN_EXCHANGES_MILLIS) {
        timeSinceLastOK = false;
      } else {
        timeSinceLastOK = true;
      }
      Log.v(TAG, "Ready to connect? " + (timeSinceLastOK && !getConnecting()));
      Log.v(TAG, "Connecting: " + getConnecting());
      Log.v(TAG, "timeSinceLastOK: " + timeSinceLastOK);
      return timeSinceLastOK && !getConnecting(); 
    }

    /**
     * Set the time of the last exchange, kept in storage, to the current time.
     */
    private void setLastExchangeTime() {
      Log.i(TAG, "Setting last exchange time");
      long now = System.currentTimeMillis();
      mStore.putLong(LAST_EXCHANGE_TIME_KEY, now);
    }

    /**
     * Method called periodically on a background thread to perform Rangzen's
     * background tasks.
     */
    public void backgroundTasks() {
        // Log.v(TAG, "Background Tasks Started");

        // TODO(lerner): Why not just use mPeerManager?
        PeerManager peerManager = PeerManager.getInstance(getApplicationContext());
        peerManager.tasks();
        mBluetoothSpeaker.tasks();
        mWifiDirectSpeaker.tasks();

        List<Peer> peers = peerManager.getPeers();
        // TODO(lerner): Don't just connect all willy-nilly every time we have
        // an opportunity. Have some kind of policy for when to connect.
        if (peers.size() > 0 && readyToConnect() ) {
          Peer peer = peers.get(mRandom.nextInt(peers.size()));
          try {
            if (peerManager.thisDeviceSpeaksTo(peer)) {
              // Connect to the peer, starting an exchange with the peer once
              // connected. We only do this if thisDeviceSpeaksTo(peer), which
              // checks whether we initiate conversations with this peer or
              // it initiates with us (a function of our respective addresses).
              connectTo(peer);
            }
          } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "No such algorithm for hashing in thisDeviceSpeaksTo!? " + e);
            return;
          } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding exception in thisDeviceSpeaksTo!?" + e);
            return;
          }
        } else {
          Log.v(TAG, String.format("Not connecting (%d peers, ready to connect is %s)",
                                   peers.size(), readyToConnect()));
        }
        mBackgroundTaskRunCount++;

        // Log.v(TAG, "Background Tasks Finished");
    }

    /**
     * Connect to the peer via Bluetooth. Upon success, start an exchange with
     * the peer. If we're already connecting to someone, this method returns
     * without doing anything.
     *
     * @param peer The peer we want to talk to.
     */
    public void connectTo(Peer peer) {
      if (getConnecting()) {
        Log.w(TAG, "connectTo() not connecting to " + peer + " -- already connecting to someone");
        return;
      }

      Log.i(TAG, "connecting to " + peer);
      // TODO(lerner): Why not just use mPeerManager?
      PeerManager peerManager = PeerManager.getInstance(this);

      // This gets reset to false once an exchange is complete or when the 
      // connect call below fails. Until then, no more connections will be
      // attempted. (One at a time now!)
      setConnecting(true);

      Log.i(TAG, "Starting to connect to " + peer.toString());
      // The peer connection callback (defined elsewhere in the class) takes
      // the connect bluetooth socket and uses it to create a new Exchange.
      mBluetoothSpeaker.connect(peer, mPeerConnectionCallback);
    }

    /**
     * Handles connection to a peer by taking the connected bluetooth socket and
     * using it in an Exchange.
     */
    /*package*/ PeerConnectionCallback mPeerConnectionCallback = new PeerConnectionCallback() {
      @Override
      public void success(BluetoothSocket socket) {
        Log.i(TAG, "Callback says we're connected to " + socket.getRemoteDevice().toString());
        if (socket.isConnected()) {
          mSocket = socket;
          Log.i(TAG, "Socket connected, attempting exchange");
          try {
            mExchange = new BandwidthMeasurementExchange(
                socket.getInputStream(),
                socket.getOutputStream(),
                true,
                new FriendStore(RangzenService.this, StorageBase.ENCRYPTION_DEFAULT),
                new MessageStore(RangzenService.this, StorageBase.ENCRYPTION_DEFAULT),
                RangzenService.this.mBandwidthExchangeCallback);

            // Latency of communication: Start timer here.
            exchangeStartTimeMillis = System.currentTimeMillis();

            (new Thread(mExchange)).start();
          } catch (IOException e) {
            RangzenService.this.cleanupAfterExchange();
            Log.e(TAG, "Getting input/output stream from socket failed: " + e);
            Log.e(TAG, "Exchange not happening.");
          }
        } else {
          RangzenService.this.cleanupAfterExchange();
          Log.w(TAG, "But the socket claims not to be connected!");
        }
      }
      @Override
      public void failure(String reason) {
        RangzenService.this.cleanupAfterExchange();
        Log.i(TAG, "Callback says we failed to connect: " + reason);
        setConnecting(false);
        setLastExchangeTime();
      }
    };

    /**
     * Logs events to a CSV file on external storage.
     * TODO(lerner): Log to file instead of just to console!
     *
     * @param row String array of column values for the CSV.
     */
    private void logEventToCSV(String[] row) {
      try { 
        Log.i(TAG, row.toString());
        String BENCHMARK_FILENAME = "benchmarks.csv";
        FileWriter file = new FileWriter(new File(getBenchmarkDataDir(), BENCHMARK_FILENAME));
        CSVWriter writer = new CSVWriter(file, ',', '\'');
        writer.writeNext(row);

        file.close();
        writer.close();
      } catch (IOException e) {
        Log.e(TAG, "Failed to write to CSV: " + e);
      }
    }

    /**
     */
    /* package */ ExchangeCallback mBandwidthExchangeCallback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
        RangzenService.this.cleanupAfterExchange();
      }
      @Override
      public void failure(Exchange exchange, String reason) {
        RangzenService.this.cleanupAfterExchange();
      }
    };

    /**
     * Passed to an Exchange to be called back to when the exchange completes.
     * Records the time since the exchange started for benchmarking.
     */
    /* package */ ExchangeCallback mLatencyBenchmarkCallback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
        // Latency of communication: Stop timer here.
        long exchangeStopTimeMillis = System.currentTimeMillis();
        // Latency of communication: Calculate RTT from # of round trips in exchange.
        // In this case, we're using a NonceEchoExchange, which includes one round-trip,
        // so the RTT time is just the time between the start and end of the exchange.
        long rttMillis = exchangeStopTimeMillis - exchangeStartTimeMillis;

        // Latency of communication: Record RTT and any other parameters.
        // TODO(lerner): Record time.
        String init = exchange.asInitiator ? "initiator" : "listener";
        Log.i(TAG, String.format("Exchange complete as %s, took %d milliseconds", init, rttMillis));

        BluetoothSocket activeSocket = (mSocket != null) ? mSocket : mBluetoothSpeaker.mSocket;
        RangzenService.this.logEventToCSV(new String[] {
          activeSocket.getRemoteDevice().getAddress(),
          init,
          Long.toString(System.currentTimeMillis()),
          Long.toString(rttMillis)
        });

        RangzenService.this.cleanupAfterExchange();
      }
      @Override
      public void failure(Exchange exchange, String reason) {
        long exchangeStopTimeMillis = System.currentTimeMillis();
        long rttMillis = exchangeStopTimeMillis - exchangeStartTimeMillis;
        Log.e(TAG, String.format("Exchange failed, latency benchmark took %d milliseconds", rttMillis));

        RangzenService.this.cleanupAfterExchange();
      }
    };

    /** 
     * Checks if external storage is available for read and write. If it's mounted
     * by a computer or it may not be.
     *
     * @return True if external storage is writable, false otherwise.
     */
    /* package */ boolean isExternalStorageWritable() {
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) {
        return true;
      }
      return false;
    }

    /**
     * Checks if external storage is available to at least read.
     *
     * @return True if external storage is readable, false otherwise.
     */
    /* package */ boolean isExternalStorageReadable() {
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state) ||
          Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
        return true;
      }
      return false;
    }

    /**
     * Get the directory where we store our benchmark data.
     * This directory is in external storage where it's accessible over USB
     * and by other apps.
     *
     * @return A file object pointing to a directory where we'll store benchmark
     * data files.
     */
    private File getBenchmarkDataDir() {
      File file = new File(Environment.getExternalStorageDirectory(), BENCHMARK_DIR);
      if (!file.exists()) {
        if (!file.mkdirs()) {
          Log.e(TAG, "Benchmark data dir not created: " + file);
          return null;
        }
      } else if (file.exists() && !file.isDirectory()) {
        Log.e(TAG, "Benchmark data dir " + file + " exists but isn't a directory!");
        return null;
      }

      return file;
    }

    /**
     * Cleans up sockets and connecting state after an exchange, including recording
     * that an exchange was just attempted, that we're no longer currently connecting,
     * closing sockets and setting socket variables to null, etc.
     */
    /* package */ void cleanupAfterExchange() {
      setConnecting(false);
      setLastExchangeTime();
      try {
        if (mSocket != null) {
          mSocket.close();
        }
        if (mBluetoothSpeaker.mSocket != null) {
          mBluetoothSpeaker.mSocket.close();
        }
      } catch (IOException e) {
        Log.w(TAG, "Couldn't close bt socket after exhange success: " + e);
      }
      mSocket = null;
      mBluetoothSpeaker.mSocket = null;
    }

    /**
     * Passed to an Exchange to be called back to when the exchange completes.
     * Performs the integration of the information received from the exchange -
     * adds new messages to the message store, weighting their priorities
     * based upon the friends in common.
     */
    /* package */ ExchangeCallback mExchangeCallback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
        List<RangzenMessage> newMessages = exchange.getReceivedMessages();
        int friendOverlap = exchange.getCommonFriends();
        Log.i(TAG, "Got " + newMessages.size() + " messages in exchangeCallback");
        Log.i(TAG, "Got " + friendOverlap + " common friends in exchangeCallback");
        for (RangzenMessage message : newMessages) {
          Set<String> myFriends = mFriendStore.getAllFriends();
          double stored = mMessageStore.getPriority(message.text);
          double remote = message.priority;
          double newPriority = Exchange.newPriority(remote, stored, friendOverlap, myFriends.size());
          try {
            if (mMessageStore.contains(message.text)) {
              mMessageStore.updatePriority(message.text, newPriority);
            } else {
              mMessageStore.addMessage(message.text, newPriority);
            }
          } catch (IllegalArgumentException e) {
            Log.e(TAG, String.format("Attempted to add/update message %s with priority (%f/%f)" +
                                    ", %d friends, %d friends in common",
                                    message.text, newPriority, message.priority, 
                                    myFriends.size(), friendOverlap));
          }
        }
        RangzenService.this.cleanupAfterExchange();
      }

      @Override
      public void failure(Exchange exchange, String reason) {
        Log.e(TAG, "Exchange failed, reason: " + reason);
        RangzenService.this.cleanupAfterExchange();
      }
    };

    /**
     * Check whether any network connection (Wifi/Cell) is available according
     * to the OS's connectivity service.
     * 
     * @return True if any network connection seems to be available, false
     *         otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Return true if Bluetooth is turned on, false otherwise.
     * 
     * @return Whether Bluetooth is enabled.
     */
    private boolean isBluetoothOn() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        } else {
            return adapter.isEnabled();
        }
    }

    /**
     * Return the number of times that background tasks have been executed since
     * the service was started.
     * 
     * @return The number of times backgroundTasks() has been called.
     */
    public int getBackgroundTasksRunCount() {
        return mBackgroundTaskRunCount;
    }
  
    /**
     * Get the time at which this instance of the service was started.
     * 
     * @return A Date representing the time at which the service was started.
     */
    public Date getServiceStartTime() {
        return mStartTime;
    }

    /** Synchronized accessor for connecting. */
    private synchronized boolean getConnecting() {
      return connecting;
    }

    /** Synchronized setter for connecting. */
    private synchronized void setConnecting(boolean connecting) {
      this.connecting = connecting;
    }

    /**
     * This method has to be implemented on a service, but I haven't written the
     * service with binding in mind. Unsure what would happen if it were used
     * this way.
     * 
     * @param intent
     *            The intent used to bind the service (passed to
     *            Context.bindService(). Extras included in the intent will not
     *            be visible here.
     * @return A communication channel to the service. This implementation just
     *         returns null.
     * @see android.app.Service
     * 
     */
    @Override
    public IBinder onBind(Intent intent) {
        // This service is not meant to be used through binding.
        return null;
    }
}
