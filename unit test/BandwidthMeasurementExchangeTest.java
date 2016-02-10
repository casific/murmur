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


import org.denovogroup.rangzen.Crypto;
import org.denovogroup.rangzen.Crypto.PrivateSetIntersection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for the BandwidthMeasurementExchange class.
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18,
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class BandwidthMeasurementExchangeTest {
  /** A message store to pass to the exchange. */
  private MockMessageStore messageStoreA;
  /** A friend store to pass to the exchange. */
  private FriendStore friendStoreA;

  /** A message store to pass to the exchange. */
  private MockMessageStore messageStoreB;
  /** A friend store to pass to the exchange. */
  private FriendStore friendStoreB;

  /** Input stream for one exchange. */
  private PipedInputStream inputStreamA;
  /** Output stream for one exchange. */
  private PipedOutputStream outputStreamA;
  /** Input stream for the other exchange. */
  private PipedInputStream inputStreamB;
  /** Output stream for the other exchange. */
  private PipedOutputStream outputStreamB;

  /** A callback to provide to the Exchange under test. */ 
  private ExchangeCallback callback;

  /** One Exchange under test. */
  private BandwidthMeasurementExchange exchangeA;
  /** The other Exchange under test. */
  private BandwidthMeasurementExchange exchangeB;
  /** Runs before each test. */

  @Before
  public void setUp() throws IOException {
    outputStreamA = new PipedOutputStream();
    inputStreamA = new PipedInputStream();
    outputStreamB = new PipedOutputStream();
    inputStreamB = new PipedInputStream();

    // We'll hear what the exchange says via testInputStream,
    // and we can send data to it on testOutputStream.
    outputStreamA.connect(inputStreamB);
    outputStreamB.connect(inputStreamA);

    SlidingPageIndicator context = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();

    messageStoreA = new MockMessageStore(context, StorageBase.ENCRYPTION_DEFAULT); 
    friendStoreA = new MockFriendStore(context, StorageBase.ENCRYPTION_DEFAULT); 
    messageStoreB = new MockMessageStore(context, StorageBase.ENCRYPTION_DEFAULT); 
    friendStoreB = new MockFriendStore(context, StorageBase.ENCRYPTION_DEFAULT); 

    callback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
      }

      @Override
      public void failure(Exchange exchange, String reason) {
      }
    };

    Robolectric.getBackgroundScheduler().pause();
    // Robolectric.getUiThreadScheduler().pause();
  }   

  /**
   * Utility method that creates two exchanges, starts them in threads, joins the
   * threads and returns when all that is done.
   */
  private void performExchange() throws InterruptedException {
    exchangeA = createExchange(true, inputStreamA, outputStreamA, friendStoreA, messageStoreA);
    exchangeB = createExchange(false, inputStreamB, outputStreamB, friendStoreB, messageStoreB);

    // Start the exchange.
    Thread threadA = new Thread(exchangeA);
    Thread threadB = new Thread(exchangeB);

    threadA.start();
    threadB.start(); 

    threadA.join();
    threadB.join();

    assertEquals(exchangeA.getErrorMessage(), Exchange.Status.SUCCESS, exchangeA.getExchangeStatus());
    assertEquals(exchangeB.getErrorMessage(), Exchange.Status.SUCCESS, exchangeB.getExchangeStatus());
  }

  /**
   * Create an exchange.
   *
   * @param asInitiator Tell the exchange whether or not to speak first.
   */
  private BandwidthMeasurementExchange createExchange(boolean asInitiator, 
                                   InputStream inputStream, OutputStream outputStream,
                                   FriendStore friendStore, MessageStore messageStore) {


    BandwidthMeasurementExchange exchange = new BandwidthMeasurementExchange(inputStream,
                                         outputStream,
                                         asInitiator, 
                                         friendStore,
                                         messageStore,
                                         callback);
    return exchange;
  }

  @Test(timeout=5000)
  public void bandwidthExchangeTest() throws IOException, InterruptedException {
    performExchange();

    assertEquals(2, exchangeA.getBytesPerSecond());
  }

}
