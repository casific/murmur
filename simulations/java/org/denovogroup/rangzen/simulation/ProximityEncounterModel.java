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
import sim.util.Double2D;

import java.util.List;

/**
 * This class contains the logic for checking whether two people encounter one
 * another during a simulation based on physical location.
 */
public class ProximityEncounterModel implements Steppable {
  private static final long serialVersionUID = 1;

  public static final double NEIGHBORHOOD_RADIUS = 20;
  public static final double ENCOUNTER_CHANCE = 0.05;

  /**
   * Check for people near to one another and have them encounter each other
   * with probability determined by the ENCOUNTER_CHANCE parameter.
   */
  public void step(SimState state) {
    ProximitySimulation sim = (ProximitySimulation) state;

    Bag people = sim.socialNetwork.getAllNodes();
    for (Object p1 : people) {
        Double2D location = sim.space.getObjectLocation(p1);
        if (location != null) {
            // Check for a jammer in the jamming radius
            if (sim.mobileJamming) {
                boolean jammed = false;
                // Get all the node's neighbors, and check which ones are adversarial
                Bag neighborhood = 
                    sim.space.getNeighborsExactlyWithinDistance(location, sim.JAMMING_RADIUS);
                    for (Object p2 : neighborhood) {
                        if (((Person) p2).trustPolicy == Person.TRUST_POLICY_ADVERSARY_JAMMER) {
                            jammed = true;
                            break;
                        }
                    }
                if (jammed) {
                    continue;
                }
            } else if (sim.staticJamming) {
                boolean jammed = false;
                // compute distance to all the jammers, and treat the node as jammed if one is within radius
                for (int j=0; j<sim.jammerLocations.size(); j++) {
                    if (location.distance((Double2D) sim.jammerLocations.get(j)) < sim.JAMMING_RADIUS ) {
                        jammed = true;
                        break;
                    }
                }
                if (jammed) {
                    continue;
                }
            }
        
            // If you're not jammed, just do a regular encounter with someone in your radius
            Bag neighborhood = 
                sim.space.getNeighborsExactlyWithinDistance(location, 
                    NEIGHBORHOOD_RADIUS);
            for (Object p2 : neighborhood) {
                if (sim.random.nextDouble() < ENCOUNTER_CHANCE && p1 != p2) {
                  // System.err.println(((Person)p1).trustPolicy + " and " + ((Person)p2).trustPolicy);
                  ((Person) p1).encounter((Person) p2);
                }
            }
        }
    }

  }

  /**
   * When p1 and p2 encounter one another, we call p1.encounter(p2) and vice
   * versa.
   */
  private void encounter(Person p1, Person p2) {
    p1.encounter(p2);
    p2.encounter(p1);
      
  }
}
