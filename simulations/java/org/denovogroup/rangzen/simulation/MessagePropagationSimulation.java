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

import sim.engine.Sequence;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Double2D;

import au.com.bytecode.opencsv.CSVReader;

import uk.me.jstott.jcoord.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * A MessagePropagationSimulation is a MASON SimState which represents
 * the simulation of messages moving between mobile actors in space.
 */
public abstract class MessagePropagationSimulation extends SimState {
  private static final long serialVersionUID = 1;

  public static final double METERS_PER_KILOMETER = 1000.0;
  public static final double randomMultiplier = 0.5;
    
  /** Physical space in which mobility happens. */
  public Continuous2D space;

  /** The encounter model in use. */
  Steppable encounterModel = new ProximityEncounterModel();

  /** The social network of the people in the simulation. */
  public Network socialNetwork;

  public void start() {
    super.start(); 
  }

  /**
   * Sorts the nodes by their degree in the social graph, from lowest degree to highest.
   * 
   * @param people The Bag of People to sort.
   */
  public List<Integer> orderNodesByDegree(Bag people) {
    /** takes in a bag of people in the social network, returns a list of integers with the ordering from
    smallest to largest */
    Bag friends = new Bag();
    int[] degreeArray = new int[people.numObjs];
    TreeMap<Double,Object> sorted_map = new TreeMap<Double,Object>();
        
    // characterize the degree distribution
    int count = 0;
    for (Object person : people) {
        socialNetwork.getEdges(person,friends);
        degreeArray[count] = friends.numObjs;
        count += 1;
    }
    
    // Sort and store the indices
    TreeMap<Integer, List<Integer>> map = new TreeMap<Integer, List<Integer>>();
    for(int i = 0; i < degreeArray.length; i++) {
        List<Integer> ind = map.get(degreeArray[i]);
        if(ind == null){
            ind = new ArrayList<Integer>();
            map.put(degreeArray[i], ind);
        }
        ind.add(i);
    }

    // Now flatten the list
    List<Integer> indices = new ArrayList<Integer>();
    for(List<Integer> arr : map.values()) {
        indices.addAll(arr);
    }
    return indices;
  }
  public MessagePropagationSimulation(long seed) {
    super(seed);
  }

  // public static void main(String[] args) {
  //   doLoop(MessagePropagationSimulation.class, args);
  //   System.exit(0);
  // }
}
