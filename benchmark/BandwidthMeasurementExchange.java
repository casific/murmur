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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import okio.ByteString;

import org.apache.commons.lang3.time.StopWatch;

import android.util.Log;


/**
 * Performs an exchange with a Rangzen peer to measure the available bandwidth
 * to that peer. Measures the time to send a large, random message and receive
 * back a checksum of that message.
 */
public class BandwidthMeasurementExchange extends Exchange {
  /** Included with Android log messages. */
  private static final String TAG = "BandwidthMeasurementExchange";

  /** Stopwatch for timing bandwidth usage. */
  private StopWatch stopwatch = new StopWatch();

  /**
   * Pass-through constructor to super-class Exchange.
   *
   * @see org.denovogroup.rangzen.Exchange
   */
  public BandwidthMeasurementExchange(InputStream in, OutputStream out, boolean asInitiator, 
                            FriendStore friendStore, MessageStore messageStore, 
                            ExchangeCallback callback) throws IllegalArgumentException {
    super(in, out, asInitiator, friendStore, messageStore, callback);
  }

  /** Amount of random data to send. */
  private static final int MESSAGE_SIZE = 1 * 512;

  @Override
  public void run() {
    if (asInitiator) {
      // We're the measurement node.
      // Generate random data to send.
      byte[] randomBytes = new byte[MESSAGE_SIZE];
      // (new Random()).nextBytes(randomBytes);
      // ByteString randomBytesString = ByteString.of(randomBytes);
      // BunchOfBytes message = new BunchOfBytes.Builder()
      //                                        .bunchOfBytes(randomBytesString)
      //                                        .build();     
      
      ByteString digest;
      try { 
        digest = digestBytes(randomBytes);
      } catch (NoSuchAlgorithmException e) {
        setErrorMessage("No digest algorithm!");
        setExchangeStatus(Status.ERROR);
        callback.failure(this, getErrorMessage());
        return;
      }

      // Start the timer and begin transmission.
      Log.i(TAG, "Sending random bytes");
      stopwatch.start();
      // lengthValueWrite(out, message);
      sendBytes(randomBytes);
      Log.i(TAG, "Sent random bytes, waiting for digest in reply.");


      // BunchOfBytes remoteDigest = lengthValueRead(in, BunchOfBytes.class);
      byte[] echo = receiveBytes();
      stopwatch.stop();
      Log.i(TAG, "Received echo in reply.");
      
      if (Arrays.equals(echo, randomBytes)) {
      // if (remoteDigest != null && digest.equals(remoteDigest.bunchOfBytes)) {
        setExchangeStatus(Status.SUCCESS);
        Log.i(TAG, String.format("Completed exchange of %d bytes in %d nanos (%f b/s).", 
                                 MESSAGE_SIZE, stopwatch.getNanoTime(), getBytesPerSecond()));
                                   

      } else {
        Log.i(TAG, "Echo didn't match!");
        setErrorMessage(String.format("Matching echo not received, %d vs %d", 
                                      randomBytes.length, echo.length));
        setExchangeStatus(Status.ERROR);
        callback.failure(this, getErrorMessage());
        return;
      }
      
    } else {
      // We're not the measurement node.
      // BunchOfBytes randomBytes = lengthValueRead(in, BunchOfBytes.class);
      byte[] randomBytes = receiveBytes();
      if (randomBytes != null) {
      // if (randomBytes.bunchOfBytes != null) {
        // ByteString digest;
        // try {
        //   digest = digestBytes(randomBytes.bunchOfBytes.toByteArray());
        // } catch (NoSuchAlgorithmException e) {
        //   setErrorMessage("No digest algorithm!");
        //   setExchangeStatus(Status.ERROR);
        //   callback.failure(this, getErrorMessage());
        //   return;
        // }
      
        // BunchOfBytes message = new BunchOfBytes.Builder()
        //                                        .bunchOfBytes(digest)
        //                                        .build();     
        // lengthValueWrite(out, message);
        sendBytes(randomBytes);
      } else {
        setErrorMessage("No random data received from measurement node.");
        setExchangeStatus(Status.ERROR);
        callback.failure(this, getErrorMessage());
        return;
      }
      setExchangeStatus(Status.SUCCESS);
    }
    callback.success(this);
  }

  /** The default hash algorithm to use. */
  private static final String HASH_ALGORITHM = "SHA-1";

  /**
   * Compute a digest of the given bytes and return a ByteString representing
   * that digest.
   */
  public static ByteString digestBytes(byte[] bytes) throws NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
      return ByteString.of(md.digest(bytes));
  }

  /**
   * Get the nanoseconds taken to send the data.
   */
  public long getStopwatchTimeNanos() {
    return stopwatch.getNanoTime();
  }

  /** Number of nanoseconds per second. */
  private static final long NANOS_PER_SECOND = 1000 * 1000 * 1000;
  
  /** 
   * Get the bandwidth measured.
   *
   * @return Bandwidth measured, in bytes/second.
   */
  public double getBytesPerSecond() {
    double seconds = stopwatch.getNanoTime() / ((double) NANOS_PER_SECOND);
    return ((double) MESSAGE_SIZE) / seconds;
  }

  private void sendBytes(byte[] payload) {
    try { 
      out.write(payload);
    } catch (IOException e) {
      Log.e(TAG, "Failed to send " +payload.length + " bytes: " + e);
    }
    Log.i(TAG, "Sent " + payload.length);
  }
  private byte[] receiveBytes() {
    byte[] received = new byte[MESSAGE_SIZE];
    try { 
      int recvCount = in.read(received);
      Log.i(TAG, "Received " + recvCount);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read! Returning empty array!");
    }
    return received;
  }
}
