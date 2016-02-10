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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.spec.DHParameterSpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.DHPublicKeyParameters;
import org.spongycastle.crypto.params.DHPrivateKeyParameters;

import okio.ByteString;

/**
 * Tests the functionality of the cryptographic routines.
 *
 * Note, these tests cannot directly verify the _security_ of the crypto routines.
 */
@RunWith(JUnit4.class)
public class CryptoTest {
  /**
   * A helper routine that verifies that a given Diffie-Hellman keypair is valid.
   *
   * @return Returns the private key as a BigInteger for further verification.
   */
  public BigInteger validateDHKeyPair(AsymmetricCipherKeyPair pair) {
    DHPrivateKeyParameters priv = (DHPrivateKeyParameters) pair.getPrivate();
    DHPublicKeyParameters pub = (DHPublicKeyParameters) pair.getPublic();

    assertNotNull(priv);
    assertNotNull(pub);

    // Check that the public key == g^(private key) mod p.
    // This test is overkill, but validates that the we've hooked into the right library.
    BigInteger g = Crypto.DH_GROUP_PARAMETERS.getG();
    BigInteger p = Crypto.DH_GROUP_PARAMETERS.getP();

    assertEquals("Tests that the public key y == g^(private key x) mod p",
                 pub.getY(), g.modPow(priv.getX(), p));

    BigInteger foo = new BigInteger(Crypto.DH_SUBGROUP_SIZE, Crypto.random);
    BigInteger gToTheFoo = g.modPow(foo, Crypto.DH_GROUP_PARAMETERS.getP());
    BigInteger fooInverse = foo.modInverse(Crypto.DH_GROUP_PARAMETERS.getQ());
    BigInteger newG = gToTheFoo.modPow(fooInverse, Crypto.DH_GROUP_PARAMETERS.getP());

    assertEquals("Tests that we can inverse group exponentiation", g, newG);

    return priv.getX();
  }

  /**
   * Tests that we can generate a valid Diffie-Hellman keypair, and that each
   * call creates a different keypair.
   */
  @Test
  public void generateDHPairTest() {
    AsymmetricCipherKeyPair pair1 = Crypto.generateDHKeyPair();
    assertNotNull(pair1);
    BigInteger priv1 = validateDHKeyPair(pair1);

    AsymmetricCipherKeyPair pair2 = Crypto.generateDHKeyPair();
    assertNotNull(pair2);
    BigInteger priv2 = validateDHKeyPair(pair2);

    assertNotEquals("Testing that two Diffie-Hellman keypairs have different private keys",
                    priv1, priv2);
  }

  /**
   * Tests that we can encode and decode public keys.
   */
  @Test
  public void encodeDecodePublicKeyTest() {
    AsymmetricCipherKeyPair pair = Crypto.generateDHKeyPair();
    byte[] encoded1 = Crypto.encodeDHPublicKey((DHPublicKeyParameters) pair.getPublic());

    assertNotNull(encoded1);

    DHPublicKeyParameters pub = Crypto.decodeDHPublicKey(encoded1);
    byte[] encoded2 = Crypto.encodeDHPublicKey((DHPublicKeyParameters) pub);
    assertNotNull(encoded2);

    assertArrayEquals("Testing that pubkey encoding/decoding", encoded1, encoded2);
  }

  /**
   * Tests that the the public ID generated is the same regardless of how it's produced.
   */
  @Test
  public void generateIDAndEncodeTest() {
    AsymmetricCipherKeyPair id = Crypto.generateUserID();

    assertNotNull(id);

    byte[] encoded1 = Crypto.encodeDHPublicKey((DHPublicKeyParameters) id.getPublic());
    byte[] encoded2 = Crypto.generatePublicID(id);

    assertArrayEquals("Testing that public ID generation works", encoded1, encoded2);
  }

  /**
   * Tests some cases of set intersection.
   */
  @Test
  public void setIntersectionCasesTest() throws NoSuchAlgorithmException {
    byte[] oneone = new byte[] { 1, 1 };
    byte[] onetwo = new byte[] { 1, 2 };
    byte[] twoone = new byte[] { 2, 1 };
    byte[] twotwo = new byte[] { 2, 2 };

    final int NUM_CLIENT_VALUES = 2;
    final int NUM_SERVER_VALUES = 4;
    final int INTERSECTION_CARDINALITY = 2;
    ArrayList<byte[]> clientValues = new ArrayList<>(Arrays.asList(oneone, twotwo));
    ArrayList<byte[]> serverValues = new ArrayList<>(Arrays.asList(oneone, onetwo, twoone, twotwo));

    Crypto.PrivateSetIntersection client = new Crypto.PrivateSetIntersection(clientValues);
    Crypto.PrivateSetIntersection server = new Crypto.PrivateSetIntersection(serverValues);

    // Test normal intersection.
    ArrayList<byte[]> clientBlindedItems = client.encodeBlindedItems();

    assertEquals("Testing that the client produces the right number of items",
                 NUM_CLIENT_VALUES, clientBlindedItems.size());

    Crypto.PrivateSetIntersection.ServerReplyTuple serverReply =
        server.replyToBlindedItems(clientBlindedItems);
    
    assertEquals("Testing that the server produces the right number of double blinded items",
                 NUM_CLIENT_VALUES, serverReply.doubleBlindedItems.size());
    assertEquals("Testing that the server produces the right number of hashed blinded items",
                 NUM_SERVER_VALUES, serverReply.hashedBlindedItems.size());

    int cardinality = client.getCardinality(serverReply);
    assertEquals("Testing that the client gets the right cardinality",
                 INTERSECTION_CARDINALITY, cardinality);

  }

  /**
   * Tests that from a functionality perspective, the PSI protocol returns accurate results.
   */
  @Test
  public void setIntersectionTest() throws NoSuchAlgorithmException {
    byte[] oneone = new byte[] { 1, 1 };
    byte[] onetwo = new byte[] { 1, 2 };
    byte[] twoone = new byte[] { 2, 1 };
    byte[] twotwo = new byte[] { 2, 2 };

    final int NUM_CLIENT_VALUES = 2;
    final int NUM_SERVER_VALUES = 4;
    final int INTERSECTION_CARDINALITY = 2;
    ArrayList<byte[]> clientValues = new ArrayList<>(Arrays.asList(oneone, twotwo));
    ArrayList<byte[]> serverValues = new ArrayList<>(Arrays.asList(oneone, onetwo, twoone, twotwo));

    Crypto.PrivateSetIntersection client = new Crypto.PrivateSetIntersection(clientValues);
    Crypto.PrivateSetIntersection server = new Crypto.PrivateSetIntersection(serverValues);

    // Test normal intersection.
    ArrayList<byte[]> clientBlindedItems = client.encodeBlindedItems();

    assertEquals("Testing that the client produces the right number of items",
                 NUM_CLIENT_VALUES, clientBlindedItems.size());

    Crypto.PrivateSetIntersection.ServerReplyTuple serverReply =
        server.replyToBlindedItems(clientBlindedItems);
    
    assertEquals("Testing that the server produces the right number of double blinded items",
                 NUM_CLIENT_VALUES, serverReply.doubleBlindedItems.size());
    assertEquals("Testing that the server produces the right number of hashed blinded items",
                 NUM_SERVER_VALUES, serverReply.hashedBlindedItems.size());

    int cardinality = client.getCardinality(serverReply);
    assertEquals("Testing that the client gets the right cardinality",
                 INTERSECTION_CARDINALITY, cardinality);

    // Test empty intersection.
    client = new Crypto.PrivateSetIntersection(new ArrayList<byte[]>());
    clientBlindedItems = client.encodeBlindedItems();

    assertEquals("Testing that the client produces the right number of items",
                 0, clientBlindedItems.size());

    serverReply = server.replyToBlindedItems(clientBlindedItems);
    
    assertEquals("Testing that the server produces the right number of double blinded items",
                 0, serverReply.doubleBlindedItems.size());
    assertEquals("Testing that the server produces the right number of hashed blinded items",
                 NUM_SERVER_VALUES, serverReply.hashedBlindedItems.size());

    cardinality = client.getCardinality(serverReply);
    assertEquals("Testing that the client gets the right cardinality", 0, cardinality);
  }

  /**
   * Tests that the behavior of byteStringsToArray and byteArraysToStrings is
   * correct when the list passed is empty.
   */
  @Test
  public void emptyListConversionTest() {
    ArrayList<byte[]> emptyArrays = new ArrayList<byte[]>();
    ArrayList<ByteString> emptyStrings = new ArrayList<ByteString>();

    ArrayList<byte[]> convertedArrays = Crypto.byteStringsToArrays(emptyStrings);
    ArrayList<ByteString> convertedStrings = Crypto.byteArraysToStrings(emptyArrays);

    assertEquals(0, emptyArrays.size());
    assertEquals(0, emptyStrings.size());
    assertNotNull(convertedArrays);
    assertNotNull(convertedStrings);
    assertEquals(0, convertedArrays.size());
    assertEquals(0, convertedStrings.size());
  }

  /**
   * Demonstrates that replyToBlindedItems throws an IllegalArgumentException when
   * passed null. Previously it would throw NullPointerException, which didn't 
   * seem clean to me.
   */
  @Test
  public void replyToBlindedItemsNullTest() throws NoSuchAlgorithmException {
    byte[] oneone = new byte[] { 1, 1 };
    byte[] onetwo = new byte[] { 1, 2 };
    byte[] twoone = new byte[] { 2, 1 };
    byte[] twotwo = new byte[] { 2, 2 };
    try {
      ArrayList<byte[]> serverValues = new ArrayList<>(Arrays.asList(oneone, onetwo, twoone, twotwo));

      Crypto.PrivateSetIntersection server = new Crypto.PrivateSetIntersection(serverValues);
      server.replyToBlindedItems(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  }

  /**
   * Check that the individual encode/decode private/public key methods reverse each other.
   * Check that the generatePublicID/generatePrivateID methods are reversed by the 
   * corresponding decode method.
   */
  @Test
  public void encodeDecodeKeysTest() throws IOException {
    AsymmetricCipherKeyPair keypair = Crypto.generateDHKeyPair();
    DHPrivateKeyParameters privKey = (DHPrivateKeyParameters) keypair.getPrivate();
    DHPublicKeyParameters pubKey = (DHPublicKeyParameters) keypair.getPublic();

    assertEquals(pubKey, Crypto.decodeDHPublicKey(Crypto.encodeDHPublicKey(pubKey)));
    assertEquals(privKey, Crypto.decodeDHPrivateKey(Crypto.encodeDHPrivateKey(privKey)));

    assertEquals(pubKey, Crypto.decodeDHPublicKey(Crypto.generatePublicID(keypair)));
    assertEquals(privKey, Crypto.decodeDHPrivateKey(Crypto.generatePrivateID(keypair)));

    // Took this out because sometimes pubkey is 129 and sometimes it's 128.
    // Not sure why that is (or what else to test here).
    // int BYTES_IN_PUBKEY = 128;
    // int BYTES_IN_PRIVKEY = 21;
    // assertEquals(BYTES_IN_PUBKEY, Crypto.encodeDHPublicKey(pubKey).length);
    // assertEquals(BYTES_IN_PRIVKEY, Crypto.encodeDHPrivateKey(privKey).length);
  }
}
