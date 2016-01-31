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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 * This module exposes an API for the application to find out the current 
 * list of peers, to request that new peers be sought, and to request that
 * peers be garbage collected. The API and all the behavior of this class are 
 * independent of underlying protocols for those peers. 
 */
public class PeerManager {
  /** 
   * A static variable to hold the active instance of PeerManager so that other
   * app components can call methods in its API.
   */
  private static PeerManager sPeerManager;

  // TODO(lerner): I suspect we want to convert this to a Set eventually, since
  // that best represents the set of peers we can see (unordered, each is
  // unique).
  /** The most recent, up-to-date list of peers. */
  private List<Peer> mCurrentPeers;

  /** For app-local intent broadcasting/receiving of peer events. */
  private LocalBroadcastManager mBroadcastManager;

  /** Handle to the app's BluetoothSpeaker. */
  private BluetoothSpeaker mBluetoothSpeaker;

  /** Remembers the last time we successfully had an exchange with a peer. */
  private Map<String, Date> exchangeTimes = new HashMap<String, Date>();

  /** Remembers the last time we attempted an exchange with a peer. */
  private Map<String, Date> exchangeAttemptTimes = new HashMap<String, Date>();

  /** 
   * The length of time (in milliseconds) we consider peers valid. 
   * TODO(lerner): Decide on an appropriate value for this.
   */
  public static final long PEER_TIMEOUT = 2 * 60 * 1000;

  /** 
   * The length of time we wait before talking to the same peer again, in ms.
   * TODO(lerner): Decide on an appropriate value for this.
   */
  private static final long MS_BETWEEN_EXCHANGES = 15 * 60 * 1000;

  /** 
   * The max length of time we wait before attempted to exchange with the same peer again, in ms.
   * TODO(barath): Decide on an appropriate value for this.
   */
  private static final long MS_BETWEEN_EXCHANGE_ATTEMPTS = 5 * 60 * 1000;

  /** A Random instance for use in timing exchange attempts. */
  private static final Random random = new Random();

  /** Displayed in Android Monitor logs. */
  private static String TAG = "MurmurPeerManager";

    private static final Logger log = Logger.getLogger(TAG);


  /**
   * Private constructor. Use PeerManager.getInstance() to obtain the app's
   * instance of the class.
   *
   * @param context A context object from the app.
   */
  private PeerManager(Context context) {
    mCurrentPeers = new ArrayList<Peer>();
    mBroadcastManager = LocalBroadcastManager.getInstance(context); 

    log.debug( "Finished PeerManager constructor.");
  }

  /**
   * Obtain the current instance of PeerManager.
   *
   * @param context A context object from the app.
   * @return The app's instance of PeerManager.
   */
  public static PeerManager getInstance(Context context) {
    if (sPeerManager == null) {
      sPeerManager = new PeerManager(context);
      log.debug( "Created instance of PeerManager");
    }
    return sPeerManager;
  }

  /**
   * This method garbage runs the peer garbage collector on all peers that
   * should be garbage collected. It runs synchronously and returns when done,
   * but should be very fast (deciding whether to garbage collect a peer is
   * not a complicated action).
  */
  public synchronized void garbageCollectPeers() {
    for (Peer p : mCurrentPeers) {
      if (shouldGarbageCollectPeer(p)) {
        garbageCollectPeer(p);
      }
    } 
  }

  /**
   * Check whether a peer is already in the peer list. 
   *
   * Peer equality is based on whether their PeerNetworks refer to the same
   * destinations, so two peers might be .equals() even if not ==.
   *
   * @param peer The Peer to find in the list.
   * @return True if the peer is in the list, false otherwise.
   */
  public synchronized boolean isKnownPeer(Peer peer) {
    for (Peer peerInList : mCurrentPeers) {
      if (peer.equals(peerInList)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update the last seen time of the peer.
   *
   * It is possible (indeed, frequent) to have two different peer objects
   * which logically refer to the same peer. This updates the canonical copy -
   * the one stored in the peer list - as well as the copy passed in.
   *
   * @param peer The Peer to update.
   */
  private synchronized void touchPeer(Peer peer) {
    Peer copyInList = getCanonicalPeer(peer);
    if (copyInList != null) {
      copyInList.touch();
    } 
   if (peer != null) {
      peer.touch();
    }
  }

  /**
   * If the peer given is known to the peer manager, return a canonical
   * Peer object which represents the peer and is .equals() to the peer
   * given. If the peer requested is not yet known, returns the peer
   * requested as its own canonical form.
   *
   * @param peerDesired The peer to look up.
   * @return The canonical version of the given peer, which is the same
   * object if the peer is not yet known to the PeerManager.
   */
  public synchronized Peer getCanonicalPeer(Peer peerDesired) {
    if (peerDesired == null) {
      return null;
    }
    for (Peer peerInList : mCurrentPeers) {
      if (peerDesired.equals(peerInList)) {
        return peerInList;
      }
    }
    // If not already known, add the peer to make it actually canonical.
    addPeer(peerDesired);
    return peerDesired;
  }

  /**
   * Check whether a peer is considered old enough to consider it unlikely
   * to return.
   */
  private synchronized boolean shouldGarbageCollectPeer(Peer peer) {
    Date lastSeen = peer.getLastSeen();
    Date now = new Date();

    long msSinceSeen = now.getTime() - lastSeen.getTime();

    // TODO(lerner): Use a more sophisticated mechanism than a simple
    // time threshold since last seen. For example, we may not want to evict
    // anyone if we haven't scanned for a while (or, maybe we do).
    return msSinceSeen > PEER_TIMEOUT;
  }

  /**
   * Invalidates a peer and removes it from the PeerManager's list of current
   * peers.
   */
  private synchronized void garbageCollectPeer(Peer peer) {
    log.debug( "Garbage collected peer " + peer);
    mCurrentPeers.remove(peer);
  }

  /**
   * Get a snapshot of the current list of peers. Peers are not guaranteed 
   * to be reachable or still in existence, and the snapshot may be outdated
   * as peers are sought.
   *
   * @return A copy of the list of currently known peers.
   */
  public synchronized List<Peer> getPeers() {
    List<Peer> copy = new ArrayList<Peer>();
    for (Peer p : mCurrentPeers) {
      copy.add(p);
    }
    return copy;
  }

  /**
   * Add list of peers to the current peers list, just as though they were
   * added individually with newPeer().
   *
   * @return The number of non-duplicate peers added.
   */
  public synchronized int addPeers(List<Peer> newPeers) {
    int nonDuplicateCount = 0;
    for (Peer p : newPeers) {
      if (addPeer(p)) {
        nonDuplicateCount++;
      }
    }
    return nonDuplicateCount;
  }

  /**
   * Remove all peers from the peer list.
   */
  public synchronized void forgetAllPeers() {
    mCurrentPeers.clear();
  }

  /**
   * Add a peer to the current list of peers. Used internally to add peers
   * discovered, but also can be called externally to add peers which use
   * very asynchronous mechanisms (e.g. SD card) which do not support
   * automatic discovery.
   *
   * @return True if the peer was added, false if the peer was a duplicate
   * and thus was already in the list.
   */
  public synchronized boolean addPeer(Peer p) {
    if (isKnownPeer(p)) {
      touchPeer(p);
      return false;
    } else {
      mCurrentPeers.add(p);
      return true;
    }
  }

  /**
   * Tell the PeerManager about the app's BluetoothSpeaker.
   *
   * @param speaker The app's instance of BluetoothSpeaker.
   */
  public void setBluetoothSpeaker(BluetoothSpeaker speaker) {
    mBluetoothSpeaker = speaker;
  }

  /**
   * Remember the time that this exchange occurred in a local map, in order to
   * prevent contacting the same peer repeatedly in a short time.
   *
   * @param peer The remote peer about whom we are remembering an exchange.
   * @param exchangeTime The time at which we had an exchange with the peer.
   */
  private void recordExchangeTime(Peer peer, Date exchangeTime) {
    BluetoothDevice device = peer.getNetwork().getBluetoothDevice();
    if (device == null) {
      log.error( "Recording exchange time of non-bluetooth peer! Can't do it.");
      return;
    } else {
      exchangeTimes.put(device.getAddress(), exchangeTime);
    }
  }

  /**
   * Remember the time that an exchange was attempted.
   *
   * @param peer The remote peer about whom we are remembering an exchange attempt.
   * @param exchangeTime The time at which we had an exchange with the peer.
   */
  private void recordExchangeAttemptTime(Peer peer, Date exchangeTime) {
    BluetoothDevice device = peer.getNetwork().getBluetoothDevice();
    if (device == null) {
      log.error( "Recording exchange attempt time of non-bluetooth peer! Can't do it.");
      return;
    } else {
      Date nextAttempt = new Date(exchangeTime.getDate() +
                                  (random.nextInt() % MS_BETWEEN_EXCHANGE_ATTEMPTS));
      exchangeAttemptTimes.put(device.getAddress(), nextAttempt);
      log.warn( "Will attempt another exchange with peer " + peer + " no sooner than " + nextAttempt);
    }
  }

  /**
   * Return a date representing the last time we spoke to this peer. If we
   * don't remember ever speaking to the peer, the epoch (beginning of time).
   * Values of last exchange times are not persisted - they're only stored as
   * instance variables, so these times are reliable only within the same Murmur
   * process.
   *
   *
   * @param peer The peer about which we are inquiring.
   * @return The Date at which the last known successful exchange with the peer
   * occurred, or the epoch if none is known.
   */
  private Date getLastExchangeTime(Peer peer) {
    BluetoothDevice device = peer.getNetwork().getBluetoothDevice();
    if (device == null) {
      log.error( "Getting last exchange time of non-bluetooth peer! Can't do it!");
      return null;
    } else {
      Date when = exchangeTimes.get(device.getAddress());
      if (when == null) {
        return new Date(0);
      } else {
        return when;
      }
    }
  }

  /**
   * Return a date representing the next time we should attempt an exchange with
   * this peer. If we don't remember ever speaking to the peer, returns the
   * epoch (beginning of time).  Values of last exchange times are not persisted
   * - they're only stored as instance variables, so these times are reliable
   * only within the same Murmur process.
   *
   * @param peer The peer about which we are inquiring.
   * @return The Date at which the last known successful exchange with the peer
   * occurred, or the epoch if none is known.
   */
  private Date getNextExchangeAttemptTime(Peer peer) {
    BluetoothDevice device = peer.getNetwork().getBluetoothDevice();
    if (device == null) {
      log.error( "Getting last exchange time of non-bluetooth peer! Can't do it!");
      return null;
    } else {
      Date when = exchangeAttemptTimes.get(device.getAddress());
      if (when == null) {
        return new Date(0);
      } else {
        return when;
      }
    }
  }

  /**
   * Check whether we've had an exchange with the given peer within the last 
   * MS_BETWEEN_EXCHANGES ms. Time since exchange isn't persisted, so this might
   * answer false incorrectly if Murmur has been stopped and started.
   *
   * @return True if we've had an exchange with the peer within the threshold,
   * false otherwise.
   */
  private boolean recentlyExchangedWithPeer(Peer peer) {
    long now = (new Date()).getTime();
    long then = getLastExchangeTime(peer).getTime();
    return (now - then) < MS_BETWEEN_EXCHANGES;
  }

  /**
   * Check whether we can attempt another exchange with the given peer. Time
   * for exchange attempts isn't persisted, so this might answer incorrectly if
   * Murmur has been stopped and started.
   *
   * @return True if we've attempted an exchange with the peer within the threshold,
   * false otherwise.
   */
  private boolean recentlyAttemptedExchangeWithPeer(Peer peer) {
    long now = (new Date()).getTime();
    long then = getNextExchangeAttemptTime(peer).getTime();
    return (now < then);
  }

  /**
   * Run tasks, e.g. garbage collection of peers, speaker tasks, etc.
   */
  public void tasks() {
    log.info("Started PeerManager tasks.");

    garbageCollectPeers();
    
    log.info("Finished with PeerManager tasks.");
  }

  /**
   * Check whether this peer should start an exchange with the other peer
   * or allow that peer to start an exchange, based on their addresses.
   * Also returns false if the other peer isn't a Bluetooth using peer.
   *
   * @param other The other peer which we may want to talk to.
   * @return True if it is our responsibility to start exchanges between this
   * pair of devices, false otherwise.
   */
  public boolean thisDeviceSpeaksTo(Peer other) throws NoSuchAlgorithmException,
                                                       UnsupportedEncodingException {
    if (other == null || other.getNetwork() == null ||
        other.getNetwork().getBluetoothDevice() == null ) {
        log.info( "this device not speaking to peer: "+other+" either peer or peer network is null");
      return false;
    } 
    return thisDeviceSpeaksTo(other.getNetwork().getBluetoothDevice());
  }

  /**
   * Check whether this device should start an exchange with the other device
   * or allow that device to start an exchange, based on their addresses.
   *
   * @param other The other device which we may want to talk to.
   * @return True if it is our responsibility to start exchanges between this
   * pair of devices, false otherwise.
   */
  public boolean thisDeviceSpeaksTo(BluetoothDevice other) throws NoSuchAlgorithmException,
                                                                  UnsupportedEncodingException {

      /*TODO LIRAN this method cannot work along with the backoff algorithm if speaker
        device have a long backoff period and spokento device have some new data to share.
        speaker device will remain unreachable for the entire duration of the backoff, rendering
        the app useless, to resolve this this method has been silenced if USE_BACKOFF is set to true
       */
      if(MurmurService.USE_BACKOFF) return true;

      if (other == null) {
        log.info( "This device not speaking to peer, peer is null");
      return false;
    } 
    String otherAddr = other.getAddress();
    if (otherAddr == null) {
        log.info( "This device not speaking to peer :"+other+", peer address is null");
      return false;
    }
    String myAddr = mBluetoothSpeaker.getAddress(); 
    if (myAddr.equals(whichInitiates(myAddr, otherAddr))) {
      return true;
    } else {
      return false;
    }

  }

  /**
   * Decide which of the two addresses given should initiate communications.
   * The algorithm is: sort the strings lexicographically, concatenate them,
   * take their SHA-256 hash. If the first bit of the hash is a 1, then the
   * lexicographically LOWER one initiates. Otherwise, the HIGHER one initiates.
   *
   * @param a A string. The order of these parameters does not matter.
   * @param b A string. The order of these parameters does not matter.
   *
   * @return Which of the addresses should start speaking.
   */
  static String whichInitiates(String a, String b) throws NoSuchAlgorithmException,
                                                    UnsupportedEncodingException {
    if (a == null || b == null) {
      return null;
    }

    // If a < b, concatenate them in the order a + b.
    if (a.compareTo(b) < 0) {
      if (startsWithAOneBit(concatAndHash(a, b))) {
        return a;
      } else {
        return b;
      }
    // a > b, so concatenate them b + a.
    } else {
      if (startsWithAOneBit(concatAndHash(b, a))) {
        return b;
      } else {
        return a;
      }
    }
  }

  /**
   * Concatenates b to a and returns the SHA-256 of their concatenation.
   *
   * @param x A string
   * @param y Another string.
   * @return SHA-256(x+y)
   */
  static byte[] concatAndHash(String x, String y) throws NoSuchAlgorithmException,
                                                    UnsupportedEncodingException {
    MessageDigest md;
    md = MessageDigest.getInstance("SHA-256");
    md.update((x+y).getBytes("UTF-8"));
    return md.digest();
  }

  /**
   * Check whether the first bit of the 0'th byte of the given byte array is a 1.
   * 
   * @param bytes array of bytes.
   * @return True if the first bit of the 0th byte is a 1, false otherwise.
   */
  static boolean startsWithAOneBit(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return false;
    }
    return (bytes[0] & 0x01) == 0x01;
  }
}
