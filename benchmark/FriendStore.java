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
import android.util.Base64;
import android.util.Log;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Storage for friends that uses StorageBase underneath. 
 */
public class FriendStore {
  /** A handle for the underlying store */
  private StorageBase store;
  
  /** The internal key used in the underlying store for Rangzen friend data. */
  private static final String FRIENDS_STORE_KEY = "RangzenFriend-";

  /** The internal keys used in the underlying store for the public device ID (keys). */
  private static final String DEVICE_PUBLIC_ID_KEY = "PublicDeviceIDKey";

  /** The internal keys used in the underlying store for the private device ID (keys). */
  private static final String DEVICE_PRIVATE_ID_KEY = "PrivateDeviceIDKey";

  /** URI scheme for Rangzen friending. */
  public static final String QR_FRIENDING_SCHEME = "rangzen://";

  /** Tag for Android log messages. */
  private static final String TAG = "FriendStore";

  /**
   * Creates a Rangzen friend store, with a consistent application of encryption of that stored
   * data, as specified.
   *
   * @param context A context in which to do storage.
   * @param encryptionMode The encryption mode to use for all calls using this instance.
   */
  public FriendStore(Context context, int encryptionMode) throws IllegalArgumentException {
    store = new StorageBase(context, encryptionMode);
  }

  /**
   * Adds the given friend.
   *
   * @param msg The friend to add.
   *
   * @return Returns true if the friend was stored, false if the friend was already
   * stored.
   */
  private boolean addFriend(String friend) {
    Set<String> friends = store.getSet(FRIENDS_STORE_KEY);
    if (friends == null) {
      friends = new HashSet<String>();
    }
    if (friends.contains(friend)) {
      // Friend already stored.
      return false;
    }
    friends.add(friend);
    store.putSet(FRIENDS_STORE_KEY, friends);
    return true;
  }

  /**
   * Add the given bytes as a friend, converting them to base64 and storing them
   * in the FriendStore.
   *
   * @param friend The friend to be added.
   * @return True if the friend was added, false if not since it was already there.
   */
  public boolean addFriendBytes(byte[] friend) {
    if (friend == null) {
      throw new IllegalArgumentException("Null friend added through addFriendBytes()");
    }
    return addFriend(bytesToBase64(friend));
  }

  /**
   * Delete the given friend ID from the friend store, if it exists.
   *
   * @param friend The friend ID to delete.
   *
   * @return True if the friend existed and was deleted, false otherwise.
   */
  private boolean deleteFriend(String friend) {
    Set<String> friends = store.getSet(FRIENDS_STORE_KEY);
    if (friends == null) {
      // No friends known, so deleting a friend should always fail.
      return false;
    }
    // Friend known, so delete it.
    if (friends.contains(friend)) {
      friends.remove(friend);
      store.putSet(FRIENDS_STORE_KEY, friends);
      return true;
    }
    // Friends not empty but given friend to delete not known.
    return false;
  }

  /**
   * Delete the given bytes as a friend.
   *
   * @param friend The friend to be deleted.
   * @return True if the friend was deleted, false if they weren't in the store.
   */
  public boolean deleteFriendBytes(byte[] friend) {
    if (friend == null) {
      throw new IllegalArgumentException("Null friend deleted through addFriendBytes()");
    }
    return deleteFriend(bytesToBase64(friend));
  }

  /**
   * Get a list of all friends stored on this device.
   *
   * @return A set of friends ids.
   */
  public Set<String> getAllFriends() {
    Set<String> friends = store.getSet(FRIENDS_STORE_KEY);
    if (friends == null) {
      return new HashSet<String>();
    }
    return friends;
  }

  /**
   * Encode a byte array as a base64 string.
   * This method should be used to convert from byte[]s accepted by Crypto.java
   * and Strings stored in FriendStore.
   *
   * @param bytes The bytes to be converted.
   * @return A base64 encoded string of the bytes given, or null if bytes was null.
   */
  public static String bytesToBase64(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    return Base64.encodeToString(bytes, Base64.NO_WRAP);
  }

  /**
   * Encode a byte array as a base64 string.
   * This method should be used to convert from Strings stored by FriendStore
   * to byte[]s accepted by Crypto.java.
   *
   * @param string The string to be converted.
   * @return A byte[] of the bytes represented in base64 by the given string, or
   * null if the string was null or wasn't well formed base64.
   */
  public static byte[] base64ToBytes(String base64) throws IllegalArgumentException {
    if (base64 == null) {
      return null;
    }

    try {
      return Base64.decode(base64, Base64.NO_WRAP);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Returning null on attempt to decode badly formed base64 string: " + base64);
      return null;
    }
  }

  /**
   * Return all friends stored as byte[], decoded from their base64 stored representations.
   *
   * This doesn't take arguments but throws an underlying IllegalArgumentException.
   * Which is awkward, but it needs to be some kind of exception, and the underlying
   * exception has the most information. The exception might be thrown by 
   * base64ToBytes().
   *
   * @return The set of all stored friend IDs, as byte[].
   */
  public ArrayList<byte[]> getAllFriendsBytes() throws IllegalArgumentException {
    Set<String> base64s = getAllFriends();
    ArrayList<byte[]> byteArrays = new ArrayList<byte[]>();
    for (String base64 : base64s) { 
      byte[] bytes = base64ToBytes(base64);
      byteArrays.add(bytes);
    }
    return byteArrays;
  }

  /**
   * If the device has not previously generated and persisted its device ID (aka
   * public/private keypair for PSI), generates and stores said ID.
   *
   * If the ID is already stored, this harmlessly does nothing.
   */
  private void generateAndStoreDeviceID() {
    String privateDeviceID = store.get(DEVICE_PRIVATE_ID_KEY);
    String publicDeviceID = store.get(DEVICE_PUBLIC_ID_KEY);
    if (privateDeviceID == null || publicDeviceID == null) {
      // This would be very strange, if only half the ID was stored.
      if (privateDeviceID != publicDeviceID) {
        if (privateDeviceID == null) {
          Log.wtf(TAG, "Only one of private and public ID are stored! Public is stored, private is null.");
        } else {
          Log.wtf(TAG, "Only one of private and public ID are stored! Private is stored, public is null.");
        }
      }

      AsymmetricCipherKeyPair keypair = Crypto.generateUserID();
      privateDeviceID = bytesToBase64(Crypto.generatePrivateID(keypair));
      publicDeviceID = bytesToBase64(Crypto.generatePublicID(keypair));
      store.put(DEVICE_PRIVATE_ID_KEY, privateDeviceID);
      store.put(DEVICE_PUBLIC_ID_KEY, publicDeviceID);
    }
  }

  /**
   * Return the device's public device ID as a base64 encoded string, ready to be
   * shared with another device (e.g. as part of a QR code).
   *
   * @return A base64 encoded string representing the local device's public ID,
   * or null if something went wrong.
   */
  public String getPublicDeviceIDString() {
    generateAndStoreDeviceID();
    return store.get(DEVICE_PUBLIC_ID_KEY);
  }

  /**
   * Extract the public ID from the contents of a QR code.
   *
   * TODO(lerner): Validate the ID more than cursorily.
   *
   * @param qrContents The full contents of a QR code (e.g. rangzen://<publicid>)
   * @return A byte[] representing the public ID stored in that QR code, or null
   * if the code was malformed/null/didn't contain an ID.
   */
  public static byte[] getPublicIDFromQR(String qrContents) {
    if (qrContents == null) { 
      return null; 
    } else if (!qrContents.startsWith(QR_FRIENDING_SCHEME)) {
      return null;
    } else {
      // TODO(lerner): Perform more aggressive validation of the ID.
      // For now we're willing to return any thing after rangzen://, which might
      // be various bad things, like...
      //   --nothing
      //   --tons of data
      //   --not the right size/format/number of bytes/parity to form a key
      // But I don't know what the spec is for these keys, so I can't verify more now.
      return base64ToBytes(qrContents.substring(QR_FRIENDING_SCHEME.length()));
    }
    
  }

}
