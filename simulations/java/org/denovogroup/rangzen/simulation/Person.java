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

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.network.Edge;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * This class represents a single "person" - a mobile agent in a ProximitySimulation
 * with a mobility trace that it follows through time, communicating with
 * other nearby Persons.
 */
public class Person extends SimplePortrayal2D implements Steppable {
  private static final long serialVersionUID = 1;

  public static final String TRUST_POLICY_FRACTION_OF_FRIENDS = "FRACTION OF FRIENDS";
  public static final String TRUST_POLICY_MAX_FRIENDS = "MAX FRIENDS";
  public static final String TRUST_POLICY_SIGMOID_FRACTION_OF_FRIENDS = "SIGMOID FRACTION OF FRIENDS";
  public static final String TRUST_POLICY_ADVERSARY = "TRUST ONLY OTHER ADVERSARIES";
  public static final String TRUST_POLICY_ADVERSARY_JAMMER = "TRUST ONLY OTHER ADVERSARIES AND JAM";
  
  // simulation parameters
  /** MAX_QUEUE_LENGTH is the maximum number of messages that a node can store. This represents finite storage on a device. */
  public static final int MAX_QUEUE_LENGTH = 5;
  
  // privacy parameters
  /** MEAN is the mean amount of Gaussian noise to add to the priority scores. */
  public static final double MEAN = 0.0;    // Mean of priority noise
  /** VAR is the variance of the Gaussian noise to be added to the priority scores. */
  public static final double VAR = 0.1;     // variance of priority noise
  
  
  /** name is the ID of a node in the simulation. */
  public int name;
  /** trustPolicy is the node's policy for trusting other nodes. E.g. if it is 'adversarial', it will 
  trust only other adversarial nodes. This is set to one of the trust policy strings defined above. */
  public String trustPolicy;
  
  /** mobilityTrace is the mobility trace to use for node mobility. This is set to one of the mobility 
  trace strings defined above. */
  public MobilityTrace mobilityTrace;
  // private Iterator<Location> mobilityIterator;
  private int nextStepIndex = 0;

  private MessagePropagationSimulation sim;

  public Map<Person, Integer> encounterCounts = new HashMap<Person, Integer>();

  /** The message queue for the node. */
  public PriorityQueue<Message> messageQueue = new PriorityQueue<Message>();

  /** The next location to step to. */
  private Location nextStep;
  

  /** Create a new Person with the given "name", trust policy, and as a part
   * of the given simulation.
   *
   * @param name An integer naming this person.
   * @param trustPolicy One of the trust policies listed above as static Strings.
   * @param sim The simulation this person is a part of.
   */
  public Person(int name, String trustPolicy, MessagePropagationSimulation sim) {
    this.name = name;
    this.sim = sim;
    this.trustPolicy = trustPolicy;
    
    // initialize the message queue
    /** Comment this for loop to simulate infinite capacity */
    for (int i=0; i<MAX_QUEUE_LENGTH; i++) {
      double p = sim.random.nextDouble();
      addMessageToQueue(new Message(UUID.randomUUID().toString(), p));
    }
  }

  /**
   * A person's step() method makes them move to the next position in their
   * mobility trace.
   */
  public void step(SimState state) {
    // MessagePropagationSimulation sim = (MessagePropagationSimulation) state;
    // takeRandomStep(sim);
    takeMobilityTraceStep();
  }

  /**
   * Schedule the person once for each point in their mobility trace. For example,
   * if they have the points (x, y, 5000) and (x', y', 6000), they'll be scheduled
   * at times 5000 and 6000.
   */
  public void schedule() {
    for (Location location : mobilityTrace) {
      long time = location.date.getTime();
      // sim.schedule.scheduleOnce(time, meAndMeasurer);
      ((ProximitySimulation) sim).schedulePerson(this, time);
    }
  }
    
  /**
   * Get the next location in the mobility trace and move there.
   */
  private void takeMobilityTraceStep() {
    ProximitySimulation proxSim = (ProximitySimulation) sim;
    if (nextStep != null) {
      proxSim.setObjectLatLonLocation(this, nextStep); 
      nextStepIndex++;
      if (nextStepIndex < mobilityTrace.locations.size()) {
        // nextStep = mobilityIterator.next();
        nextStep = mobilityTrace.locations.get(nextStepIndex);
      }
      else {
        nextStep = null;
      }
    }
  }
  
  private void takeRandomStep(MessagePropagationSimulation sim) {
    Double2D me = sim.space.getObjectLocation(this);
    MutableDouble2D sumForces = new MutableDouble2D();
    sumForces.addIn(new Double2D(sim.randomMultiplier * (sim.random.nextInt(5)-2 * 1.0),
          sim.randomMultiplier * (sim.random.nextInt(5)-2 * 1.0)));

    sumForces.addIn(me);
    sim.space.setObjectLocation(this, new Double2D(sumForces));

  }

  public boolean queueHasMessageWithContent(String soughtMessage) {
    if (soughtMessage == null) {
      return false;
    }
    for (Message m : messageQueue) {
      if (soughtMessage.equals(m.content)) {
        return true;
      }
    }
    return false;
  }
  public boolean queueHasMessageWithContent(Message message) {
    if (message == null) {
      return false;
    }
    else {
      return queueHasMessageWithContent(message.content);
    }
  }

  public void putMessages(PriorityQueue<Message> newMessages, Person sender) {
    Set<Object> sharedFriends = sender.getSharedFriends(this);
    for (Object friend : sharedFriends) {
      // System.out.print(name+"/"+sender+": "+friend + ", ");
    }
    // System.out.println();
    int otherName = sender.name;
    // double trustMultiplier = 
    //       sharedFriends.size() / ProximitySimulation.MAX_FRIENDS;
    // if (sharedFriends.size() == 0) {
    //   trustMultiplier = ProximitySimulation.EPSILON_TRUST;
    // }
    for (Message m : newMessages) {
      // if (!messageQueue.contains(m)) {
      if (!queueHasMessageWithContent(m)) {
        Message copy = m.clone();
        copy.priority = computeNewPriority(m.priority, sharedFriends.size(), getFriends().size(),sender);
        addMessageToQueue(copy);
        // System.out.println(name+"/"+otherName+": "+messageQueue.peek());
      }
    }
  }
  
  public void addMessageToQueue(Message m) {
    messageQueue.add(m);
    while (messageQueue.size() > MAX_QUEUE_LENGTH) {
      Message popped = messageQueue.poll();
      // System.err.println("Priority of popped message: " + popped.priority);
    }
    Message bottom = messageQueue.peek();
    // System.err.println("Priority of next message: " + bottom.priority);
  }

  public double computeNewPriority(double priority, 
                                   int sharedFriends, 
                                   int myFriends,
                                   Person sender) {
    if (trustPolicy == TRUST_POLICY_FRACTION_OF_FRIENDS) {
      return computeNewPriority_fractionOfFriends(priority, sharedFriends, myFriends);
    }
    else if (trustPolicy == TRUST_POLICY_MAX_FRIENDS) {
      return computeNewPriority_maxFriends(priority, sharedFriends, myFriends);
    }
    else if (trustPolicy == TRUST_POLICY_SIGMOID_FRACTION_OF_FRIENDS) {
      return computeNewPriority_sigmoidFractionOfFriends(priority, sharedFriends, myFriends);
    }
    else if (trustPolicy == TRUST_POLICY_ADVERSARY) {
      return computeNewPriority_adversary(priority, sharedFriends, myFriends,sender);
    }
    else {
      return computeNewPriority_maxFriends(priority, sharedFriends, myFriends);
    }
  }

  /** Compute the priority score for a person normalized by the maximum number of possible friends.
   *
   * @param priority The priority of the message before computing trust.
   * @param sharedFriends Number of friends shared between this person and the message sender.
   * @param myFriends The number of friends this person has.
   */
  public static double computeNewPriority_maxFriends(double priority,
                                                     int sharedFriends, 
                                                     int myFriends) {
    double trustMultiplier =  
            sharedFriends / (double) ProximitySimulation.MAX_FRIENDS;
    if (sharedFriends == 0) {
          trustMultiplier = ProximitySimulation.EPSILON_TRUST;
    }
    return priority * trustMultiplier;
  } 
  
  /** Compute the priority score for a person normalized by his number of friends.
   *
   * @param priority The priority of the message before computing trust.
   * @param sharedFriends Number of friends shared between this person and the message sender.
   * @param myFriends The number of friends this person has.
   */
  public static double computeNewPriority_fractionOfFriends(double priority,
                                                            int sharedFriends, 
                                                            int myFriends) {
    double trustMultiplier = sharedFriends / (double) myFriends;
    if (sharedFriends == 0) {
          trustMultiplier = ProximitySimulation.EPSILON_TRUST;
    }
    return priority * trustMultiplier;
  } 

  /** Compute the priority score for a person normalized by his number of friends, and passed
   * through a sigmoid function.
   *
   * @param priority The priority of the message before computing trust.
   * @param sharedFriends Number of friends shared between this person and the message sender.
   * @param myFriends The number of friends this person has.
   */
  public static double computeNewPriority_sigmoidFractionOfFriends(double priority,
                                                            int sharedFriends,
                                                            int myFriends) {
    double trustMultiplier = sigmoid(sharedFriends / (double) myFriends, 0.3, 13.0);
    // add noise
    trustMultiplier = trustMultiplier + getGaussian(MEAN,VAR);
    
    // truncate range
    trustMultiplier = Math.min(trustMultiplier,1);
    trustMultiplier = Math.max(trustMultiplier,0);
    
    if (sharedFriends == 0) {
          trustMultiplier = ProximitySimulation.EPSILON_TRUST;
    }
    return priority * trustMultiplier;
  }

  /** Pass an input trust score through a sigmoid between 0 and 1, and return the result.
   *
   * @param input The input trust score.
   * @param cutoff The transition point of the sigmoid.
   * @param rate The rate at which the sigmoid grows.
   */
  public static double sigmoid(double input, double cutoff, double rate) {
    return 1.0/(1+Math.pow(Math.E,-rate*(input-cutoff)));
  }
  
  public static double computeNewPriority_adversary(double priority,
                                                            int sharedFriends,
                                                            int myFriends,
                                                            Person sender) {
    // the adversary accepts nobody's messages except for other those of other adversaries
    if (sender.trustPolicy == TRUST_POLICY_ADVERSARY) {
        return 1.0;
    } else {
        return 0.0;
    }
  }

  /** Conduct an encounter with another person, involving exchanging messages and computing trust.
   *
   * @param other The person whom we should encounter.
   */
  public void encounter(Person other) {
    Integer count = encounterCounts.get(other);
    if (count == null) {
      encounterCounts.put(other, 1);
    }
    else {
      count++;
      encounterCounts.put(other, count);
    } 
    other.putMessages(messageQueue, this);
  }

  /** Return a set containing the current person's friends in the social graph.
   */
  public Set<Person> getFriends() {
    Bag myEdges = sim.socialNetwork.getEdges(this, null);
    Set<Person> friends = new HashSet<Person>();

    for (Object e1 : myEdges) {
      Person from = (Person) ((Edge) e1).from();
      Person to = (Person) ((Edge) e1).to();

      if (from == this) {
        friends.add(to);
      } else {
        friends.add(from);
      }
    }
    return friends;
  }

  /** Return a set containing the common friend with "other". This would be handled by a 
   * PSI operation in practice.
   *
   * @param other The person with whom we should compute mutual friends.
   */
  public Set<Object> getSharedFriends(Person other) {
    Bag myEdges = sim.socialNetwork.getEdges(this, null);
    Bag otherEdges = sim.socialNetwork.getEdges(other, null);

    Set<Object> sharedFriends = new HashSet<Object>();
    for (Object e1 : myEdges) {
      for (Object e2 : otherEdges) {
        // There has to be some way to do this more elegantly?
        Object myFrom = ((Edge) e1).from();
        Object myTo = ((Edge) e1).to();
        Object otherFrom = ((Edge) e2).from();
        Object otherTo = ((Edge) e2).to();

        Object myFriend = (myFrom == this) ? myTo : myFrom;
        Object otherFriend = (otherFrom == other) ? otherTo : otherFrom;

        // System.out.println(myFrom + " " + myTo + " " + otherFrom + " " + otherTo);
        if (myFriend == otherFriend) {
          sharedFriends.add(myFriend);
        }
      }
    }
    return sharedFriends;
  }

  /** Add a mobility trace to the person's schedule.
   *
   * @param filename The file name for the mobility trace of this person.
   */
  public void addMobilityTrace(String filename) throws FileNotFoundException {
    this.mobilityTrace = new MobilityTrace(filename);
    // this.mobilityIterator = mobilityTrace.iterator();
    // if (mobilityIterator.hasNext()) {
    if (mobilityTrace.locations.size() > 0) {
      // nextStep = mobilityIterator.next();
      nextStep = mobilityTrace.locations.get(nextStepIndex);
      nextStepIndex++;
    }
  }
  
  /** Add a mobility trace to the person's schedule.
   *
   * @param mobilityTrace The mobilityTrace object for the mobility trace of this person.
   */
  public void addMobilityTrace(MobilityTrace mobilityTrace) {
    this.mobilityTrace = mobilityTrace;
  }

  /** Returns a string version of this person's name.
   */
  public String toString() {
    return "" + name;
  }
 
  /** Returns a number drawn from a Gaussian distribution determined by the input parameters.
   *
   * @param mean The mean of the Gaussian distribution.
   * @param variance The variance of the Gaussian distribution.
   */ 
  private static double getGaussian(double mean, double variance){
    Random fRandom = new Random();
    return mean + fRandom.nextGaussian() * Math.sqrt(variance);
  }

  protected Color noMessageColor = new Color(0,0,0);
  protected Color messageColor = new Color(255,0,0);
  public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
    // double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
    // double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;
    double diamx = 10;
    double diamy = 10;

    if (messageQueue.isEmpty()) {
      graphics.setColor( noMessageColor );            
    }
    else { 
      graphics.setColor( messageColor );            
    }
    graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
      
    // graphics.setColor( goodMarkColor );
    // graphics.fillRect((int)(info.draw.x-diamx/3),(int)(info.draw.y-diamy/16),(int)(diamx/1.5),(int)(diamy/8));
    // graphics.fillRect((int)(info.draw.x-diamx/16),(int)(info.draw.y-diamy/3),(int)(diamx/8),(int)(diamy/1.5));
  }
}
