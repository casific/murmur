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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.ScanResult;
import android.bluetooth.BluetoothDevice;

import java.util.Arrays;

/**
 * This class represents the network connectivity over Bluetooth Low Energy
 * that can be used to reach a certain peer.
 */
public class BluetoothLEPeerNetwork implements PeerNetwork {
  /** 
   * A BluetoothDevice (remote Bluetooth device) associated with this Peer Network,
   * and used for low energy BT communication.
   */
  BluetoothDevice mBluetoothLEDevice;

  /**
   * Create a BluetoothLEPeerNetwork with no remote network devices to talk to.
   */
  public BluetoothLEPeerNetwork() { }

  /**
   * Create a BluetoothLEPeerNetwork with a remote BluetoothDevice to talk to .
   */
  public BluetoothLEPeerNetwork(BluetoothDevice bluetoothDevice) {
    this.mBluetoothLEDevice = bluetoothDevice;
  }

  /**
   * Unimplemented.
   *
   * TODO(lerner): Send a message. Returns immediately, sending the message asynchronously
   * as it is possible to do so given the constraints of the network media
   * (peer comes and goes, need to connect first, etc.)
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
    PeerNetwork clone = new BluetoothLEPeerNetwork(mBluetoothLEDevice);
    return clone;
  }

  public String toString() {
    if (mBluetoothLEDevice == null) {
      return "<no bluetooth device>";
    }
    return mBluetoothLEDevice.getAddress();
  }

  /**
   * Returns null, since this Peer Network is backed by a BluetoothDevice.
   *
   * @return Null.
   */
  public WifiP2pDevice getWifiP2pDevice() {
    return null;
  }

  /**
   * Returns null, since this Peer Network is backed by a BluetoothDevice.
   *
   * @return Null.
   */
  public ScanResult getScanResult() {
    return null;
  }

  /**
   * Return the backing BluetoothDevice of this peer network.
   *
   * @return The BluetoothDevice backing this peer network, or null if none
   * exists.
   */
  public BluetoothDevice getBluetoothLEDevice() {
    return mBluetoothLEDevice;
  }

  /**
   * Returns null, since this Peer Network is low energy, not normal Bluetooth.
   *
   * @return Null.
   */
  public BluetoothDevice getBluetoothDevice() {
    return null;
  }

  /**
   * Return a constant indicating that this peer network is of type
   * Bluetooth Low Energy.
   *
   * @return The constant PeerNetwork.BLUETOOTH_LOW_ENERGY_TYPE
   */
  public int getNetworkType() {
    return PeerNetwork.BLUETOOTH_LOW_ENERGY_TYPE;
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
    } else if (((PeerNetwork) other).getBluetoothLEDevice() == null) {
      return this.getBluetoothLEDevice() == null;
    } 
    return ((PeerNetwork) other).getBluetoothLEDevice().equals(this.getBluetoothLEDevice());
  }

  /**
   * Return a hash code unique-ish to this object.
   *
   * @return An integer hash code.
   */
  @Override
  public int hashCode() {
    // The only thing distinct about a BluetoothLEPeerNetwork is its underlying
    // BluetoothLEDevice.
    if (mBluetoothLEDevice == null) {
      // TODO(lerner): Return something more reasonable here.
      return 0;
    }
    return mBluetoothLEDevice.hashCode();
  }

}
