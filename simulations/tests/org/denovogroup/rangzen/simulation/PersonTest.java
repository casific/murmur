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
package org.denovogroup.rangzen.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;
import java.util.PriorityQueue;

/**
 * Tests for the Person class in the Rangzen simulations.
 */
@RunWith(JUnit4.class)
public class PersonTest {
  private static final double MESSAGE_PRIORITY = 1.0;
  private static final String MESSAGE_CONTENT = "a message";
  MessagePropagationSimulation sim;
  Person person;
  Person otherPerson;
  Message message;

  @Before
  public void setUp() {
    sim = new MessagePropagationSimulation(System.currentTimeMillis());
    sim.start();
    person = new Person(0, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    otherPerson = new Person(100, Person.TRUST_POLICY_MAX_FRIENDS, sim);

    message = new Message(MESSAGE_CONTENT, MESSAGE_PRIORITY);
  }

  /**
   * Ensure that get friends returns all the friends in the graph.
   */
  @Test
  public void testGetFriends() {
    Person p1 = new Person(1, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p2 = new Person(2, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p3 = new Person(3, Person.TRUST_POLICY_MAX_FRIENDS, sim);

    sim.socialNetwork.addEdge(person, p1, new Double(1.0));
    sim.socialNetwork.addEdge(person, p2, new Double(1.0));

    Set<Person> friends = person.getFriends(); 

    assertTrue("Added friend not in set returned by getFriends()", friends.contains(p1));
    assertTrue("Added friend not in set returned by getFriends()", friends.contains(p2));
    assertFalse("Non-added friend in set returned by getFriends()", friends.contains(p3));

  }

  @Test
  public void testFriendIntersection() {
    Person p1 = new Person(1, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p2 = new Person(2, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p3 = new Person(3, Person.TRUST_POLICY_MAX_FRIENDS, sim);

    sim.socialNetwork.addEdge(person, p1, new Double(1.0));
    sim.socialNetwork.addEdge(person, p2, new Double(1.0));
    sim.socialNetwork.addEdge(otherPerson, p1, new Double(1.0));

    Set<Object> sharedFriends = person.getSharedFriends(otherPerson);

    assertTrue("Shared friend not in set from getSharedFriends()", sharedFriends.contains(p1));
    assertFalse("Non-shared friend in set from getSharedFriends()", sharedFriends.contains(p2));
  }

  private boolean queueContainsMessage(PriorityQueue<Message> messages, Message soughtMessage) {
    for (Message m : messages) {
      if (m.equals(soughtMessage)) {
        return true;
      } 
    }
    return false;
  }

  @Test
  public void testFriendIntersectionWithWeirdThings() {
    Person p1 = new Person(1, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p2 = new Person(2, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p3 = new Person(3, Person.TRUST_POLICY_MAX_FRIENDS, sim);
    Person p4 = new Person(4, Person.TRUST_POLICY_MAX_FRIENDS, sim);

    // Add person <-> p1, person <-> p2, person <-> p3
    // and
    // otherPerson <-> p1, otherPerson <-> p2
    sim.socialNetwork.addEdge(person, p1, new Double(1.0));
    sim.socialNetwork.addEdge(person, p2, new Double(1.0));
    sim.socialNetwork.addEdge(p1, person, new Double(1.0));
    sim.socialNetwork.addEdge(p2, person, new Double(1.0));
    sim.socialNetwork.addEdge(person, p3, new Double(1.0));
    sim.socialNetwork.addEdge(p3, person, new Double(1.0));
     
    sim.socialNetwork.addEdge(otherPerson, p1, new Double(1.0));
    sim.socialNetwork.addEdge(otherPerson, p2, new Double(1.0));
    sim.socialNetwork.addEdge(p1, otherPerson, new Double(1.0));
    sim.socialNetwork.addEdge(p2, otherPerson, new Double(1.0));
    sim.socialNetwork.addEdge(p3, otherPerson, new Double(1.0));

    Set<Object> sharedFriends = person.getSharedFriends(otherPerson);

    assertTrue("Shared friend not in set from getSharedFriends() after adding twice.", sharedFriends.contains(p1));
    assertTrue("Shared friend not in set from getSharedFriends() after adding twice.", sharedFriends.contains(p2));
    assertTrue("Shared friend not in set from getSharedFriends() after adding twice.", sharedFriends.contains(p3));
    assertFalse("Non-shared friend in set from getSharedFriends() after adding others twice.", sharedFriends.contains(p4));

    // Add redundant edges, see what happens.
    sim.socialNetwork.addEdge(person, p1, new Double(1.0));
    sim.socialNetwork.addEdge(person, p1, new Double(1.0));
    sim.socialNetwork.addEdge(person, p1, new Double(1.0));

    assertTrue("Shared friend not in set from getSharedFriends() after adding a bunch of times.", sharedFriends.contains(p1));
    assertTrue("Shared friend not in set from getSharedFriends() after adding ta bunch of times", sharedFriends.contains(p2));
    assertTrue("Shared friend not in set from getSharedFriends() after adding ta bunch of times", sharedFriends.contains(p3));
    assertFalse("Non-shared friend in set from getSharedFriends() after adding others twice.", sharedFriends.contains(p4));
  }

  @Test
  public void testPutMessage() {
    person.messageQueue.add(message);
    otherPerson.putMessages(person.messageQueue, person);

    assertTrue("putMessage() didn't transfer message.", 
               queueContainsMessage(otherPerson.messageQueue, message));
  }

  @Test
  public void testPutMessagePriorityZeroFriends() {
    person.messageQueue.add(message);
    otherPerson.putMessages(person.messageQueue, person);

    assertEquals("0 friends message transfer didn't result in epsilon trust",
               otherPerson.messageQueue.peek().priority,
               MESSAGE_PRIORITY * MessagePropagationSimulation.EPSILON_TRUST,
               0.00001);
  }

  @Test
  public void testPutMessagePriorityWithKFriends() {
    for (int k=1; k <= MessagePropagationSimulation.MAX_FRIENDS; k++) {
      // New persons each time.
      person = new Person(1000, Person.TRUST_POLICY_MAX_FRIENDS, sim);
      otherPerson = new Person(10001, Person.TRUST_POLICY_MAX_FRIENDS, sim);
      sim.socialNetwork.clear();

      for (int i=0; i<k; i++) {
        Person pk = new Person(1001+i, Person.TRUST_POLICY_MAX_FRIENDS, sim);

        sim.socialNetwork.addEdge(person, pk, new Double(1.0));
        sim.socialNetwork.addEdge(otherPerson, pk, new Double(1.0));
      }

      person.messageQueue.add(message);
      otherPerson.putMessages(person.messageQueue, person);
      assertEquals("Priority didn't work out for " + k + " friends.",
                  otherPerson.messageQueue.peek().priority,
                  MESSAGE_PRIORITY * (k / (double) MessagePropagationSimulation.MAX_FRIENDS),
                  0.00001);
    } 
  }

  @Test
  public void testPutMessageMultiplePriorityZeroFriends() {
    person.messageQueue.add(message);
    person.messageQueue.add(new Message("second message", 1.0));
    person.messageQueue.add(new Message("third message", 1.0));

    otherPerson.putMessages(person.messageQueue, person);

    for (Message m : otherPerson.messageQueue) {
      assertEquals("0 friends message transfer didn't result in epsilon trust",
                   m.priority,
                   MESSAGE_PRIORITY * MessagePropagationSimulation.EPSILON_TRUST,
                   0.00001);
    }
  }

  @Test
  public void testPutMessageMultiplePriorityWithKFriends() {
    for (int k=1; k <= MessagePropagationSimulation.MAX_FRIENDS; k++) {
      // New persons each time.
      person = new Person(1000, Person.TRUST_POLICY_MAX_FRIENDS, sim);
      otherPerson = new Person(10001, Person.TRUST_POLICY_MAX_FRIENDS, sim);
      sim.socialNetwork.clear();

      for (int i=0; i<k; i++) {
        Person pk = new Person(1001+i, Person.TRUST_POLICY_MAX_FRIENDS, sim);

        sim.socialNetwork.addEdge(person, pk, new Double(1.0));
        sim.socialNetwork.addEdge(otherPerson, pk, new Double(1.0));
      }

      person.messageQueue.add(message);
      person.messageQueue.add(new Message("second message", 1.0));
      person.messageQueue.add(new Message("third message", 1.0));
      otherPerson.putMessages(person.messageQueue, person);
      for (Message m : otherPerson.messageQueue) {
        assertEquals("Priority didn't work out for " + k + " friends.",
                     m.priority,
                     MESSAGE_PRIORITY * (k / (double) MessagePropagationSimulation.MAX_FRIENDS),
                     0.00001);
      }
    } 
  }

  @Test
  public void testMaxFriendsPrioritizationPolicy() {
    for (int sharedFriends = 1; sharedFriends<MessagePropagationSimulation.MAX_FRIENDS; sharedFriends++) {
      assertEquals(
          "Max friends policy wrong with " + sharedFriends + " shared friends",
          Person.computeNewPriority_maxFriends(MESSAGE_PRIORITY, sharedFriends, sharedFriends),
          MESSAGE_PRIORITY * (sharedFriends / (double) MessagePropagationSimulation.MAX_FRIENDS),
          0.000001);
    }
  }

  @Test
  public void testFractionOfFriendsPrioritizationPolicy() {
    for (int sharedFriends = 1; sharedFriends < MessagePropagationSimulation.MAX_FRIENDS; sharedFriends++) {
      for (int myFriends = sharedFriends; myFriends < MessagePropagationSimulation.MAX_FRIENDS; myFriends++) {
        double newPriority = Person.computeNewPriority_fractionOfFriends(MESSAGE_PRIORITY, 
            sharedFriends, 
            myFriends);
        assertEquals("Fraction of friends policy wrong with " + sharedFriends + " / " + myFriends + " friends.",
            newPriority,
            MESSAGE_PRIORITY * (sharedFriends / (double) myFriends),
            0.000001);
      }
    }
  }


  /**
   * This test shouldn't show up as a pass or a fail in results, since it is
   * ignored.
   */
  @Test
  @Ignore
  public void thisIsIgnored() {
  }

}
