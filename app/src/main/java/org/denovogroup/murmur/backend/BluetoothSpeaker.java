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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Handles interaction with Android's Bluetooth subsystem.
 */
public class BluetoothSpeaker {
  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "BluetoothSpeaker";

  /** The expected size of peer exchanges. */
  public static final int EXCHANGE_SIZE = 1024 * 30;

  /** Constant int passed to request to enable Bluetooth, required by Android. */
  public static final int REQUEST_ENABLE_BT = 54321;

    private static final int SOCKET_CONNECTION_TIMEOUT = 30 * 1000;

  /** SDP name for creating Rangzen service on listening socket. */
  private static final String SDP_NAME = "RANGZEN_SDP_NAME";

  /** Payload for exchange. */
  private static byte[] mPayload;

  /** A UUID for this particular device. */
  private UUID mThisDeviceUUID;
  
  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private BluetoothAdapter mBluetoothAdapter;

  /** A handle to a server socket which receives connections from remote BT peers. */
  private BluetoothServerSocket mServerSocket;

  /** Socket received from accepting on server socket. */
  /* package */ BluetoothSocket mSocket;

  /** Thread which calls accept on the server socket. */
  private Thread mConnectionAcceptingThread;

  /** Ongoing exchange. */
  private Exchange mExchange;

  /** Context of the Rangzen Service. */
  private MurmurService mContext;

  /** Receives Bluetooth related broadcasts. */
  private BluetoothBroadcastReceiver mBluetoothBroadcastReceiver;

    private static final Logger log = Logger.getLogger(TAG);


  /**
   * @param context A context, from which to access the Bluetooth subsystem.
   * @param peerManager The app's PeerManager instance.
   */
  public BluetoothSpeaker(MurmurService context, PeerManager peerManager) {
    super();
      log.info( "Creating BluetoothSpeaker");
    mPayload = new byte[EXCHANGE_SIZE];
    for (int i=0; i < EXCHANGE_SIZE; i++) {
      mPayload[i] = (byte) i;
    }

    this.mContext = context;
    this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    this.mBluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(context);

    if (mBluetoothAdapter == null) {
      // TODO (lerner): Tell the server that this device doesn't do Bluetooth.
      // TODO (lerner): Opt this device out of data collection?
        mBluetoothBroadcastReceiver.showNoBluetoothNotification(context);
      log.error( "Device doesn't support Bluetooth.");
      return;
    } else if (!mBluetoothAdapter.isEnabled()) {
      log.error("Got a non-null Bluetooth Adapter but it's not enabled.");
       // TODO(lerner): This is contrary to Android user experience, which
       // states that apps should never turn on Bluetooth without explicit
       // user interaction. We should instead prompt the user with the
       // Android built-in intent for asking to turn on Bluetooth.
      /*if (mBluetoothAdapter.enable()) {
        Log.i(TAG, "Enabling Bluetooth.");
      } else {
        Log.e(TAG, "Attempt to enable Bluetooth returned false.");
      }*/
    }

      this.mThisDeviceUUID = getUUIDFromMACAddress(getAddress());
      log.info( "This device's UUID is " + mThisDeviceUUID.toString());

    if (mBluetoothAdapter.isEnabled()) {
      try { 
        createListeningSocket();
        spawnConnectionAcceptingThread();
      } catch (IOException e) {
        log.error("Failed to create listening BT server socket. ", e);
          e.printStackTrace();
        log.error("Can't receive incoming connections.");
          ServiceWatchDog.getInstance().notifyError(e);
      }
    } else {
        mBluetoothBroadcastReceiver.showNoBluetoothNotification(context);
    }
    log.debug( "Finished creating BluetoothSpeaker.");
  }

  /**
   * Retrieve a BluetoothDevice corresponding to the given address.
   *
   * @return A BluetoothDevice with the given address.
   */
  public BluetoothDevice getDevice(String address) {
    if (mBluetoothAdapter != null) {
      try { 
        return mBluetoothAdapter.getRemoteDevice(address);
      } catch (IllegalArgumentException e) {
        log.error( "Passed illegal address to get remote bluetooth device: '" + address+"'");
        return null;
      }
    } else {
      return null;
    }

  }
  /**
   * Retrieve our Bluetooth MAC address.
   *
   * @return The Bluetooth MAC address of this device.
   */
  public String getAddress() {
    if(mBluetoothAdapter == null)
      return null;

    if(Build.VERSION.SDK_INT >= 23){
      if(mContext == null)
        return null;
      return SecurityManager.getStoredMAC(mContext);
    }

    return mBluetoothAdapter.getAddress();
  }

  /**
   * Sanity check a string for whether it looks basically like a BT MAC. Just
   * regexes the string, looking for 6 groups of hex digits deparated by : or -.
   * Case insensitive.
   *
   * @param s A string which is to be checked for whether it looks like a Bluetooth
   * MAC address.
   * @return True if the string looks like a MAC address, false otherwise.
   */
  public static boolean looksLikeBluetoothAddress(String s) {
     return Pattern.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", s);
  }

  /**
   * Check whether the address is owned by IANA or is the broadcast address
   * (FF:FF:FF:FF:FF:FF).
   *
   * TODO(lerner): Improve this check. We might want to reject all multicast/broadcast
   * and such; addresses not assigned to anyone; or other reserved addresses that
   * I might not have been able to find.
   *
   * @param s An address to be checked.
   * @return True if the address appears to be IANA owned or a broadcast address.
   * Returns false if the address doesn't seem to be a MAC address at all.
   */
  public static boolean isReservedMACAddress(String s) {
    if (!looksLikeBluetoothAddress(s)) {
      return false;
    }
    // Normalize the address to simplify the regexes.
    s = s.replace(":", "-");
    s = s.toUpperCase();     

    // Check against IANA-owned addresses and the broadcast address.
    //
    // https://www.iana.org/assignments/ethernet-numbers/ethernet-numbers.xhtml
    // IANA is assigned all addresses starting 00-00-5E and 01-00-5E (the flipped
    // least significant bit in the first octet indicates multicast instead of unicast).
    //
    // The second to least significant bit also has special meaning (group/individual)
    // so I interpret that 2/3 are also reserved to IANA, even though their
    // doc (linked above) only mentions 00-00-5E and 01-00-5E.
    if (Pattern.matches("^0[0-3]-00-5E.*", s)) {
      return true;
    }
    else if (Pattern.matches("^FF-FF-FF-FF-FF-FF$", s)) { // Broadcast address.
      return true;
    }
    return false;
  }

  /**
   * Creates a thread which listens on the BluetoothSpeaker's
   * BluetoothServerSocket as long as Bluetooth reamins on, accepting any
   * incoming connections and completing exchanges with them.
   *
   * If Bluetooth is turned off (or an exception occurs for any other reason 
   * while accepting a new connection), the thread dies and will be restarted
   * later when Bluetooth is on again.
   */
  private void spawnConnectionAcceptingThread() {
    mConnectionAcceptingThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            // Wait between accepting connections.
            try {
              Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
              log.error( "Connection accepting thread was interrupted during sleep: ",e);
            }
            acceptConnection();
          } catch (IOException e) {
            log.error("IOException while accepting/responding to a connection",e);
            if (!mBluetoothAdapter.isEnabled()) {
              log.error("Bluetooth adapter is disabled; not accepting connections.");
              mServerSocket = null;
              return;
            }
          }
        }
      }
    };
    mConnectionAcceptingThread.start();
  }
  
  /**
   * Create a listening Bluetooth socket and listen for murmur connections.
   */
  private void createListeningSocket() throws IOException {
      mServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SDP_NAME, mThisDeviceUUID);
      log.info(String.format("Listening socket created: Listening (insecure RFCOMM) - name <%s>, UUID <%s>.",
              SDP_NAME, mThisDeviceUUID));
  }

    /**
   * Accept a connection to the BluetoothServerSocket we've set up.
   * @throws IOException
   */
  private void acceptConnection() throws IOException {
    if (mServerSocket == null) {
      throw new IOException("ServerSocket is null, not trying to accept().");
    } else if (!mBluetoothAdapter.isEnabled()) {
      throw new IOException("Bluetooth adapter is disabled, not trying to accept().");
    }
    log.info( "Calling mServerSocket.accept()");
    mSocket = mServerSocket.accept();
    log.info("Accepted socket from " + mSocket.getRemoteDevice());
    log.info( "Accepted socket connected? " + mSocket.isConnected());
      MurmurService.direction = -1;
      MurmurService.remoteAddress = mSocket.getRemoteDevice().getAddress();
    mExchange = new CryptographicExchange(
                            mContext,
                            mSocket.getRemoteDevice().getAddress(),
                            mSocket.getInputStream(),
                             mSocket.getOutputStream(),
                             false,
                             FriendStore.getInstance(mContext),
                             MessageStore.getInstance(mContext),
                             mContext.mExchangeCallback);
    //mExchange.execute((Boolean) null);
    // Start the exchange.
    (new Thread(mExchange)).start();
  }

  /**
   * Called periodically by the background tasks method of the murmur Service.
   * Recreates the listening socket and thread if they're not active. The listening
   * thread might be inactive if Bluetooth was turned off previously. return true
   * if state allow for proper bluetooth handling or false if process should be terminated
   */
  public boolean tasks() {
    log.info("Starting BluetoothSpeaker tasks.");
      log.info("mServerSocket:"+(mServerSocket == null)
              +" mBluetoothAdapter:"+(mBluetoothAdapter != null)
              +" mConnectionAcceptingThread:"+(mBluetoothAdapter != null));
    if (mServerSocket == null         && mBluetoothAdapter != null &&
        mBluetoothAdapter.isEnabled() && (mConnectionAcceptingThread == null || !mConnectionAcceptingThread.isAlive())) {
      try { 
        log.info("No ServerSocket, creating a new one.");
        createListeningSocket();
        spawnConnectionAcceptingThread();
      } catch (IOException e) {
        log.error("Tasks: failed to create listening BT server socket. ",e);
        log.error("Can't receive incoming connections.");
        return false;
      }
    }

      if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
          log.info("bluetooth tasks failed, bluetooth disabled");
          return false;
      }

      log.info("finished BluetoothSpeaker tasks.");
      return true;
  }

  /**
   * Convert buffer to string. 
   *
   * TODO(lerner): Improve encoding. Currently uses default encoding and 10 
   * bytes, whatever they are, ignoring any terminators.
   *
   * @return 10 bytes of the array, rendered as characters in the default encoding.
   */
  private String bufferToString(char[] charArray) {
    return new String(charArray, 0, 10);
  }

  public void connect(Peer peer, PeerConnectionCallback callback) {
    // Start connecting in a new thread, passing the callback and peer.
    // Return.
    (new Thread(new ConnectionRunnable(peer, callback))).start();
    return;
  }

  /**
   * Use the bytes of the given address to generate a type-3 UUID.
   *
   * @param address The MAC address to be converted into a UUID.
   * @return A UUID corresponding to the MAC address given.
   */
  private UUID getUUIDFromMACAddress(String address) {
    if (address == null) {
      return null;
    } else {
      return UUID.nameUUIDFromBytes(address.getBytes());
    }
  }

    /**
   * A thread which attempts to connect to the given peer and return a Bluetooth
   * Socket to that peer through the success() method of the connection callback.
   */
  private class ConnectionRunnable implements Runnable {
    /** The peer to which we're attempting to connect. */
    private Peer mPeer;

    /** A callback to report success (and the open socket) or failure. */
    private PeerConnectionCallback mCallback;

    /**
     * Create a new ConnectionRunnable which will connect to the given peer
     * and report success or failure on the given callback.
     *
     * @param peer A remote peer to connect to.
     * @param callback A PeerConnectionCallback to report success or failure.
     */
    public ConnectionRunnable(Peer peer, PeerConnectionCallback callback) {
      this.mPeer = peer;
      this.mCallback = callback;
    }
    
    /**
     * Connect to the peer and report success or failure.
     */
    public void run() {
        log.info( "Running connectionRunnable for :"+mPeer);
      BluetoothDevice device = mPeer.getNetwork().getBluetoothDevice();
      if (device == null) {
        mCallback.failure("No bluetooth device for peer " + mPeer.toString());
        return;
      }
      UUID remoteUUID = getUUIDFromMACAddress(device.getAddress()); 
      final BluetoothSocket socket;
      try {
        socket = device.createInsecureRfcommSocketToServiceRecord(remoteUUID);
      } catch (IOException e) {
        mCallback.failure(
            String.format("Failed to create insecure RFCOMM socket to %s on peer %s. IOException: %s", 
                           remoteUUID, mPeer, e)
        );
        return;
      }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        class ConnectBluetoothSocket implements Callable<Boolean>{
            /* LIRAN: attempt to connect to current socket, this implementation allow
             * timing out the connection if takes too long, it was found that
             * from time to time socket.connect() was blocked indefinitely which
             * in turn cause the app to stop receiving/sending messages
             */
            @Override
            public Boolean call() throws Exception {
                // cancel the discovery before connecting since it slows down connection.
                if(mBluetoothAdapter != null) mBluetoothAdapter.cancelDiscovery();
                socket.connect();
                return false;
            }
        }

        Future<Boolean> task = executor.submit(new ConnectBluetoothSocket());
        try {
            Boolean connectSuccessful = task.get(SOCKET_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            log.debug("got socket connection result");
        } catch (ExecutionException e) {
            mCallback.failure(
                    String.format("Exception connecting to %s on peer %s. IOException: %s",
                            remoteUUID, mPeer, e)
            );
            return;
        } catch (InterruptedException|TimeoutException e) {
            log.error("socket connection timed out for: " + mPeer, e);
            mCallback.failure(
                    String.format("Exception connecting to %s on peer %s. IOException: %s",
                            remoteUUID, mPeer, e)
            );
            return;
        }

      if (socket.isConnected()) {
        mCallback.success(socket);
      } else {
        mCallback.failure(String.format("Socket to %s on %s wasn't connected after connection attempt.",
                                        remoteUUID, mPeer));
      }
    }
  }

    public void dismissNoBluetoothNotification(){
        mBluetoothBroadcastReceiver.dismissNoBluetoothNotification(mContext);
    }

    public void unregisterReceiver(Context context){
        log.info( "Unregistering Bluetooth receiver");
        context.unregisterReceiver(mBluetoothBroadcastReceiver);
    }
}
