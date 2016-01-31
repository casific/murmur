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

import org.apache.log4j.Logger;
import org.denovogroup.murmur.objects.CleartextFriends;
import org.denovogroup.murmur.objects.CleartextMessages;
import org.denovogroup.murmur.objects.MurmurMessage;
import org.denovogroup.murmur.ui.MurmurApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs a single exchange with a Rangzen peer.
 * This class is given input and output streams and communicates over them,
 * oblivious to the underlying network communications used.
 */
public class Exchange implements Runnable {
    /** Peer bluetooth address with which interacting, for history optimization. */
  /* package */ String peerAddress;
  /** Store of friends to use in this exchange. */
  /* package */ FriendStore friendStore;
  /** Store of messages to use in this exchange. */
  /* package */ MessageStore messageStore;
  /** Input stream connected to the remote communication partner. */
  /* package */ InputStream in;
  /** Output stream connected to the remote communication partner. */
  /* package */ OutputStream out;
  /** A callback to report the result of an exchange. */
  /* package */ ExchangeCallback callback;
  /** 
   * Whether to start the exchange with the first message or wait for the other side
   * to begin the exchange.
   */
  private boolean asInitiator;
  
  /** The number of friends in common with the remote peer. */
  /* package */ int commonFriends = -1;

  /** Messages received from remote party. */
  /* package */ List<MurmurMessage> mMessagesReceived;

  /** Friends received from remote party. */
  private CleartextFriends mFriendsReceived;

  /** Send up to this many messages (top priority) from the message store. */
  public static final int NUM_MESSAGES_TO_EXCHANGE = 100;

  /** Minimum trust multiplier in the case of 0 shared friends. */
  public static final double EPSILON_TRUST = .001;

  /** Enum indicating status of the Exchange. */
  enum Status {
    IN_PROGRESS,
    SUCCESS,
    ERROR,
      ERROR_RECOVERABLE
  }
  /**
   * Whether the exchange has completed successfully. Starts false and remains
   * false until the exchange completes, if it ever does. Remains true thereafter.
   * Set in doInBackground and checked in onPostExecute().
   */
  private Status mStatus = Status.IN_PROGRESS;

  /** 
   * An error message, if any, explaning why the exchange isn't successful.
   * Set to null upon success.
   */
  private String mErrorMessage = "Not yet complete.";

  /** Included with Android log messages. */
  private static final String TAG = "Exchange";

    private static final Logger log = Logger.getLogger(TAG);

  /** Number of bytes in a megabyte. */
  private static final int MEGABYTES = 1024 * 1024;

    /** The number of milliseconds until a single message exchange times out */
    public static final long EXCHANGE_TIMEOUT = 2000;

  /**
   * Size, in bytes, of the maximum size message we'll try to read with lengthValueRead.
   * This is necessary since otherwise a malicious remote party can just provide a huge
   * size and cause an OutOfMemory error when we allocate a buffer.
   */
  private static final int MAX_MESSAGE_SIZE = 10 * MEGABYTES;

  /** Synchronized getter for status. */
  /* package */ synchronized Status getExchangeStatus() {
    return mStatus;
  }
  /** Synchronized setter for status. */
  /* package */ synchronized void setExchangeStatus(Status status) {
    this.mStatus = status;
  }

  /** Synchronized getter for error message. */
  /* package */ synchronized String getErrorMessage() {
    return mErrorMessage;
  }
  /** Synchronized setter for error message. */
  /* package */ synchronized void setErrorMessage(String errorMessage) {
    this.mErrorMessage = errorMessage;
  }

  /**
   * Create a new exchange which will communicate over the given Input/Output
   * streams and use the given context to access storage for messages/friends.
   *
   * @param peerAddress the bluetooth address of the remote peer.
   * @param in An input stream which delivers a stream of data from the remote peer.
   * @param in An output stream which delivers a stream of data to the remote peer.
   * @param friendStore A store of friends to use in the friend-exchange protocol.
   * @param messageStore A store of messages to exchange with the remote peer.
   */
  public Exchange(String peerAddress, InputStream in, OutputStream out, boolean asInitiator,
                  FriendStore friendStore, MessageStore messageStore, 
                  ExchangeCallback callback) throws IllegalArgumentException {
      this.peerAddress = peerAddress;
    this.in = in;
    this.out = out;
    this.friendStore = friendStore;
    this.messageStore = messageStore;
    this.asInitiator = asInitiator;
    this.callback = callback;

    // TODO(lerner): Probalby best to throw exceptions here, since these are fatal.
    // There's no point in trying to have an exchange without someone to talk to
    // or friends/messages to talk about.
    
    if (in == null) {
      throw new IllegalArgumentException("Input stream for exchange is null.");
    }
    if (out == null) {
      throw new IllegalArgumentException("Output stream for exchange is null.");
    }
    if (friendStore == null) {
      throw new IllegalArgumentException("Friend store for exchange is null.");
    }
    if (messageStore == null) {
      throw new IllegalArgumentException("Message store for exchange is null.");
    }
    if (callback == null) {
      // I log this as a warning because not providing callbacks is a thing, it's
      // just an illogical thing here in all likelihood.
      // But I throw an exception because it simply isn't reasonable to pass null
      // unless we change the architecture of exchanges.
      log.warn( "No callback provided for exchange - nothing would happen locally!");
      throw new IllegalArgumentException("No callback provided for exchange.");
    }
  }

  /**
   * Get friends from the FriendStore, encode them as a CleartextFriends protobuf
   * object, and write that Message out to the output stream.
   *
   * TODO(lerner): Limit the number of friends used.
   */
  private void sendFriends() {
    List<String> friends = new ArrayList<String>();
    friends.addAll(friendStore.getAllFriends());
    CleartextFriends friendsMessage = new CleartextFriends((ArrayList<String>) friends);
      JSONObject friendsMessageJson = friendsMessage.toJson();
    lengthValueWrite(out, friendsMessageJson);
  }

  /**
   * Retrieve at most NUM_MESSAGES_TO_EXCHANGE messages from the message store and
   * return them. If no messages, returns a empty list.
   *
   * @return The top NUM_MESSAGES_TO_EXCHANGE in the MessageStore.
   */
  /* package */ List<MurmurMessage> getMessages(int sharedContacts) {
    return MessageStore.getInstance().getMessagesForExchange(sharedContacts);
  }

  /**
   * Get messages from the MessageStore, encode them as a CleartextMessages protobuf
   * object, and write that Message out to the output stream.
   */
  private void sendMessages() {
      // get messages to send
      List<MurmurMessage> messages = getMessages(0);
      //notify the recipient how many items we expect to send him.
      MurmurMessage exchangeInfoMessage = new MurmurMessage("ExchangeAgreement", Integer.toString(messages.size()),1d);
      if(lengthValueWrite(out, exchangeInfoMessage.toJSON(MurmurApplication.getContext()))) {
          // Send messages
         for(MurmurMessage message : messages){

             List<MurmurMessage> packet = new ArrayList<>();
             packet.add(message);

              CleartextMessages messagesMessage = new CleartextMessages((ArrayList<MurmurMessage>) packet);
              lengthValueWrite(out, messagesMessage.toJson(MurmurApplication.getContext()));
          }
      }
  }

  /**
   * Receive friends from the remote device.
   */
  private void receiveFriends() {
    CleartextFriends friendsReceived = CleartextFriends.fromJSON(lengthValueRead(in));
    this.mFriendsReceived = friendsReceived;

    if (mFriendsReceived != null && mFriendsReceived.friends != null) {
      Set<String> myFriends = friendStore.getAllFriends();
      Set<String> theirFriends = new HashSet(mFriendsReceived.friends);
      Set<String> intersection = new HashSet(myFriends);
      intersection.retainAll(theirFriends);
      commonFriends = intersection.size();
      log.info( "Received " + theirFriends.size() + " friends. Overlap with my " +
              myFriends.size() + " friends is " + commonFriends);
    } else if (mFriendsReceived == null) {
      log.info( "Friends received is null: " + mFriendsReceived);
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Failed receiving friends.");
    } else {
      log.error( "Friends received.friends is null");
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Failed receiving friends.");
    }
  }

  /**
   * Receive messages from the remote device.
   */
  private void receiveMessages() {
      //the first message received is a hint, telling the us how many messages will be sent
      int messageCount = 0;
      MurmurMessage exchangeInfo = MurmurMessage.fromJSON(MurmurApplication.getContext(), lengthValueRead(in));
      if(exchangeInfo != null){
          try {
              messageCount = Math.min(NUM_MESSAGES_TO_EXCHANGE, Integer.parseInt(exchangeInfo.text));
          } catch (Exception e){}
      }

      //if recipient list is not instantiated yet create it
      if(mMessagesReceived == null) mMessagesReceived = new ArrayList<>();

      //Define the get single message task
      class ReceiveSingleMessage implements Callable<List<MurmurMessage>> {

          @Override
          public List<MurmurMessage> call() throws Exception {
              CleartextMessages mCurrentReceived;
              mCurrentReceived = CleartextMessages.fromJson(MurmurApplication.getContext(),lengthValueRead(in));
              return mCurrentReceived.messages;
          }
      }

      //read from the stream until either times out or get all the messages
      ExecutorService executor = Executors.newSingleThreadExecutor();
      while(mMessagesReceived.size() < messageCount) {
          Future<List<MurmurMessage>> task = executor.submit(new ReceiveSingleMessage());
          try {
              List<MurmurMessage> res = task.get(EXCHANGE_TIMEOUT, TimeUnit.MILLISECONDS);
              mMessagesReceived.addAll(res);
          } catch (InterruptedException |ExecutionException | TimeoutException e) {
              e.printStackTrace();
              task.cancel(true);
              break;
          }
      }
      executor.shutdown();
  }

  /**
   * Perform the exchange.
   */
  @Override
  public void run() {
    // In this version of the exchange there's no crypto, so the messages don't
    // depend on each other at all.
    if (asInitiator) {
      log.info( "About to send friends.");
      sendFriends();
      log.info( "Sent friends. About to send messages.");
      sendMessages();
      log.info( "Sent messages. About to receive friends.");
      receiveFriends();
      log.info( "Received friends. About to receive messages.");
      receiveMessages();
    } else {
      receiveFriends();
      receiveMessages();
      sendFriends();
      sendMessages();
    }
    if (getExchangeStatus() == Status.IN_PROGRESS) {
      setExchangeStatus(Status.SUCCESS);
    }

    // We're done with the mechanics of the exchange - if there's a callback
    // to report to, call its .success() or .failure() method as appropriate.
    if (callback == null) {
      log.warn( "No callback provided to exchange.");
      return;
    }
    if (getExchangeStatus() == Status.SUCCESS) {
      callback.success(this);
      return;

    }else if(getExchangeStatus() == Status.ERROR_RECOVERABLE){
        callback.recover(this, mErrorMessage);
        return;
    } else {
      callback.failure(this, mErrorMessage);
      return;
    }
  }

  /**
   * Get the number of friends in common with the other peer in the exchange.
   * -1 if the number of common friends is not yet known.
   *
   * Returns -1 until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * @return The number of friends in common with the remote peer, or -1 if the
   * number is not yet known because the exchage hasn't completed yet or has failed.
   */
  public int getCommonFriends() {
    if (getExchangeStatus() == Status.SUCCESS || getExchangeStatus() == Status.ERROR_RECOVERABLE) {
      return commonFriends;
    } else {
      return -1;
    }
  }

  /**
   * Get the messages we received from the remote peer. 
   *
   * Returns null until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * TODO(lerner): Are there any attacks involving giving multiple copies of the
   * same message? Do we take the one with the highest or lowest priority? Or 
   * abandon the exchange?
   *
   * @return The set of messages received from the remote peer, or null if we
   * the exchange hasn't completed yet or the exchange failed.
   */ 
  public List<MurmurMessage> getReceivedMessages() {
    if (getExchangeStatus() == Status.SUCCESS || getExchangeStatus() == Status.ERROR_RECOVERABLE) {
      return mMessagesReceived;
    } else {
      return null;
    }
  }

  /**
   * Get the friends we received from the remote peer. 
   *
   * Returns null until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * TODO(lerner): This is only a thing with cleartext friends. Later, with crypto
   * there's no way to know who the friends are.
   *
   * @return The set of friends received from the remote peer, or null if we
   * the exchange hasn't completed yet or the exchange failed.
   */ 
  public CleartextFriends getReceivedFriends() {
    if (getExchangeStatus() == Status.SUCCESS || getExchangeStatus() == Status.ERROR_RECOVERABLE) {
      return mFriendsReceived;
    } else {
      return null;
    }
  }

  /**
   * Take a Wire protobuf Message and encode it in a byte[] as:
   *
   * [length, value]
   *
   * where length is the length in bytes encoded as a 4 byte integer
   * and value are the bytes produced by Message.toByteArray().
   *
   * @param m A message to encode in length/value format.
   * @return A ByteBuffer containing the encoded bytes of the message and its length.
   */
  /* package */ static ByteBuffer lengthValueEncode(JSONObject m) {
    byte[] value = m.toString().getBytes();

    ByteBuffer encoded = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE + value.length);
    encoded.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    encoded.putInt(value.length);
    encoded.put(value);

    return encoded;
  }

  /**
   * Send the given message, encoded as length-value, on the given output stream.
   *
   * TODO(lerner): I don't like the fact that this returns true/false to signal
   * success or failure. I'd rather it threw an IOException that has to be handled
   * by the calling code. Would prefer to change this in future.
   *
   * @param outputStream The output stream to write the Message to.
   * @param m A message to write.
   * @return True if the write succeeds, false otherwise.
   */
  public static boolean lengthValueWrite(OutputStream outputStream, JSONObject m) {
    if (outputStream == null || m == null) {
      return false;
    }
    try {
      byte[] encodedMessage = Exchange.lengthValueEncode(m).array();
      outputStream.write(encodedMessage);
        outputStream.flush();
      return true;
    } catch (IOException e) {
      log.error( "Length/value write failed with exception: " , e);
      return false;
    }
  }

  /**
   * Decode a Message of the given type from the input stream and return it.
   * This method is much like a delimited read from Google's Protobufs.
   *
   * @param inputStream An input stream to read a Message from
   * @return The message recovered from the stream, or null if an error occurs.
   */
  public static JSONObject lengthValueRead(InputStream inputStream) {
    int length = popLength(inputStream);
    if (length < 0) {
      return null;
    } else if (length > MAX_MESSAGE_SIZE) {
      log.error( "Remote party asked us to read " + length + " bytes in a length/value read");
      return null;
    }
    byte[] messageBytes = new byte[length];
    JSONObject recoveredMessage;
    int readByteCount = 0;
    try {
      while (readByteCount != length) {
        readByteCount += inputStream.read(messageBytes, readByteCount, length - readByteCount);
      }

      recoveredMessage = new JSONObject(new String(messageBytes));

    } catch (IOException e) {
      log.error( "IOException parsing message bytes: " , e);
        return null;
    } catch (JSONException e) {
        log.error( "IOException parsing message bytes: ", e);
        return null;
    }
      return recoveredMessage;
  }

  /**
   * Take the output of lengthValueEncode() and decode it to a Message of the
   * given type.
   */
  /* package */ static int popLength(InputStream stream) {
    byte[] lengthBytes = new byte[Integer.SIZE/Byte.SIZE];
    try {
      stream.read(lengthBytes);
    } catch (IOException e) {
      log.error( "IOException popping length from input stream: " , e);
      return -1;
    }
    ByteBuffer buffer = ByteBuffer.wrap(lengthBytes);
    buffer.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    return buffer.getInt();
  }

  /**
   * Given an old priority and the number of friends in common, calculate the
   * value of the new priority of a message.
   *
   * @param remote The priority of a message given by a peer.
   * @param stored The priority of the message in our store.
   * @param commonFriends The number of friends in common with the peer who sent
   * this message.
   */
  public static double newPriority(double remote, double stored, 
                                   int commonFriends, int myFriends) {
    
    // TODO(lerner): Add noise.
    return Math.max(fractionOfFriendsPriority(remote, commonFriends, myFriends),
            stored);
  }

  /** Compute the priority score for a person normalized by his number of friends.
   *
   * @param priority The priority of the message before computing trust.
   * @param sharedFriends Number of friends shared between this person and the message sender.
   * @param myFriends The number of friends this person has.
   */
  public static double fractionOfFriendsPriority(double priority,
                                                 int sharedFriends, 
                                                 int myFriends) {
    double trustMultiplier;
    // We want to preserve trust ordering in the case of 0 shared friends, so
    // we multiply by a non-0 number even when we have no shared friends.
    if (sharedFriends == 0 || myFriends == 0) {
      trustMultiplier = EPSILON_TRUST;
    } else {
      trustMultiplier = sharedFriends / (double) myFriends;
    }
    return priority * trustMultiplier;
  }

    public String getPeerAddress(){
        return peerAddress;
    }
}
