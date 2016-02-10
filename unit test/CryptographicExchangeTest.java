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

import okio.ByteString;


/**
 * Tests for the CryptographicExchange class.
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18,
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class CryptographicExchangeTest {
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
  private Exchange exchangeA;
  /** The other Exchange under test. */
  private Exchange exchangeB;

  private static byte[] TEST_FRIEND_1; 
  private static byte[] TEST_FRIEND_2; 
  private static byte[] TEST_FRIEND_3; 
  private static byte[] TEST_FRIEND_4; 
  private static byte[] TEST_FRIEND_5; 
  private static byte[] TEST_FRIEND_6; 
  private static final String TEST_MESSAGE_1 = "TEST_MESSAGE_1";
  private static final String TEST_MESSAGE_2 = "TEST_MESSAGE_2";
  private static final String TEST_MESSAGE_3 = "TEST_MESSAGE_3";
  private static final double TEST_PRIORITY_1 = 0.4f;
  private static final double TEST_PRIORITY_2 = 0.5f;
  private static final double TEST_PRIORITY_3 = 1.0f;

  /** Runs before each test. */
  @Before
  public void setUp() throws IOException {
    // Test friends have to be real base64 encoded strings, otherwise we get
    // weird behavior. This needs to be fixed in the system.
    TEST_FRIEND_1 = "TESTFRIEND1".getBytes("UTF-8");
    TEST_FRIEND_2 = "TESTFRIEND2".getBytes("UTF-8");
    TEST_FRIEND_3 = "TESTFRIEND3".getBytes("UTF-8");
    TEST_FRIEND_4 = "TESTFRIEND4".getBytes("UTF-8");
    TEST_FRIEND_5 = "TESTFRIEND5".getBytes("UTF-8");
    TEST_FRIEND_6 = "TESTFRIEND6".getBytes("UTF-8");

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
   * Test when the exchange is told it speaks first that it sends the right
   * things when the lists of things are empty (regression, empty lists broke 
   * things at one time).
   */
  @Test(timeout=5000)
  public void emptyListsTest() throws IOException, InterruptedException {
    // This test involves empty friend/message lists.
    assertTrue(friendStoreA.getAllFriends().isEmpty());
    assertNull(messageStoreA.getKthMessage(0));
    assertTrue(friendStoreB.getAllFriends().isEmpty());
    assertNull(messageStoreB.getKthMessage(0));

    performExchange();

    assertEquals(0, exchangeA.getCommonFriends());
    assertEquals(0, exchangeB.getCommonFriends());
    assertEquals(0, exchangeA.getReceivedMessages().size());
    assertEquals(0, exchangeB.getReceivedMessages().size());
  }

  /**
   * Tests that the count of messages received is equal to the number in the message
   * of the sender.
   */
  @Test(timeout=5000)
  public void emptyFriendsSomeMessagesTest() throws IOException, InterruptedException {
    // This test involves empty friend/message lists.
    assertTrue(friendStoreA.getAllFriends().isEmpty());
    assertNull(messageStoreA.getKthMessage(0));
    assertTrue(friendStoreB.getAllFriends().isEmpty());
    assertNull(messageStoreB.getKthMessage(0));

    // 3 messeages in A's MessageStore, 0 in B's.
    messageStoreA.addMessage(TEST_MESSAGE_1, TEST_PRIORITY_1);
    messageStoreA.addMessage(TEST_MESSAGE_2, TEST_PRIORITY_2);
    messageStoreA.addMessage(TEST_MESSAGE_3, TEST_PRIORITY_3);

    assertEquals(3, messageStoreA.getMessageCount());
    assertEquals(0, messageStoreB.getMessageCount());

    performExchange();

    // Friend lists were empty, so no common friends.
    assertEquals(0, exchangeA.getCommonFriends());
    assertEquals(0, exchangeB.getCommonFriends());

    // Each side receives the number of messages in the other side's store.
    assertEquals(messageStoreB.getMessageCount(), exchangeA.getReceivedMessages().size());
    assertEquals(messageStoreA.getMessageCount(), exchangeB.getReceivedMessages().size());
  }

  /**
   * One exchange has 0 friends, the other exchange has some friends.
   * Tests that their friend overlap is 0 in both directions.
   */
  @Test(timeout=5000)
  public void onePartyHasNoFriendsTest() throws IOException, InterruptedException {
    // ExchangeB has 3 friends, 
    friendStoreB.addFriendBytes(TEST_FRIEND_1);
    friendStoreB.addFriendBytes(TEST_FRIEND_2);
    friendStoreB.addFriendBytes(TEST_FRIEND_3);

    assertEquals(0, friendStoreA.getAllFriends().size());
    assertEquals(3, friendStoreB.getAllFriends().size());

    performExchange();

    // 1 overlapping friend was added to each.
    assertEquals(0, exchangeA.getCommonFriends());
    assertEquals(0, exchangeB.getCommonFriends());
  }

  /**
   * Both sides have the same friends.
   * Tests that their friend overlap is equal to the number of friends they have.
   */
  @Test(timeout=5000)
  public void partiesHaveSameFriendsTest() throws IOException, InterruptedException {
    friendStoreA.addFriendBytes(TEST_FRIEND_1);
    friendStoreA.addFriendBytes(TEST_FRIEND_2);
    friendStoreA.addFriendBytes(TEST_FRIEND_3);
    friendStoreB.addFriendBytes(TEST_FRIEND_1);
    friendStoreB.addFriendBytes(TEST_FRIEND_2);
    friendStoreB.addFriendBytes(TEST_FRIEND_3);

    assertEquals(3, friendStoreA.getAllFriends().size());
    assertEquals(3, friendStoreB.getAllFriends().size());

    performExchange();

    assertEquals(3, exchangeA.getCommonFriends());
    assertEquals(3, exchangeB.getCommonFriends());
  }

  /**
   * Both sides have friends, but none of them overlap.
   * Tests that their friend overlap is 0 in both directions.
   */
  @Test(timeout=5000)
  public void bothHaveFriendsNoOverlapTest() throws IOException, InterruptedException {
    // Add 3 non-overlapping friends to each side.
    friendStoreA.addFriendBytes(TEST_FRIEND_1);
    friendStoreA.addFriendBytes(TEST_FRIEND_2);
    friendStoreA.addFriendBytes(TEST_FRIEND_3);
    friendStoreB.addFriendBytes(TEST_FRIEND_4);
    friendStoreB.addFriendBytes(TEST_FRIEND_5);
    friendStoreB.addFriendBytes(TEST_FRIEND_6);

    assertEquals(3, friendStoreA.getAllFriends().size());
    assertEquals(3, friendStoreB.getAllFriends().size());

    performExchange();

    assertEquals(0, exchangeA.getCommonFriends());
    assertEquals(0, exchangeB.getCommonFriends());
  }

  /**
   * One side's friends are a strict subset of the other side's friends.
   */
  @Test(timeout=5000)
  public void oneSideFriendsSubsetOtherTest() throws IOException, InterruptedException {
    // Add 3 non-overlapping friends to each side.
    friendStoreA.addFriendBytes(TEST_FRIEND_1);
    friendStoreA.addFriendBytes(TEST_FRIEND_2);
    friendStoreA.addFriendBytes(TEST_FRIEND_3);
    friendStoreB.addFriendBytes(TEST_FRIEND_1);

    assertEquals(3, friendStoreA.getAllFriends().size());
    assertEquals(1, friendStoreB.getAllFriends().size());

    performExchange();

    assertEquals(1, exchangeA.getCommonFriends());
    assertEquals(1, exchangeB.getCommonFriends());
  }

  /**
   * Non-zero intersection between friends without either side being a subset of the other.
   */
  @Test(timeout=5000)
  public void overlappingFriendsTest() throws IOException, InterruptedException {
    // Add 3 non-overlapping friends to each side.
    friendStoreA.addFriendBytes(TEST_FRIEND_1);
    friendStoreA.addFriendBytes(TEST_FRIEND_2);
    friendStoreA.addFriendBytes(TEST_FRIEND_3);
    friendStoreB.addFriendBytes(TEST_FRIEND_1);
    friendStoreB.addFriendBytes(TEST_FRIEND_3);
    friendStoreB.addFriendBytes(TEST_FRIEND_5);
    friendStoreB.addFriendBytes(TEST_FRIEND_6);

    assertEquals(3, friendStoreA.getAllFriends().size());
    assertEquals(4, friendStoreB.getAllFriends().size());

    performExchange();

    // 1 and 3 are in common.
    assertEquals(2, exchangeA.getCommonFriends());
    assertEquals(2, exchangeB.getCommonFriends());
  }

  /**
   * Utility method that creates two exchanges, starts them in threads, joins the
   * threads and returns when all that is done.
   */
  private void performExchange() throws InterruptedException {
    exchangeA = createExchange(true, inputStreamA, outputStreamA, friendStoreA, messageStoreA);
    exchangeB = createExchange(true, inputStreamB, outputStreamB, friendStoreB, messageStoreB);

    // Start the exchange.
    Thread threadA = new Thread(exchangeA);
    Thread threadB = new Thread(exchangeB);

    threadA.start();
    threadB.start(); 

    threadA.join();
    threadB.join();

    assertEquals(Exchange.Status.SUCCESS, exchangeA.getExchangeStatus());
    assertEquals(Exchange.Status.SUCCESS, exchangeB.getExchangeStatus());
  }

  /**
   * Testing utility method that starts a new exchange.
   *
   * @param asInitiator Tell the exchange whether or not to speak first.
   */
  private CryptographicExchange createExchange(boolean asInitiator, 
                                   InputStream inputStream, OutputStream outputStream,
                                   FriendStore friendStore, MessageStore messageStore) {


    CryptographicExchange exchange = new CryptographicExchange(inputStream,
                                         outputStream,
                                         asInitiator, 
                                         friendStore,
                                         messageStore,
                                         callback);
    return exchange;
  }

  /**
   * A mock of FriendStore allowing us to easily pass two separate message stores
   * to separate exchanges for testing.
   */
  private class MockFriendStore extends FriendStore {
    /** Mock storage of friends. */
    private HashSet<String> mockFriends = new HashSet<String>();

    /**
     * Pass-through constructor.
     */
    public MockFriendStore(Context context, int encryptionMode) { 
      super(context, encryptionMode);
    }

    @Override
    public boolean addFriendBytes(byte[] friend) {
      mockFriends.add(FriendStore.bytesToBase64(friend));
      return true;
    }

    @Override
    public Set<String> getAllFriends() {
      return mockFriends;
    }
  }

  /**
   * A mock of MessageStore allowing us to easily pass two separate message stores
   * to separate exchanges for testing.
   *
   * It doesn't test for duplicate messages.
   */
  private class MockMessageStore extends MessageStore {
    /** Mock storage of messages, without regard to priority. */
    private ArrayList<Message> mockMessages = new ArrayList<Message>();

    /** Check how many times getKthMessage was called. */
    public int kthCalls = 0;

    /**
     * Pass-through constructor.
     */
    public MockMessageStore(Context context, int encryptionMode) { 
      super(context, encryptionMode);
    }

    @Override
    public boolean addMessage(String message, double priority) {
      mockMessages.add(new Message(priority, message));
      return true;
    }

    @Override
    public int getMessageCount() {
      return mockMessages.size();  
    }

    @Override
    public Message getKthMessage(int k) {
      kthCalls++;
      try {
        return mockMessages.get(k);
      } catch (IndexOutOfBoundsException e) {
        return null;
      }
    }
  }



  // TODO(lerner): Remove all methods below when they turn out not to be needed.
  // /** 
  //  * Utility method to read a ServerMessage from testInputStream. Unfortunately,
  //  * this method blocks if there's no data to the input stream.
  //  * 
  //  * @return A ServerMessage object read from testInputStream.
  //  */
  // private ServerMessage readServerMessage() {
  //   return Exchange.lengthValueRead(testInputStream, ServerMessage.class);
  // }

  // /** 
  //  * Utility method to read a ClientMessage from testInputStream. Unfortunately,
  //  * this method blocks if there's no data to the input stream.
  //  * 
  //  * @return A ClientMessage object read from testInputStream.
  //  */
  // private ClientMessage readClientMessage() {
  //   return Exchange.lengthValueRead(testInputStream, ClientMessage.class);
  // }
  
}

