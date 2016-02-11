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

import uk.me.jstott.jcoord.LatLng;
 
import sim.util.Double2D;

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
 * Tests of methods from MessagePropagationSimulation.
 */
@RunWith(JUnit4.class)
public class MessagePropagationSimulationTest {
  private MessagePropagationSimulation sim;
  private double DELTA = 1;
  private double OPPOSITE_CORNER_Y = 0;
  private double OPPOSITE_CORNER_X = 26300;
  private static final double METERS_PER_KILOMETER = 1000;

  @Before
  public void setUp() {
    sim = new MessagePropagationSimulation(System.currentTimeMillis());
  }

  @Test
  public void coordinateTransformTest() {
    Location originLocation = 
            new Location(MessagePropagationSimulation.LOWEST_LATITUDE, 
                         MessagePropagationSimulation.LOWEST_LONGITUDE, 
                         0);
    Double2D origin = sim.translateLatLonToSimCoordinates(originLocation);
    assertEquals("Lowest lon not at x=0 meters", origin.x, 0, DELTA);
    assertEquals("Lowest lat not at y=0 meters", 
                 origin.y,
                 MessagePropagationSimulation.height,
                 DELTA);

    Location oppositeCornerLocation = 
            new Location(MessagePropagationSimulation.HIGHEST_LATITUDE, 
                         MessagePropagationSimulation.HIGHEST_LONGITUDE, 
                         0);
    Double2D oppositeCorner =
           sim.translateLatLonToSimCoordinates(oppositeCornerLocation);
    // Margin of error of 100 meters because the tool used to calculate
    // the "correct" value only gives precision out to 100 meters.
    // (http://www.movable-type.co.uk/scripts/latlong.html)
    assertEquals("Highest lon not at correct meter location", 
                 OPPOSITE_CORNER_X,
                 oppositeCorner.x,
                 100);
    assertEquals("Highest lat not at correct meter location", 
                 OPPOSITE_CORNER_Y,
                 oppositeCorner.y, 
                 100);
  }
}
