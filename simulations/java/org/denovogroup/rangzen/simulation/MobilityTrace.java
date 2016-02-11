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

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents an entire mobility trace for a single agent in a
 * ProximitySimulation.
 */
public class MobilityTrace implements Iterable<Location>, Serializable {

  public List<Location> locations;
  public static final int INDEX_LATITUDE = 0;
  public static final int INDEX_LONGITUDE = 1;
  public static final int INDEX_DATE = 3;

  /**
   * Create a new mobility trace using the given list of locations.
   *
   * @param locations A list of locations in the order the agent was there.
   */
  public MobilityTrace(List<Location> locations) {
    this.locations = locations;
  }
  
  /**
   * Create a new mobility trace using the given list of locations.
   *
   * @param locations The filename of a CSV file containing locations in
   * Cabspotting's format (http://cabspotting.org/).
   */
  public MobilityTrace(String filename) throws FileNotFoundException {
    this.locations = parseLocationsFile(filename);
  }

  /**
   * Return an iterator over the mobility trace's list of locations.
   */
  public Iterator<Location> iterator() {
    return locations.iterator();
  }

  /**
   * Parse the given filename and return a list of locations, in the order
   * given in the file.
   *
   * @param filename The file to be parsed.
   * @return The list of locations contained in the file, or null in the case
   * of an exception.
   */ 
  private List<Location> parseLocationsFile(String filename) throws FileNotFoundException {
    System.err.println("Parsing " + filename);
    char DELIMITER = ' ';
    CSVReader reader;
    reader = new CSVReader(new FileReader(filename), DELIMITER);
    String [] nextLine;
    List<Location> locations = new ArrayList<Location>();
    try {
      while ((nextLine = reader.readNext()) != null) {
        double lat = Double.parseDouble(nextLine[INDEX_LATITUDE]);
        double lon = Double.parseDouble(nextLine[INDEX_LONGITUDE]);
        long date = Long.parseLong(nextLine[INDEX_DATE]) * 1000;
        Location location = new Location(lat, lon, date);
        locations.add(location);
      }
    } catch (IOException e) {
      return null;
    }
    return locations;

  }
}
