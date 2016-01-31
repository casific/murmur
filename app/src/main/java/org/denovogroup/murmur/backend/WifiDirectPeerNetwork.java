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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.ScanResult;
import android.bluetooth.BluetoothDevice;

/**
 * This class represents the network connectivity over Wifi Direct
 * that can be used to reach a certain peer.
 */
public class WifiDirectPeerNetwork implements PeerNetwork {
  /** A WifiP2pDevice (remote MAC address) associated with this Peer Network */
  WifiP2pDevice wifiP2pDevice;

  /**
   * Create a WifiDirectPeerNetwork with no remote network devices to talk to.
   */
  public WifiDirectPeerNetwork() { }

  /**
   * Create a WifiDirectPeerNetwork with a remote WifiP2pDevice to talk to .
   */
  public WifiDirectPeerNetwork(WifiP2pDevice wifiP2pDevice) {
    this.wifiP2pDevice = wifiP2pDevice;
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
   * Return a copy of the PeerNetwork, referring to the same network locations.
   *
   * @return A deep copy of the PeerNetwork.
   */
  public PeerNetwork clone() {
    PeerNetwork clone = new WifiDirectPeerNetwork(new WifiP2pDevice(this.wifiP2pDevice));

    return clone;
  }

  public String toString() {
    if (wifiP2pDevice == null) {
      return "<no wifi device>";
    }
    return wifiP2pDevice.deviceAddress;
  }

  /**
   * Return the WifiP2pDevice backing this peer network, if any.
   *
   * @return This peer network's Wifi Direct device.
   */
  public WifiP2pDevice getWifiP2pDevice() {
    return wifiP2pDevice;
  }

  /**
   * Returns null, since this Peer Network is backed by a WifiP2pDevice.
   *
   * @return Null.
   */
  public ScanResult getScanResult() {
    return null;
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
   * Returns null, since this Peer Network is backed by a WifiP2pDevice.
   *
   * @return Null.
   */
  public BluetoothDevice getBluetoothDevice() {
    return null;
  }

  /**
   * Return a constant indicating that this peer network is of type Wifi Direct.
   *
   * @return The constant PeerNetwork.WIFI_DIRECT_TYPE.
   */
  public int getNetworkType() {
    return PeerNetwork.WIFI_DIRECT_TYPE;
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
    } else if (!(other instanceof PeerNetwork)) {
      return false;
    } else if (other.getClass() != this.getClass()) {
      return false;
    } else if (((PeerNetwork) other).getWifiP2pDevice() == null) {
      return this.getWifiP2pDevice() == null;
    } 
    return ((PeerNetwork) other).getWifiP2pDevice().equals(this.getWifiP2pDevice());
  }

  /**
   * Return a hash code unique-ish to this object.
   *
   * @return An integer hash code.
   */
  @Override
  public int hashCode() {
    // The only thing distinct about a WifiDirectPeerNetwork is its underlying
    // WifiP2pDevice.
    if (wifiP2pDevice == null) {
      // TODO(lerner): Return something more reasonable here.
      return 0;
    }
    return wifiP2pDevice.hashCode();
  }
}
