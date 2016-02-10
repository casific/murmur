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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
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

/**
 * Unit tests for Rangzen's MessageStore class
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../ui/Rangzen/res/")
@RunWith(RobolectricTestRunner.class)
public class MessageStoreTest {
  /** The instance of MessageStore we're using for tests. */
  private MessageStore store;

  /** The app instance we're using to pass to MessageStore. */
  private SlidingPageIndicator activity;

  /** Test strings we're writing into the storage system. */
  private static final String TEST_MSG_1 = "message 1";
  private static final String TEST_MSG_2 = "message 2";
  private static final String TEST_MSG_3 = "message 3";

  private static final String TEST_MSG_INVALID = "invalid";

  private static final double TEST_PRIORITY_1 = 1.0;
  private static final double TEST_PRIORITY_2 = 0.2;
  private static final double TEST_PRIORITY_3 = 0.9;
  private static final double TEST_PRIORITY_4 = 0.8;
  private static final double TEST_PRIORITY_5 = 0.7;

  private static final double TEST_PRIORITY_INVALID = 1.1;

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();
    store = new MessageStore(activity, StorageBase.ENCRYPTION_NONE);
  }

  /**
   * Tests that we can store a message and retrieve its priority.
   */
  @Test
  public void storeMessageGetPriority() {
    assertEquals(store.getMessagePriority(TEST_MSG_1, -1.0), -1.0, 0.1);
    store.addMessage(TEST_MSG_1, TEST_PRIORITY_1);
    assertEquals(store.getMessagePriority(TEST_MSG_1, -1.0), TEST_PRIORITY_1, 0.1);
  }

  /**
   * Tests that we can store messages and get them in order.
   */
  @Test
  public void storeMessages() {
    store.addMessage(TEST_MSG_1, TEST_PRIORITY_1);
    store.addMessage(TEST_MSG_2, TEST_PRIORITY_2);
    store.addMessage(TEST_MSG_3, TEST_PRIORITY_3);

    TreeMap<Double, Collection<String>> topk = store.getTopK(0);
    assertEquals(topk.size(), 0);

    topk = store.getTopK(1);
    assertEquals(1, flattenTopK(topk).size());
    assertTrue(flattenTopK(topk).contains(TEST_MSG_1));
    assertFalse(flattenTopK(topk).contains(TEST_MSG_2));
    assertFalse(flattenTopK(topk).contains(TEST_MSG_3));
    assertEquals((Double) topk.lastKey(), TEST_PRIORITY_1, 0.01);

    topk = store.getTopK(2);
    assertEquals(2, flattenTopK(topk).size());
    assertTrue(flattenTopK(topk).contains(TEST_MSG_1));
    assertFalse(flattenTopK(topk).contains(TEST_MSG_2));
    assertTrue(flattenTopK(topk).contains(TEST_MSG_3));
    assertEquals((Double) topk.lastKey(), TEST_PRIORITY_1, 0.01);
    assertEquals((Double) topk.lowerKey(topk.lastKey()), TEST_PRIORITY_3, 0.01);

    topk = store.getTopK(3);
    assertEquals(3, flattenTopK(topk).size());
    assertTrue(flattenTopK(topk).contains(TEST_MSG_1));
    assertTrue(flattenTopK(topk).contains(TEST_MSG_2));
    assertTrue(flattenTopK(topk).contains(TEST_MSG_3));
    assertEquals((Double) topk.lastKey(), TEST_PRIORITY_1, 0.01);
    assertEquals((Double) topk.lowerKey(topk.lastKey()), TEST_PRIORITY_3, 0.01);
    assertEquals((Double) topk.lowerKey(topk.lowerKey(topk.lastKey())), TEST_PRIORITY_2, 0.01);

    topk = store.getTopK(4);
    assertEquals(topk.size(), 3);
  }

  /**
   * Utility method for storeMessages test that flattens a topk set into a set
   * of messages.
   */
  private Set<String> flattenTopK(TreeMap<Double, Collection<String>> topk) {
    HashSet<String> topkMessages = new HashSet<String>();
    for (Collection<String> messages : topk.values()) {
      for (String m : messages) {
        topkMessages.add(m);
      }
    }
    return topkMessages;
  }

  /**
   * Regression test for the bug where getTopK messages only returned one message
   * per unique priority score. 
   */
  @Test
  public void regressionGetTopKPriorityScoreTest() {
    store.addMessage("Test1", TEST_PRIORITY_1);
    store.addMessage("Test2", TEST_PRIORITY_1);
    store.addMessage("Test3", TEST_PRIORITY_1);
    store.addMessage("Test4", TEST_PRIORITY_2);
    store.addMessage("Test5", TEST_PRIORITY_3);
    store.addMessage("Test6", TEST_PRIORITY_4);
    store.addMessage("Test7s", TEST_PRIORITY_4);
    store.addMessage("Test8", TEST_PRIORITY_4);
    store.addMessage("test9", TEST_PRIORITY_4);
    store.addMessage("Test10", TEST_PRIORITY_4);
    store.addMessage("Test11", TEST_PRIORITY_5);
    store.addMessage("Test12", TEST_PRIORITY_5);

    // One collection of messages per distinct priority value (there are 5 above).
    assertEquals(5, store.getTopK(12).size());
    int messagesReturned = 0;
    TreeMap<Double, Collection<String>> topk = store.getTopK(12);
    for (Collection<String> messages : topk.values()) {
      for (String m : messages) {
        messagesReturned++;
      }
    }
    // One message per message we inserted.
    assertEquals(12, messagesReturned);
  }

  /**
   * Regression test for the bug where getTopK returns too many messages because
   * it was stopping based on #bins instead of #messages.
   */
  @Test
  public void regressionGetTopKReturnsTooManyMessages() {
    store.addMessage("Test1", TEST_PRIORITY_1);
    store.addMessage("Test2", TEST_PRIORITY_1);
    store.addMessage("Test3", TEST_PRIORITY_1);
    store.addMessage("Test4", TEST_PRIORITY_2);
    store.addMessage("Test5", TEST_PRIORITY_3);
    store.addMessage("Test6", TEST_PRIORITY_4);
    store.addMessage("Test7s", TEST_PRIORITY_4);
    store.addMessage("Test8", TEST_PRIORITY_4);
    store.addMessage("test9", TEST_PRIORITY_4);
    store.addMessage("Test10", TEST_PRIORITY_4);
    store.addMessage("Test11", TEST_PRIORITY_5);
    store.addMessage("Test12", TEST_PRIORITY_5);

    // One collection of messages per distinct priority value (there are 5 above).
    assertEquals(5, store.getTopK(12).size());

    // Get right number of individual messages.
    assertEquals(11, flattenTopK(store.getTopK(11)).size());
    assertEquals(12, flattenTopK(store.getTopK(12)).size());
  }

  @Test
  public void duplicateMessageAddTest() {
    int MORE_THAN_3 = 10;
    store.addMessage("Test1", TEST_PRIORITY_1);
    store.addMessage("Test1", TEST_PRIORITY_1);

    assertEquals(1, store.getTopK(MORE_THAN_3).size());
    assertEquals(1, flattenTopK(store.getTopK(MORE_THAN_3)).size());

    store.addMessage("Test1", TEST_PRIORITY_2);

    assertEquals(1, store.getTopK(MORE_THAN_3).size());
    assertEquals(1, flattenTopK(store.getTopK(MORE_THAN_3)).size());
  }

  /**
   * Test the getPriority(String) method of MessageStore.
   */
  @Test
  public void getPriorityTest() {
    String TEST1 = "Test1";
    assertEquals(MessageStore.NOT_FOUND, store.getPriority(TEST1), 0.1);
    assertTrue(store.addMessage(TEST1, TEST_PRIORITY_1));
    assertEquals(TEST_PRIORITY_1, store.getPriority(TEST1), 0.1);

    // Re-adding the message fails and does not change its priority.
    assertFalse(store.addMessage(TEST1, TEST_PRIORITY_2));
    assertEquals(TEST_PRIORITY_1, store.getPriority(TEST1), 0.1);
  }

  /**
   * Test the updatePriority(String, float) method of MessageStore.
   */
  @Test 
  public void updatePriorityTest() {
    String TEST1 = "Test1";

    // No message in store and updating fails when that's the case.
    assertEquals(MessageStore.NOT_FOUND, store.getPriority(TEST1), 0.1);
    assertFalse(store.updatePriority(TEST1, TEST_PRIORITY_3));
    assertEquals(MessageStore.NOT_FOUND, store.getPriority(TEST1), 0.1);

    // Add the message to the store.
    assertTrue(store.addMessage(TEST1, TEST_PRIORITY_1));
    assertEquals(TEST_PRIORITY_1, store.getPriority(TEST1), 0.1);

    // Update it and find its new priority is correct.
    assertTrue(store.updatePriority(TEST1, TEST_PRIORITY_2));
    assertEquals(TEST_PRIORITY_2, store.getPriority(TEST1), 0.1);
  }

  /**
   * Test the contains(String) method of MessageStore.
   */
  @Test 
  public void containsMessageTest() {
    String TEST1 = "FooBarBaz";

    // Message not contained initially.
    assertFalse(store.contains(TEST1));

    // Add it, now it's contained.
    store.addMessage(TEST1, TEST_PRIORITY_1);
    assertTrue(store.contains(TEST1));
    
    // Add it again, still contained.
    store.addMessage(TEST1, TEST_PRIORITY_2);
    assertTrue(store.contains(TEST1));

    // Update its priority, still contained.
    store.updatePriority(TEST1, TEST_PRIORITY_3);
    assertTrue(store.contains(TEST1));
    
    // TODO(lerner): When delete message is implemented, show that after deletiong
    // the message is no longer contained.
  }

  /**
   * Utility method for testing check priority.
   */
  private boolean isValidPriority(double priority) {
    try {
      MessageStore.checkPriority(priority);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Test that checkPriority bounds the right numbers.
   */
  @Test
  public void checkPriorityTest() {
    assertTrue(isValidPriority(1.0));
    assertTrue(isValidPriority(1.2));
    assertTrue(isValidPriority(1.2398023948234092384));
    assertTrue(isValidPriority(0.5));
    assertTrue(isValidPriority(0.0));
    assertTrue(isValidPriority(0.000000000001));
    assertFalse(isValidPriority(1000.0));
    assertFalse(isValidPriority(-1.0));
    assertFalse(isValidPriority(-2.0));
    assertFalse(isValidPriority(-0.00000001));
    assertFalse(isValidPriority(10.0));
    assertFalse(isValidPriority(2.000001));
  
  }
}
