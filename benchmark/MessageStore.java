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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Storage for Rangzen messages that uses StorageBase underneath. If
 * instantiated as such, automatically encrypts and decrypts data before storing
 * in Android.
 */
public class MessageStore {
  /** A handle for the underlying store */
  private StorageBase store;

  /** Context for the app. */
  private Context mContext;

  /** The internal key used in the underlying store for Rangzen message data. */
  private static final String MESSAGES_KEY = "RangzenMessages-";

  /** Intent action for new message arrival in the store. */
  public static final String NEW_MESSAGE = "org.denovogroup.rangzen.NEW_MESSAGE_ACTION";

  /**
   * The internal key used in the underlying store for Rangzen message
   * priorities.
   */
  private static final String MESSAGE_PRIORITY_KEY = "RangzenMessagePriority-";

  /**
   * The number of bins to use for storing messages. Each bin stores
   * 1/NUM_BINS range of priority values, called the INCREMENT. Bin 0 stores
   * [0,INCREMENT), bin 1 stores [INCREMENT, 2*INCREMENT), etc.
   */
  private static final int NUM_BINS = 5;

  /** The range increment, computed from the number of bins. */
  private static final double INCREMENT = 1.0f / (double) NUM_BINS;

  /** The min priority value. */
  private static final double MIN_PRIORITY_VALUE = 0.0f;

  /** The max priority value. */
  private static final double MAX_PRIORITY_VALUE = 2.0f;

  // The default value that indicates not found.
  public static final double NOT_FOUND = -2.0f;


  /** Ensures that the given priority value is in range. */
  /* package */ static void checkPriority(double priority)
      throws IllegalArgumentException {
    if (priority < MIN_PRIORITY_VALUE || priority > MAX_PRIORITY_VALUE) {
      throw new IllegalArgumentException("Priority " + priority
          + " is outside valid range of [0,2]");
    }
  }

  /**
   * Determines the bin key for a given bin number.
   * 
   * @param bin
   *            The bin number.
   * 
   * @return The String bin key to use for that bin.
   */
  private static String getBinKey(int bin) {
    return MESSAGES_KEY + bin;
  }

  /**
   * Determines the message priority key for a given message.
   * 
   * @param msg
   *            The message.
   * 
   * @return The String message priority key for that message.
   */
  private static String getMessagePriorityKey(String msg) {
    return MESSAGE_PRIORITY_KEY + msg;
  }

  /**
   * Determines the bin that corresponds to a given priority value.
   * 
   * @param priority
   *            The priority for which to identify the bin number.
   * 
   * @return The bin number.
   */
  private static int getBinForPriority(double priority) {
    checkPriority(priority);

    int bin = (int) (priority / INCREMENT);
    if (bin >= NUM_BINS) {
      // We should only select this bin if priority == 1.0.
      bin = NUM_BINS - 1;
    }

    return bin;
  }

  /**
   * Determines the message key to use for the bin corresponding to the given
   * priority.
   * 
   * @param priority
   *            The message for which to look up the key.
   * 
   * @return A String to use as a key for the given bin.
   */
  private static String getBinKeyForPriority(double priority) {
    checkPriority(priority);

    int bin = getBinForPriority(priority);
    return getBinKey(bin);
  }

  /**
   * Creates a Rangzen message store, with a consistent application of
   * encryption of that stored data, as specified. All messages are associated
   * with a priority value that can be modified.
   * 
   * @param context
   *            The app instance for which to perform storage.
   * @param encryptionMode
   *            The encryption mode to use for all calls using this instance.
   * 
   *            TODO(barath): Add support for a storage limit, which
   *            automatically triggers garbage collection when hit.
   */
  public MessageStore(Context context, int encryptionMode)
      throws IllegalArgumentException {
    store = new StorageBase(context, encryptionMode);
    mContext = context;
  }

  /**
   * Adds the given message with the given priority.
   * 
   * @param msg
   *            The message to add.
   * @param priority
   *            The priority to associate with the message. The priority must
   *            be [0,1].
   * 
   * @return Returns true if the message was added. If the message already
   *         exists, does not modify the store and returns false.
   */
  public boolean addMessage(String msg, double priority) {
    checkPriority(priority);

    // Check whether we have the message already (perhaps in another bin).
    // TODO(barath): Consider improving performance by selecting a different
    // key.
    String msgPriorityKey = MESSAGE_PRIORITY_KEY + msg;

    // A value less than all priorities in the store.
    final double MIN_PRIORITY = -1.0f;

    boolean found = !(store.getDouble(msgPriorityKey, NOT_FOUND) < MIN_PRIORITY);
    if (found) {
      return false;
    }

    // Get the existing message set for the bin, if it exists.
    String binKey = getBinKeyForPriority(priority);
    Set<String> msgs = store.getSet(binKey);
    if (msgs == null) {
      msgs = new HashSet<String>();
    }

    // Add the message with the given priority, and to the bin.
    store.putDouble(msgPriorityKey, priority);
    msgs.add(msg);
    store.putSet(binKey, msgs);
    
    /** Sending the broadcast here when a message is added to the phone. **/
    Intent intent = new Intent();
    intent.setAction(NEW_MESSAGE);
    mContext.sendBroadcast(intent);
    return true;  
  }
  
  /** 
   * Get the priority of a message, if it exists in the store.
   *
   * @param msg The text of the message to search for.
   * @return The priority of the message as we know it, or MessageStore.NOT_FOUND
   * if the message is not in the store.
   */
  public double getPriority(String msg) {
    String msgPriorityKey = MESSAGE_PRIORITY_KEY + msg;
    return store.getDouble(msgPriorityKey, NOT_FOUND);
  }

  /**
   * Update the priority of a message, if it exists in the store.
   *
   * @param msg The message whose priority should be changed.
   * @param priority The new priority to set.
   * @return True if the message was in the store (and its priority was changed),
   * false otherwise.
   */
  /* package */ boolean updatePriority(String msg, double priority) {
    checkPriority(priority);

    // A value less than all priorities in the store.
    final double MIN_PRIORITY = -1.0f;
      
    String msgPriorityKey = MESSAGE_PRIORITY_KEY + msg;
    boolean found = !(store.getDouble(msgPriorityKey, NOT_FOUND) < MIN_PRIORITY);
    if (!found) {
      return false;
    }
    store.putDouble(msgPriorityKey, priority);
    return true;
  }

  /**
   * Check whether the store contains the given message.
   *
   * @param msg A message to check for.
   * @return True if the message is contained in the store, false otherwise.
   */
  public boolean contains(String msg) {
    // A value less than all priorities in the store.
    final double MIN_PRIORITY = -1.0f;

    String msgPriorityKey = MESSAGE_PRIORITY_KEY + msg;

    return !(store.getDouble(msgPriorityKey, NOT_FOUND) < MIN_PRIORITY);
  }

  /**
   * Removes the given message from the store.
   * 
   * @param msg
   *            The message to remove.
   * 
   * @return Returns true if the message was removed. If the message was not
   *         found, returns false.
   */
  public boolean deleteMessage(String msg) {
    // TODO(barath): Implement.
    return false;
  }

  /**
   * Returns the given message's priority, if present.
   * 
   * @param msg
   *            The message whose priority to retrieve.
   * @param defvalue
   *            The default value to return if not found.
   * 
   * @return Returns msg's priority or defvalue if not found.
   */
  public double getMessagePriority(String msg, double defvalue) {
    return store.getDouble(getMessagePriorityKey(msg), defvalue);
  }

  /**
   * Returns the messages with highest priority values.
   * 
   * @param k
   *            The number of messages to return.
   * 
   * @return Returns up to k messages with the highest priority values.
   */
  public TreeMap<Double, Collection<String>> getTopK(int k) {
    TreeMap<Double, Collection<String>> topk = new TreeMap<Double, Collection<String>>();

    int msgsStored = 0;

    binloop: for (int bin = NUM_BINS - 1; bin >= 0; bin--) {
      String binKey = getBinKey(bin);
      Set<String> msgs = store.getSet(binKey);
      if (msgs == null)
        continue;

      TreeMap<Double, List<String>> sortedmsgs = new TreeMap<Double, List<String>>();
      for (String m : msgs) {
        double priority = getMessagePriority(m, -1);
        if (!sortedmsgs.containsKey(priority)) {
          sortedmsgs.put(priority, new ArrayList<String>());
        }
        sortedmsgs.get(priority).add(m);
      }

      NavigableMap<Double, List<String>> descMap = sortedmsgs
          .descendingMap();
      for (Entry<Double, List<String>> e : descMap.entrySet()) {
        for (String m : e.getValue()) {
          if (msgsStored >= k)
            break binloop;

          double priority = e.getKey();
          if (!topk.containsKey(priority)) {
            topk.put(priority, new HashSet<String>());
          }
          topk.get(priority).add(m);
          msgsStored++;
        }
      }
    }

    return topk;
  }

  /**
   * This will iterate over all of the messages in the message store and
   * return the number of messages
   * 
   * @return count The total number of messages in the message store.
   */
  public int getMessageCount() {
    int count = 0;
    for (int bin = NUM_BINS - 1; bin >= 0; bin--) {
      String binKey = getBinKey(bin);
      Set<String> msgs = store.getSet(binKey);
      if (msgs == null)
        continue;

      for (String m : msgs) {
        count++;
      }
    }
    return count;
  }

  /**
   * This method goes through every message in all of the bins and inserts
   * them into an array list by trust score and then in the case of a tie,
   * alphabetically.
   * 
   * @param k
   *            The index of the message to return.
   * @return A message object that is the kth most trusted.
   */
  public Message getKthMessage(int k) {
    ArrayList<Message> topk = new ArrayList<Message>();

    for (int bin = NUM_BINS - 1; bin >= 0; bin--) {
      String binKey = getBinKey(bin);
      Set<String> msgs = store.getSet(binKey);
      if (msgs == null)
        continue;

      for (String m : msgs) {
        double priority = getMessagePriority(m, -1);
        topk.add(new Message(priority, m));
      }
    }
    Collections.sort(topk, new Comparator<Message>() {

      @Override
      public int compare(Message lhs, Message rhs) {
        Message left = (Message) lhs;
        Message right = (Message) rhs;
        if (left.getPriority() > right.getPriority()) {
          return -1;
        } else if (left.getPriority() < right.getPriority()) {
          return 1;
        } else {
          return left.getMessage().compareTo(right.getMessage());
        }
      }
    });
    if (topk.size() <= k) {
      return null;
    }
    return topk.get(k);
  }

  /**
   * Message Object that contains the message's priority and the contents of
   * the message.
   * 
   * @author Jesus Garcia
   * 
   */
  public class Message {
    /** The priority of the message. */
    private double mPriority;
    /** The contents of the message. */
    private String mMessage;

    public Message(double priority, String message) {
      mPriority = priority;
      mMessage = message;
    }

    public String getMessage() {
      return mMessage;
    }

    public double getPriority() {
      return mPriority;
    }
  }
}
