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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.ScanResult;
import android.bluetooth.BluetoothDevice;

/**
 * This class represents the network connectivity over wifi hotspot mode
 * that can be used to reach a certain peer.
 */
public class HotspotPeerNetwork implements PeerNetwork {
  /** A ScanResult (description of an AP) associated with this Peer Network */
  ScanResult scanResult;

  /**
   * Create a PeerNetwork with no remote network devices to talk to.
   */
  public HotspotPeerNetwork() { }

  /**
   * Create a PeerNetwork with a remote WifiP2pDevice to talk to .
   */
  public HotspotPeerNetwork(ScanResult scanResult) {
    this.scanResult = scanResult;
  }

  /**
   * Send a message. Returns immediately, sending the message asynchronously
   * as it is possible to do so given the constraints of the network media
   * (peer comes and goes, need to connect first, etc.)
   *
   * @return False if the message could not be sent, true otherwise.
   */
  public void send(String message) {
  }

  /**
   * Wait to receive a message from the peer. Currently always returns null.
   *
   * @return A byte array containing the contents of the message, or null on 
   * an error.
   */
  public byte[] receive() {
    return null;
  }

  /**
   * Return true if the ScanResults are the same (all fields equal),
   * false otherwise.
   *
   * @param a A scan result to compare.
   * @param b Another scan result to compare.
   * @return Whether the given scan results seem to point to the same
   * network.
   */
  private boolean scanResultsEqual(ScanResult a, ScanResult b) {
    if (a == null || b == null) {
      return a == b;
    } else {
      boolean sameBSSID = (a.BSSID == null ? 
                           b.BSSID == null : 
                           a.BSSID.equals(b.BSSID));
      boolean sameSSID = (a.SSID == null ? 
                          b.SSID == null : 
                          a.SSID.equals(b.SSID));
      boolean sameCapabilities = (a.capabilities == null ? 
                                  b.capabilities == null : 
                                  a.capabilities.equals(b.capabilities));
      boolean sameFrequency = (a.frequency == b.frequency);
      boolean sameTimestamp = (a.timestamp == b.timestamp);
      boolean sameLevel = (a.level == b.level);

      return sameBSSID && sameSSID && sameCapabilities &&
             sameFrequency && sameLevel && sameTimestamp;
    }
  }

  /**
   * Return a copy of the PeerNetwork, referring to the same network locations.
   *
   * @return A deep copy of the PeerNetwork.
   */
  public PeerNetwork clone() {
    PeerNetwork clone = new HotspotPeerNetwork(scanResult);

    return clone;
  }

  public String toString() {
    if (scanResult == null) {
      return "<no hotspot device>";
    }
    return scanResult.SSID;
  }

  /**
   * Returns null, since this Peer Network is backed by a ScanResult.
   *
   * @return Null.
   */
  public WifiP2pDevice getWifiP2pDevice() {
    return null;
  }

  /**
   * Return the ScanResult backing this peer network, if any.
   *
   * @return This peer network's ScanResult.
   */
  public ScanResult getScanResult() {
    return scanResult;
  }

  /**
   * Returns null, since this Peer Network is backed by a WifiP2pDevice.
   *
   * @return Null.
   */
  public BluetoothDevice getBluetoothLEDevice() {
    return null;
  }

  /**
   * Returns null, since this Peer Network is backed by a BluetoothDevice.
   *
   * @return Null
   */
  public BluetoothDevice getBluetoothDevice() {
    return null;
  }

  /**
   * Return a constant indicating that this peer network is of type Hotspot.
   *
   * @return The constant PeerNetwork.HOTSPOT_TYPE.
   */
  public int getNetworkType() {
    return PeerNetwork.HOTSPOT_TYPE;
  }

  /**
   * Overrides .equals().
   *
   * @param other Another object to compare this PeerNetwork to.
   */
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    } else if (!(other instanceof PeerNetwork)){
      return false;
    } else if (other.getClass() != this.getClass()) {
      return false;
    } else if (((PeerNetwork) other).getScanResult() == null) {
      return this.getScanResult() == null;
    } 
    return ((PeerNetwork) other).getScanResult().equals(this.getScanResult());
  }

  /**
   * Return a hash code unique-ish to this object.
   *
   * @return An integer hash code.
   */
  @Override
  public int hashCode() {
    // The only thing distinct about a HotspotPeerNetwork is its underlying
    // ScanResult.
    if (scanResult == null) {
      // TODO(lerner): Return something more reasonable here.
      return 0;
    }
    return scanResult.hashCode();
  }
}
