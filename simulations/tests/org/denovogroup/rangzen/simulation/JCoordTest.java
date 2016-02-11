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
 * Tests/examples of JCoord.
 */
@RunWith(JUnit4.class)
public class JCoordTest {
  public static final double METERS_PER_KILOMETER = 1000.0;
  public static final double DELTA = 5.0;
  @Before
  public void setUp() {
  }

  private void testKnownPoints(LatLng a, LatLng b, double distance) {
    assertEquals("Points a and b not the claimed distance apart.",
                 distance,
                 a.distance(b) * METERS_PER_KILOMETER,
                 DELTA);
    
  }
  @Test
  public void someKnownCrowDistances() {

    // According to Google Earth and http://www.movable-type.co.uk/scripts/latlong.html,
    // these two points are about 580 meters apart.
    LatLng a = new LatLng(37.78576, -122.40612);
    LatLng b = new LatLng(37.78779, -122.40004);

    assertEquals("Points not claimed to be 580 meters apart",
                 580.0,
                 a.distance(b) * METERS_PER_KILOMETER,
                 5.0);
  }

}
