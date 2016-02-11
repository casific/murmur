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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a MessagePropagationSimulation where the dataset is not spatial -
 * rather, it is a list of pairs of people who had an encounter with the time
 * that they ran into each other, as in the St Andrews dataset.
 * (http://crawdad.cs.dartmouth.edu/st_andrews/sassy/)
 */
public class StAndrewsSimulation extends MessagePropagationSimulation {
  private static final long serialVersionUID = 1;

  /** The agent which measures the simulation and reports statistics on it. */
  public Steppable measurer = new SingleMessageTrackingMeasurer(this);

  public static final String ST_ANDREWS_SOCIAL_NETWORK_FILENAME = 
          "data/standrews/srsn.csv";
  public static final String ST_ANDREWS_ENCOUNTER_FILENAME = 
          "data/standrews/dsn.csv";

  private StAndrewsSocialNetworkParser parser;
  private StAndrewsEncounterModel saEncounterModel;
  
  // Adversary info!
  public static final String RANDOM_AUTHOR = "Random author";
  public static final String ADVERSARIAL_AUTHOR = "Adversarial author";
  public static final String POPULAR_AUTHOR = "(Un)popular author";
  
  public static final int NUMBER_OF_ADVERSARIES = 0;
  public static String messageAuthor = RANDOM_AUTHOR;
  public static boolean popularAuthor = false;

  /**
   * Called at the start of the simulation. Parses datafiles, creates people,
   * etc.
   */
  public void start() {
    super.start(); 
    try {
      parser = new StAndrewsSocialNetworkParser(ST_ANDREWS_SOCIAL_NETWORK_FILENAME, 
                                               this);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    socialNetwork = parser.getNetwork();
    try {
      saEncounterModel = new StAndrewsEncounterModel(ST_ANDREWS_ENCOUNTER_FILENAME,
                                                   this);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    Set<StAndrewsEncounter> encounters =
            ((StAndrewsEncounterModel) saEncounterModel).getEncounters();
    for (StAndrewsEncounter encounter : encounters) {
      Steppable[] encounterAndMeasurer = new Steppable[2];
      encounterAndMeasurer[0] = encounter;
      encounterAndMeasurer[1] = measurer;
      Sequence s = new Sequence(encounterAndMeasurer);
      schedule.scheduleOnce(encounter.startTime, s);
    }

    schedule.scheduleOnce(measurer);     
  }

  /**
   * At the end of the simulation, a JSON containing the data gathered during
   * the run about message propagation is output on standard out.
   */
  public void finish() {
    String jsonOutput = ((SingleMessageTrackingMeasurer) measurer).getMeasurementsAsJSON();
    System.out.println(jsonOutput);
  }

  /** 
   * Create a new StAndrewsSimulation with the given randomization seed.
   */
  public StAndrewsSimulation(long seed) {
    super(seed);
  }

  public static void main(String[] args) {
    doLoop(StAndrewsSimulation.class, args);
    System.exit(0);
  }
}
