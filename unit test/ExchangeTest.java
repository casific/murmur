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

import okio.ByteString;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * Tests for the Exchange class.
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18,
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class ExchangeTest {

  /** Exchange under test. */
  private Exchange exchange;

  /** Output stream passed to exchange under test. */
  private PipedOutputStream outputStream;
  /** Input stream passed to exchange under test. */
  private PipedInputStream inputStream;

  /** Attached to the output stream passed to the exchange so we can talk to it. */
  private PipedOutputStream testOutputStream;
  /** Attached to the input stream passed to the exchange so we can hear it. */
  private PipedInputStream testInputStream;

  /** A message store to pass to the exchange. */
  private MessageStore messageStore;
  /** A friend store to pass to the exchange. */
  private FriendStore friendStore;

  /** Number of friends to include. */
  private static final int NUM_FRIENDS = 30;

  /** A list of 0 friends to send. */
  private CleartextFriends nullFriends = new CleartextFriends.Builder()
                                                             .friends(new ArrayList<String>())
                                                             .build();
  /** A list of 0 messages to send. */
  private CleartextMessages nullMessages = 
    new CleartextMessages.Builder()
                         .messages(new ArrayList<RangzenMessage>())
                         .build();

  /** A callback to provide to the Exchange under test. */
  private ExchangeCallback callback ;

  /** Runs before each test. */
  @Before
  public void setUp() throws IOException {
    outputStream = new PipedOutputStream();
    inputStream = new PipedInputStream();
    testOutputStream = new PipedOutputStream();
    testInputStream = new PipedInputStream();

    // We'll hear what the exchange says via testInputStream,
    // and we can send data to it on testOutputStream.
    testOutputStream.connect(inputStream);
    outputStream.connect(testInputStream);

    SlidingPageIndicator context = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();

    messageStore = new MessageStore(context, StorageBase.ENCRYPTION_DEFAULT); 
    friendStore = new FriendStore(context, StorageBase.ENCRYPTION_DEFAULT); 


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
   * Testing utility method that starts a new exchange.
   *
   * @param asInitiator Tell the exchange whether or not to speak first.
   */
  private void startExchange(boolean asInitiator) {
    exchange = new Exchange(inputStream,
                            outputStream,
                            asInitiator,           // Tell Exchange it's speaking first.
                            friendStore,
                            messageStore,
                            callback);

    // Start the exchange.
    (new Thread(exchange)).start();
  }

  /** 
   * Utility method to read a CleartextFriends from testInputStream. Unfortunately,
   * this method blocks if there's no data to the input stream.
   * 
   * @return A CleartextFriends object read from testInputStream.
   */
  private CleartextFriends readCleartextFriends() {
    return Exchange.lengthValueRead(testInputStream, CleartextFriends.class);
  }
  /** 
   * Utility method to read a CleartextMessages from testInputStream. 
   * Unfortunately, this method blocks if there's no data to the input stream.
   *
   * @return A CleartextFriends object read from testInputStream.
   */
  private CleartextMessages readCleartextMessages() {
    return Exchange.lengthValueRead(testInputStream, CleartextMessages.class);
  }

  /**
   * Test when the exchange is told it speaks first that it sends the right
   * things when the lists of things are empty (regression, empty lists broke 
   * things at one time).
   */
  @Test(timeout=2000)
  public void asInitiatorEmptyLists() throws IOException {
    // friendStore is empty, messageStore is empty.
    assertTrue(friendStore.getAllFriends().isEmpty());
    assertNull(messageStore.getKthMessage(0));

    // TODO(lerner): Figure out how to send these friends after receiving
    // so we can prove that the Exchange is transmitting first.
   
    // Send some friends
    Exchange.lengthValueWrite(testOutputStream, nullFriends);
    // Send some messages
    Exchange.lengthValueWrite(testOutputStream, nullMessages);
     
    startExchange(true);
    Robolectric.runBackgroundTasks();

    // // Receive friends (should be an empty list).
    CleartextFriends friendsReceived = readCleartextFriends();
    assertEquals(0, friendsReceived.friends.size());
    // Receive messages (should be an empty list).
    CleartextMessages messagesReceived = readCleartextMessages();
    assertEquals(0, messagesReceived.messages.size());

  }

  /**
   * Test when the exchange is told it speaks second that it sends the right
   * things when the lists of things are empty (regression, empty lists broke 
   * things at one time).
   */
  @Test(timeout=2000)
  public void notAsInitiatorEmptyLists() throws IOException {
    // friendStore is empty, messageStore is empty.
    assertTrue(friendStore.getAllFriends().isEmpty());
    assertNull(messageStore.getKthMessage(0));

    // Send some friends
    assertTrue(Exchange.lengthValueWrite(testOutputStream, nullFriends));
    // Send some messages
    assertTrue(Exchange.lengthValueWrite(testOutputStream, nullMessages));

    startExchange(false);
    Robolectric.runBackgroundTasks();
  
    // Receive friends (should be an empty list).
    CleartextFriends friendsReceived = readCleartextFriends();
    assertNotNull(friendsReceived);
    assertEquals(0, friendsReceived.friends.size());

    // Receive messages (should be an empty list).
    CleartextMessages messagesReceived = readCleartextMessages();
    assertNotNull(messagesReceived);
    assertEquals(0, messagesReceived.messages.size());
  }

  /**
   * Test when the exchange is told it speaks second that it sends the right
   * things.
   */
  @Test(timeout=2000)
  public void notAsInitiatorWithFriends() throws IOException {
    for (int i=0; i<NUM_FRIENDS; i++) {
      friendStore.addFriendBytes(("FRIEND" + i).getBytes());
    }

    // Send no friends
    Exchange.lengthValueWrite(testOutputStream, nullFriends);
    // Send no messages
    Exchange.lengthValueWrite(testOutputStream, nullMessages);

    startExchange(false);
    Robolectric.runBackgroundTasks();

    // Receive friends (should be the friends added above)
    CleartextFriends friendsReceived = readCleartextFriends();
    assertNotNull(friendsReceived);
    assertEquals(NUM_FRIENDS, friendsReceived.friends.size());
    assertEquals(friendStore.getAllFriends(), new HashSet<String>(friendsReceived.friends));
    // Receive messages (should be an empty list).
    CleartextMessages messagesReceived = readCleartextMessages();
    assertEquals(0, messagesReceived.messages.size());
  }

  /**
   * Test the static utility method that grabs the first four bytes from an
   * input stream and returns their value as an int.
   */
  @Test
  public void testPopLength() throws IOException {
    int testValue = 42;
    PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream outputStream = new PipedOutputStream();
    outputStream.connect(inputStream);
    
    ByteBuffer b = ByteBuffer.allocate(4);
    b.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    b.putInt(testValue);
    outputStream.write(b.array());
    assertEquals(testValue, Exchange.popLength(inputStream));
  }

  /**
   * Test that the method Exchange.newPriority computes priorities
   * as we expect it should.
   */
  @Test
  public void newPriorityTest() {
    double TEST_REMOTE_PRIORITY = 0.8;
    double TEST_LOCAL_PRIORITY = 1.0;
    int COMMON_FRIENDS = 5;
    int LOCAL_FRIENDS = 10;

    // Test that newPriority uses the local priority if it's higher than the
    // value calculated from the remote priority.
    assertEquals(TEST_LOCAL_PRIORITY, Exchange.newPriority(TEST_REMOTE_PRIORITY,
                                                           TEST_LOCAL_PRIORITY,
                                                           COMMON_FRIENDS,
                                                           LOCAL_FRIENDS), 0.001);

    double TEST_PRIORITY = 1.0;
    int NUM_FRIENDS = 10;

    // Check that more friends yields more priority for unknown messages (-2.0 priority).
    // More complicated for known messages due to the above check about using the
    // max of the computed priority and the local priority.
    for (int inCommon=0; inCommon<NUM_FRIENDS; inCommon++) {
      assertTrue(Exchange.newPriority(TEST_PRIORITY, MessageStore.NOT_FOUND, inCommon, NUM_FRIENDS) <
                 Exchange.newPriority(TEST_PRIORITY, MessageStore.NOT_FOUND, inCommon + 1, NUM_FRIENDS));
    }
    
                                                                        

  }

  /**
   * Test that the method Exchange.computeNewPriority_fractionOfFriends computes priorities
   * as we expect it should.
   */
  @Test
  public void fractionOfFriendsPriorityTest() {

    double TEST_PRIORITY = 1.0;
    int NUM_FRIENDS = 10;

    // All friends in common = unchanged priority.
    assertEquals(1.0, Exchange.fractionOfFriendsPriority(1.0, 10, 10), 0.0001); 

    // No friends still has positive priority.
    assertTrue(0 < Exchange.fractionOfFriendsPriority(0.0005, 0, 0));

    // Check that more friends always yields higher priority.
    for (int inCommon=0; inCommon<NUM_FRIENDS; inCommon++) {
      assertTrue(Exchange.fractionOfFriendsPriority(TEST_PRIORITY, inCommon, NUM_FRIENDS) <
                 Exchange.fractionOfFriendsPriority(TEST_PRIORITY, inCommon + 1, NUM_FRIENDS));
    }
  }

  @Test
  public void byteStringEqualityTest() {
    ByteString a = ByteString.of(new byte[] { 'a', 'b', 'c' });
    ByteString b = ByteString.of(new byte[] { 'a', 'b', 'c' });
    ByteString z = ByteString.of(new byte[] { 'x', 'y', 'z' });

    assertEquals(a, b);
    assertNotEquals(a, z);
    assertNotNull(ByteString.of(new byte[] {}));
  }
}
