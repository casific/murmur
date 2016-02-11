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
import sim.util.Bag;

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a single encounter that occurs in a dataset like the
 * St Andrews dataset, where the data contains an abstract list of people and
 * the people they encounter over time, rather than spatial data.
 */
public class StAndrewsEncounter implements Steppable {
  private static final long serialVersionUID = 1;

  public Person p1;
  public Person p2;
  public double startTime;
  public double endTime;
  public double duration;
  public double rssi;
  
  /**
   * Create a new encounter between the given people, lasting between the
   * given times (as epoch times), with the given signal strength.
   *
   * @param p1 One person in the encounter.
   * @param p2 The other person in the encounter.
   * @param startTime The epoch time at which the encounter started.
   * @param endTime The epoch time at which the encounter ended.
   * @param rssi A value representing the signal strength/SNI of the encounter
   * (unused in these simulations).
   *
   */
  public StAndrewsEncounter(Person p1, Person p2, 
                            double startTime, double endTime,
                            double rssi) {
    this.p1 = p1;
    this.p2 = p2;
    this.startTime = startTime;
    this.endTime = endTime;
    this.rssi = rssi;

    this.duration = endTime - startTime;
  }

  public void step(SimState state) {
    p1.encounter(p2);
    p2.encounter(p1);
  }

}
