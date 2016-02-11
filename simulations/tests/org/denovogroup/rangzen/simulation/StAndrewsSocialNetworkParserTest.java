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

import sim.field.network.Network;
import sim.util.Bag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Set;
import java.util.PriorityQueue;

/**
 * Tests for the Person class in the Rangzen simulations.
 */
@RunWith(JUnit4.class)
public class StAndrewsSocialNetworkParserTest {
  private static final String DATAFILE = "data/standrews/srsn.csv";
  private StAndrewsSocialNetworkParser parser;

  private int HIGHEST_PERSON_ID = 25;
  private int LOWEST_PERSON_ID = 1;
  private int[] invalidPersonIDs = {0, -1, -100, 1000, HIGHEST_PERSON_ID+1, LOWEST_PERSON_ID-1};


  @Before
  public void setUp() {
    try {
      parser = new StAndrewsSocialNetworkParser(DATAFILE, null);
    } catch (IOException e) {
      assertTrue("Couldn't read St Andrews Social Network data at " + DATAFILE,
                 false);
    }
  }

  private boolean networkContainsPersonWithID(Network network, int id) {
    Bag people = network.getAllNodes();
    for (int i=0; i<people.numObjs; i++) {
      Person p = (Person) people.get(i);
      if (p.name == id) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testNetworkContainsCorrectPeople() {
    Network network = parser.getNetwork();
    for (int i=LOWEST_PERSON_ID; i<=HIGHEST_PERSON_ID; i++) {
      assertTrue("Network does not contain personw it ID " + i,
                 networkContainsPersonWithID(network, i));
    }

    for (int j : invalidPersonIDs) {
      assertFalse("Network contains person with invalid ID " + j,
                  networkContainsPersonWithID(network, j));
    }
  }

  @Test
  public void testNetworkContainsCorrectEdges() {
     
  }

}
