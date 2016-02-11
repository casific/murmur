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
import sim.field.continuous.Continuous2D;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Double2D;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 
 * This class represents a mobility trace datapoint with latitude, longitude
 * and datetime.
 */
public class Location implements Serializable {
  public double latitude;
  public double longitude;
  public Date date;

  /**
   * Create a new location at the given place and time.
   *
   * @param latitude The latitude as a double (negative/positive for S/N).
   * @param longitude The longitude as a double (negative/positive for W/E).
   * @param date The datetime of the location.
   */
  public Location(double latitude, double longitude, Date date) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.date = date;
  }

  /**
   * Create a new location at the given place and time.
   *
   * @param latitude The latitude as a double (negative/positive for S/N).
   * @param longitude The longitude as a double (negative/positive for W/E).
   * @param date The datetime of the location, as milliseconds after the epoch.
   */
  public Location(double latitude, double longitude, long date) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.date = new Date(date);
  }

  /**
   * Return a string representing the location nicely.
   */
  public String toString() {
    return String.format("(%f, %f) @%s", latitude, longitude, date.toString());
  }
}
