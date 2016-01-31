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
import org.denovogroup.murmur.backend.Crypto.PrivateSetIntersection;
import org.denovogroup.murmur.backend.Crypto.PrivateSetIntersection.ServerReplyTuple;
import org.denovogroup.murmur.objects.ClientMessage;
import org.denovogroup.murmur.objects.MurmurMessage;
import org.denovogroup.murmur.objects.ServerMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;

import okio.ByteString;

/**
 * Performs a single exchange with a Rangzen peer, using the PSI protocol
 * to exchange friends in a zero-knowledge fashion.
 *
 * Transferred messages are being converted into a well known JSON format to enable message
 * exchange between two different types of message java objects.
 *
 * This class is given input and output streams and communicates over them,
 * oblivious to the underlying network communications used.
 */
public class CryptographicExchange extends Exchange {

    private Context mContext;

    /** PSI computation for the half of the exchange where we're the "client". */
  private PrivateSetIntersection mClientPSI;
  
  /** PSI computation for the half of the exchange where we're the "server". */
  private PrivateSetIntersection mServerPSI;

    /** Friends list received from the remote party */
    private ArrayList<byte[]> remoteBlindedFriends;

  /** ClientMessage received from the remote party. */
  private ClientMessage mRemoteClientMessage;

  /** ServerMessage received from the remote party. */
  private ServerMessage mRemoteServerMessage;

  /** Tag appears in Android log messages. */
  private static final String TAG = "CryptographicExchange";

    private static final Logger log = Logger.getLogger(TAG);

    private static final String MESSAGE_COUNT_KEY = "count";

  
  /**
   * Perform the exchange asynchronously, calling back success or failure on
   * the callback that was passed to the constructor.
   */
  @Override
  public void run() {
    // The error-handling here tries to be as simple as possible: if anything
    // goes wrong that causes an exception, we call the .failure() method on the
    // callback. All the subroutines here (init/send/receive client/server message routines)
    // store intermediate results in instance variables rather than passing them
    // as arguments, and they all throw IOException if something goes wrong with
    // sending/receiving (some subparts can throw other exceptions too, which are
    // handled in the same way here, treated as fatal).
    //
    // When these subroutines called in the following try block fail, they set
    // the error message and error state of the exchange to reasonable values
    // before throwing their exceptions.
    try {
        log.debug("starting cryptographicExchange");
      // TODO(lerner): This (initializing PSIs) is costly, so we may want to
      // do this offline if it's making exchanges slow.
      initializePSIObjects();
        //Send client's friends
        sendFriends();
        //receive server's friends
        receiveFriends();
        // Send server message in response to remote client message.
        sendServerMessage();
        // Receive server message.
        receiveServerMessage();

        computeSharedFriends();

      // Send client message.
      sendClientMessage();

      // Receive client message.
      receiveClientMessage();
      
      setExchangeStatus(Status.SUCCESS);

        mContext = null;

      callback.success(this);
    } catch (Exception e) {  // Treat ALL exceptions as fatal.
        log.error("Exception while run()ing CryptographicExchange: ", e);
        if(getExchangeStatus() == Status.ERROR_RECOVERABLE){
            callback.recover(this, getErrorMessage());
        } else {
            // This status setting should be redundant (whoever threw the exception
            // should have set the status to ERROR before throwing the exception) but
            // this maintains the invariant that whenever callback.failure is called
            // the error code is ERROR.
            setExchangeStatus(Status.ERROR);
            callback.failure(this, getErrorMessage());
        }
    }
  }

  /**
   * Initializes the client and server PSI objects with the node's friends.
   */
  private void initializePSIObjects() throws NoSuchAlgorithmException, 
                                             IllegalArgumentException {
      log.debug("initializing PSIObject");
      ArrayList<byte[]> friends = friendStore.getAllFriendsBytes();
    try {
      // The clientPSI object manages the interaction in which we're the "client".
      // The serverPSI object manages the interaction in which we're the "server".
      mClientPSI = new PrivateSetIntersection(friends);
      mServerPSI = new PrivateSetIntersection(friends);
    } catch (NoSuchAlgorithmException e) {
      setExchangeStatus(Status.ERROR); 
      setErrorMessage("No such algorithm when creating PrivateSetIntersection." + e);
      throw e;
    }

  }

    /**
     * Construct and send a ClientMessage to the remote party including
     * blinded friends from the friend store.
     */
    private void sendFriends() throws IOException{
        log.debug("sending local contacts list");
        ArrayList<ByteString> blindedFriends = SecurityManager.getCurrentProfile(mContext).isUseTrust() ?
                Crypto.byteArraysToStrings(mClientPSI.encodeBlindedItems()) : new ArrayList<ByteString>();
        ClientMessage cm = new ClientMessage(null ,blindedFriends);
        if(!lengthValueWrite(out, cm.toJSON())){
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Length/value write of client friends failed.");
            throw new IOException("Length/value write of client friends failed, but exception is hidden (see Exchange.java)");
        }
    }


    private void receiveFriends() throws IOException{
        log.debug("receiving remote contacts");
        mRemoteClientMessage = ClientMessage.fromJSON(lengthValueRead(in));

        if (mRemoteClientMessage == null) {
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Remote client friends was not received.");
            throw new IOException("Remote client friends not received.");
        }

        if (mRemoteClientMessage.blindedFriends == null) {
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Remote client friends field was null");
            throw new IOException("Remote client friends field was null");
        }

        // This can't return null because byteStringsToArrays only returns null
        // when passed null, and we already checked that ClientMessage.blindedFriends
        // isn't null.
        remoteBlindedFriends = SecurityManager.getCurrentProfile(mContext).isUseTrust() ?
                Crypto.byteStringsToArrays(mRemoteClientMessage.blindedFriends) :
                new ArrayList<byte[]>();
    }

  /**
   * Construct and send a ClientMessage to the remote party including messages
   * from the message store.
   */
  private void sendClientMessage() throws IOException, JSONException {
      log.debug("sending messages");
      //create a message pool to be sent and send each message individually to allow partial data recovery in case of connection loss
      boolean success = true;
      List<MurmurMessage> messagesPool = getMessages(commonFriends);

      //notify the recipient how many items we expect to send him.
      JSONObject exchangeInfoMessage = new JSONObject("{\""+MESSAGE_COUNT_KEY+"\":"+messagesPool.size()+"}");

      if(!lengthValueWrite(out, exchangeInfoMessage)){
          success = false;
      } else {
          for (MurmurMessage message : messagesPool) {
              log.debug("sending a message");
              List<JSONObject> messageWrapper = new ArrayList<>();
              messageWrapper.add(message.toJSON(mContext));
              ClientMessage cm = new ClientMessage((ArrayList<JSONObject>)messageWrapper, null);
              if (!lengthValueWrite(out, cm.toJSON())) {
                  success = false;
              }
          }
      }
    if (!success) {
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Length/value write of client message failed.");
      throw new IOException("Length/value write of client message failed, but exception is hidden (see Exchange.java)");
    }
  }

  /**
   * Receive and return a ClientMessage sent by the remote party.
   *
   * @return A ClientMessage sent by the remote party, or null in the case of an error.
   */
  private void receiveClientMessage() throws IOException {
      log.debug("receiving messages");
      //the first message received is a hint, telling the us how many messages will be sent
      int messageCount = 0;

      JSONObject exchangeInfo = lengthValueRead(in);

      if(exchangeInfo != null){
          try {
              log.debug("peer wish to send us:"+exchangeInfo.getInt(MESSAGE_COUNT_KEY)+" messages");
              messageCount = Math.min(SecurityManager.getCurrentProfile(mContext).getMaxMessages(), exchangeInfo.getInt(MESSAGE_COUNT_KEY));
              log.debug("we accept receiving only:"+messageCount+" message");
          } catch (Exception e){}
      }

      //if recipient list is not instantiated yet create it
      if(mMessagesReceived == null) mMessagesReceived = new ArrayList<>();

      //Define the get single message task
      class ReceiveSingleMessage implements Callable<ClientMessage> {

          @Override
          public ClientMessage call() throws Exception {
              log.debug("receiving message");
              ClientMessage mCurrentReceived;

              mCurrentReceived = ClientMessage.fromJSON(lengthValueRead(in));

              if (mCurrentReceived == null) {
                  throw new Exception("Remote client message not received.");
              }

              if (mCurrentReceived.messages == null) {
                  throw new Exception("Remote client messages field was null");
              }

              log.debug("message received");

              return mCurrentReceived;
          }
      }

      //read from the stream until either times out or get all the messages
      ExecutorService executor = Executors.newSingleThreadExecutor();
      while(mMessagesReceived.size() < messageCount) {
          log.debug("received message list not yet full, attempting to get messages...");
          Future<ClientMessage> task = executor.submit(new ReceiveSingleMessage());
          try {
              log.debug("requesting results from receive message task");
              mRemoteClientMessage = task.get(EXCHANGE_TIMEOUT, TimeUnit.MILLISECONDS);
              log.debug("got results from receive message task");
              //Add everything passed in the wrapper to the pool
              for(JSONObject message : mRemoteClientMessage.messages) {
                  log.debug("unwrapping message");
                  mMessagesReceived.add(MurmurMessage.fromJSON(mContext, message));
                  log.debug("message unwrapped");
              }
          } catch (ExecutionException ex){
              ex.printStackTrace();
              executor.shutdown();
              if (mMessagesReceived.isEmpty()) {
                  setExchangeStatus(Status.ERROR);
              } else {
                  setExchangeStatus(Status.ERROR_RECOVERABLE);
              }
              task.cancel(true);
              setErrorMessage(ex.getMessage());
              throw new IOException(ex.getMessage());
          } catch (InterruptedException | TimeoutException e) {
              e.printStackTrace();
              executor.shutdown();
              if (mMessagesReceived.isEmpty()) {
                  setExchangeStatus(Status.ERROR);
              } else {
                  setExchangeStatus(Status.ERROR_RECOVERABLE);
              }
              task.cancel(true);
              setErrorMessage("Message receiving timed out");
              throw new IOException ("Message receiving timed out");
          }
      }
      executor.shutdown();
      log.debug("done receiving messages");
      if (mRemoteClientMessage == null) {
          throw new IOException("Remote client message was null in sendServerMessage.");
      }
  }

  /**
   * Construct a response to the given remote client's ClientMessage and send that
   * response to the remote party.
   */
  private void sendServerMessage() throws NoSuchAlgorithmException, 
                                          IOException {
      log.debug("sending server message");
    if (remoteBlindedFriends == null) {
      throw new IOException("Remove client message blinded friends is null in sendServerMessage.");
    }

    // Calculate responses that appear in the ServerMessage.
    ServerReplyTuple srt;
    try { 
      srt = mServerPSI.replyToBlindedItems(remoteBlindedFriends);
    } catch (NoSuchAlgorithmException e) {
      log.info("No such algorithm in replyToBlindedItems: " + e);
      setExchangeStatus(Status.ERROR);
      setErrorMessage("PSI subsystem is broken, NoSuchAlgorithmException");
      throw e;
    } catch (IllegalArgumentException e) {
      log.info("Null passed to replyToBlindedItems on serverPSI? " + e);
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Bad argument to server PSI subsystem. (null remoteBlindedItems?)");
      throw e;
    }
      log.debug("formatting server message");
    // Format and create ServerMessage.
    ArrayList<ByteString> doubleBlindedStrings = Crypto.byteArraysToStrings(srt.doubleBlindedItems);
    ArrayList<ByteString> hashedBlindedStrings = Crypto.byteArraysToStrings(srt.hashedBlindedItems);
    ServerMessage sm = new ServerMessage(doubleBlindedStrings,hashedBlindedStrings);

    // Write out the ServerMessage.
    boolean success = lengthValueWrite(out, sm.toJson());
    if (!success) {
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Length/value write of server message failed.");
      throw new IOException("Length/value write of server message failed, but exception is hidden (see Exchange.java)");
    }
      log.debug("server message was sent");
  }

  /**
   * Receive server response from remote party.
   *
   * @return A ServerMessage representing the remote party's server message.
   */
  private void receiveServerMessage() throws IOException {
      log.debug("receiving server message from peer");
    mRemoteServerMessage = ServerMessage.fromJSON(lengthValueRead(in));
    if (mRemoteServerMessage == null) {
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Remote server message was not received.");
      throw new IOException("Remote server message was not received.");
    }
      log.debug("server message received");
  }

  /**
   * Compute the number of shared friends from the PSI operation and store
   * that number in an instance variable.
   */
  private void computeSharedFriends() throws NoSuchAlgorithmException, IOException {
      log.debug("calculating shared contacts");
    commonFriends = mClientPSI.getCardinality(getSRTFromServerTuple());

      int requiredFriends = SecurityManager.getCurrentProfile(mContext).minSharedContacts;
      if(requiredFriends > commonFriends && SecurityManager.getCurrentProfile(mContext).isUseTrust()){
          setExchangeStatus(Status.ERROR);
          setErrorMessage("Session rejected by client due to insufficient common friends with server(required:"+requiredFriends+" found:"+commonFriends+").");
          throw new IOException("Session rejected by client due to insufficient common friends with server(required:"+requiredFriends+" found:"+commonFriends+").");
      }
  }

  /**
   * Deserialize the contents of the ServerMessage into a ServerReplyTuple.
   */
  private ServerReplyTuple getSRTFromServerTuple() {
    ArrayList<byte[]> doubleBlindedItems = 
      Crypto.byteStringsToArrays(mRemoteServerMessage.doubleBlindedFriends);
    ArrayList<byte[]> hashedBlindedItems = 
      Crypto.byteStringsToArrays(mRemoteServerMessage.hashedBlindedFriends);
    
    // Since ServerReplyTuple is an inner non-static class, it can't be instantiated
    // without an instance of PrivateSetIntersection, which is it its outer class.
    // Thus we have to use mClientPSI.new.
    return mClientPSI.new ServerReplyTuple(doubleBlindedItems, hashedBlindedItems);
  }


  /**
   * Pass-through constructor to superclass constructor.
   */
  public CryptographicExchange(Context context, String peerAddress, InputStream in, OutputStream out, boolean asInitiator,
                               FriendStore friendStore, MessageStore messageStore, 
                               ExchangeCallback callback) throws IllegalArgumentException {
    super(peerAddress, in, out, asInitiator, friendStore, messageStore, callback);

      mContext = context;
  }


}
