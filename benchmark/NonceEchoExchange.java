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

import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * Performs a single exchange with a Rangzen peer.
 * This class is given input and output streams and communicates over them,
 * oblivious to the underlying network communications used.
 */
public class NonceEchoExchange extends Exchange {

  /** Included with Android log messages. */
  private static final String TAG = "NonceEchoExchange";

  /**
   * Pass-through constructor to super-class Exchange.
   *
   * @see org.denovogroup.rangzen.Exchange
   */
  public NonceEchoExchange(InputStream in, OutputStream out, boolean asInitiator, 
                            FriendStore friendStore, MessageStore messageStore, 
                            ExchangeCallback callback) throws IllegalArgumentException {
    super(in, out, asInitiator, friendStore, messageStore, callback);
  }

  @Override
  public void run() {
    if (asInitiator) {
      int r = (new Random()).nextInt();
      sendNonce(r);
      try {
        int remoteR = receiveNonce();
        if (remoteR == r) {
          setExchangeStatus(Status.SUCCESS);
        } else {
          setExchangeStatus(Status.ERROR);
          setErrorMessage("Got wrong number back");
        }
      } catch (IOException e) {
        setErrorMessage("Failed to receive number from remote party");
        setExchangeStatus(Status.ERROR);
      }
    } else {
      try {
        int r = receiveNonce();
        sendNonce(r);
      } catch (IOException e) {
        setErrorMessage("Error receiving nonce");
        setExchangeStatus(Status.ERROR);
      }
      setExchangeStatus(Status.SUCCESS);
    }

    // We're done with the mechanics of the exchange - if there's a callback
    // to report to, call its .success() or .failure() method as appropriate.
    if (callback == null) {
      Log.w(TAG, "No callback provided to exchange.");
      return;
    }
    if (getExchangeStatus() == Status.SUCCESS) {
      callback.success(this);
      return;

    } else {
      callback.failure(this, mErrorMessage);
      return;
    }
  }

  /** 
   * Send an integer to the communication partner.
   *
   * @param r The integer to send.
   */
  private void sendNonce(int r) {
    Nonce nonceMessage = new Nonce.Builder()
                                  .nonce(r)
                                  .build();
    lengthValueWrite(out, nonceMessage);
  }

  /** 
   * Receive an integer from the communication partner.
   *
   * @return The number received.
   */
  private int receiveNonce() throws IOException {
    Nonce nonceMessage = lengthValueRead(in, Nonce.class);
    if (nonceMessage != null) {
      return nonceMessage.nonce;
    } else {
      throw new IOException("Didn't get number from remote party.");
    }
  }

}
