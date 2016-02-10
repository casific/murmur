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

import com.squareup.wire.Wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests that demonstrate that Wire is integrated with our code base.
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18,
        resourceDir="../../ui/Rangzen/res/")
@RunWith(RobolectricTestRunner.class)
public class WireTest {
  private final static String name = "A NAME";
  private final static Integer id = 123;

  /** Output stream to echo objects to ourselves. */
  private PipedOutputStream outputStream;
  /** Input stream to hear echoed objects. */
  private PipedInputStream inputStream;

  /** Instance of Wire to deal with deserialization. */
  private Wire wire;

  /** An instance of a demo Message, Person, to test things with. */
  private Person person;

  /** An instance of CleartextFriends, a type of message. */
  private CleartextFriends cleartextFriends;

  /** A list of friends to include in CleartextFriends. */
  private List<String> friends = new ArrayList<String>();

  /** An instance of CleartextMessages, a type of message. */
  private CleartextMessages cleartextMessages;

  /** An empty list of friends. */
  private CleartextFriends nullFriends;

  /** A list of friends to include in CleartextMessages. */
  private List<RangzenMessage> messages = 
                        new ArrayList<RangzenMessage>();

  @Before
  public void setup() throws IOException {
    wire = new Wire();
    outputStream = new PipedOutputStream();
    inputStream = new PipedInputStream();
    outputStream.connect(inputStream); 
    person = new Person.Builder()
                              .id(id)
                              .name(name)
                              .build();

    friends.add("foo");
    friends.add("bar");
    cleartextFriends = new CleartextFriends.Builder()
                                           .friends(friends)
                                           .build();
    nullFriends = new CleartextFriends.Builder()
                                           .friends(new ArrayList<String>())
                                           .build();
                                          
    messages.add(new RangzenMessage.Builder()
                                   .text("foo")
                                   .priority(0.25)
                                   .build());
    messages.add(new RangzenMessage.Builder()
                                   .text("bar")
                                   .priority(0.75325)
                                   .build());
    cleartextMessages = new CleartextMessages.Builder()
                                             .messages(messages)
                                             .build();
  }

  /**
   * Test that creating a person actually sets its fields, and they're retrievable.
   */
  @Test
  public void testCreatePersonWorked() {
    assertEquals(name, person.name);
    assertEquals(id, person.id);
  }

  /**
   * Test the ability to serialize and deserialize a protobuf object to and
   * from a byte array.
   */
  @Test 
  public void serializeDeserializePerson() throws IOException {
    Person recoveredPerson = wire.parseFrom(person.toByteArray(), Person.class);
    assertEquals(name, recoveredPerson.name);
    assertEquals(id, recoveredPerson.id);
    assertEquals(person, recoveredPerson);
  }

  /** 
   * Test that we can read a protobuf object over a stream ONLY IF the object
   * is the entire stream (stream closes after object).
   */
  @Test
  public void sendPersonOverStream() throws IOException {
    outputStream.write(person.toByteArray());
    outputStream.close();
    Person recoveredPerson = wire.parseFrom(inputStream, Person.class);
    assertEquals(name, recoveredPerson.name);
    assertEquals(id, recoveredPerson.id);
    assertEquals(person, recoveredPerson);
  }

  @Test
  public void sendPersonOverDelimitedStream() throws IOException {
    Exchange.lengthValueWrite(outputStream, person);
    Person recoveredPerson = Exchange.lengthValueRead(inputStream, Person.class);
    assertEquals(name, recoveredPerson.name);
    assertEquals(id, recoveredPerson.id);
    assertEquals(person, recoveredPerson);
  }

  /**
   * Ensure that Exchange.lengthValueWrite claims failure when the output stream
   * or the Message is null. (Or both).
   */
  @Test
  public void delimitedWriteEdgeCases() throws IOException {
    assertFalse(Exchange.lengthValueWrite(null, null));
    assertFalse(Exchange.lengthValueWrite(null, person));
    assertFalse(Exchange.lengthValueWrite(outputStream, null));
    
    // Closed output stream will generate an IOException when the method
    // tries to write to it, resulting in a failed write.
    outputStream.close();
    assertFalse(Exchange.lengthValueWrite(outputStream, person));
  }

  /**
   * Check that CleartextFriends works on the most basic level.
   */
  @Test
  public void testCleartextFriends() {
    assertEquals(cleartextFriends.friends, friends);

    assertEquals(nullFriends.friends, new ArrayList<String>());
  }

  /**
   * Check that CleartextMessages works on the most basic level.
   */
  @Test
  public void testCleartextMessages() {
    List<RangzenMessage> recoveredMessages = cleartextMessages.messages;
    assertEquals(recoveredMessages, messages);
  }

  /** Expected size of an int is 4 bytes. */
  private static final int sizeOfIntExpected = 4 * Byte.SIZE;
  /** Expected size of a byte is 8 bits. */
  private static final int sizeOfByteExpected = 8;
  
  /**
   * We expect that every system this runs on thinks an Integer is 4 bytes, or 
   * 32 bits, because all Java implementations do. But my understanding is that
   * they technically don't have to.
   *
   * This is mostly just making our assumptions explicit, since our wire format
   * (length, value encoding) relies on writing an int to the wire.
   */
  @Test
  public void testSizeOfInt() {
    assertEquals(sizeOfIntExpected, Integer.SIZE);
    assertEquals(sizeOfByteExpected, Byte.SIZE);
  }
}
