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

import java.util.Date;

/**
 * Medium-independent representation of a remote peer.
 */
public class Peer {
    /** The bluetooth address of this peer */
    public String address;
  /** An object that encapsulates the network connection(s) to this peer */
  private PeerNetwork network;

  /** The datetime at which this peer was last seen over the network */
  private Date lastSeen;

  /**
   * Create a new Peer which might be reached over the given PeerNetwork.
   *
   * @param network The PeerNetwork object for this peer
   */
  public Peer(PeerNetwork network, String address) {
      this.address = address;
    this.network = network;
    lastSeen = new Date();
  }

  /**
   * Retrieve the PeerNetwork corresponding to this peer.
   *
   * @return The PeerNetwork object for this peer
   */
  public PeerNetwork getNetwork() {
    return network;
  }

  /**
   * Get the last time the peer was seen.
   *
   * @return A Time object representing the last time the peer was seen.
   */
  public Date getLastSeen() {
    return lastSeen;
  }

  /**
   * Set the time that this peer was last seen to now.
   */
  public void touch() {
    lastSeen = new Date();
  }

  /**
   * Set the time this peer was last seen to the given datetime.
   */
  public void touch(Date date) {
    // Record a copy since Dates are mutable.
    lastSeen = (Date) date.clone();
  }

  /**
   * Send a message to this peer.
   */
  public void send(String message) {
    network.send(message);
  }

  /**
   * Two peers are equal if they use the same network.
   *
   * @return True if the peer uses the same network connection.
   */
  public boolean equals(Peer p) {
    // Other instance is null, this is never null, so can't be equal.
    if (p == null) {
      return false;
    } else if (p.getNetwork() == null) {
      // If both PeerNetworks are null, then the instances are equal.
      return p.getNetwork() == network;
    } else {
      // If the PeerNetwork objects are equal, the Peers are equal. 
      return p.getNetwork().equals(network);
    }
  } 

  /**
   * Overrides .equals().
   *
   * @param other Another object to compare this Peer to.
   */
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    } else if (other.getClass() != this.getClass()) {
      return false;
    } else if (((Peer) other).getNetwork() == null) {
      return this.getNetwork() == null;
    } 
    return ((Peer) other).getNetwork().equals(this.getNetwork());
  }

  /**
   * Returns a hash code representing the object. Required for reimplementing
   * .equals(), and must return the same hash code iff two objects are equal.
   *
   * @return An integer hash code.
   */
  @Override
  public int hashCode() {
    // Since the only thing distinguishing peers is their PeerNetwork objects,
    // we simply report the hashcode of the underlying peer network object.
    if (this.getNetwork() == null) {
      // TODO(lerner): Return something more reasonable here.
      return 0;
    }
    return this.getNetwork().hashCode();
  }

  /**
   * Create a deep copy of the Peer which references the same remote peer 
   * but whose PeerNetwork is a distinct object.
   *
   * @return A deep copy of the peer.
   */
  public Peer clone() {
    Peer clone = new Peer(network.clone(), address);

    return clone;
  }

  public String toString() {
    return String.format("Peer (network: %s)", network.toString());
  }
}
