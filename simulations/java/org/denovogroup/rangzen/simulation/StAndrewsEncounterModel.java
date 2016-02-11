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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains the logic for checking which people have encounters
 * between them when the encounter dataset is of the St Andrews type (a list
 * of encounters, rather than spatial information.
 *
 * It includes the logic for loading/parsing St Andrews data files.
 */
public class StAndrewsEncounterModel implements Serializable {
  private static final long serialVersionUID = 1;

  private static final double encounterChance = 0.1;
  private static final int NUM_ROWS_TO_SKIP = 1;
  private static final char QUOTE_CHAR = '"';
  private static final char DELIMITER = ',';
  
  private static final int INDEX_DEVICE_1 = 0;
  private static final int INDEX_DEVICE_2 = 1;
  private static final int INDEX_ENCOUNTER_START_TIME = 2; 
  private static final int INDEX_ENCOUNTER_END_TIME = 3; 
  private static final int INDEX_ENCOUNTER_UPLOAD_TIME = 4; 
  private static final int INDEX_RSSI_VALUE = 5; 
  private static final int INDEX_ERROR_VALUE = 6; 

  private Set<StAndrewsEncounter> encounters = new HashSet<StAndrewsEncounter>();
  private StAndrewsSimulation sim;

  /**
   * Create a new encounter model, loading the data from the given filename.
   *
   * @param encounterDataFilename The file containing the St Andrews data.
   * @param sim The simulation this encounter model is a part of.
   */
  public StAndrewsEncounterModel(String encounterDataFilename, 
                                 StAndrewsSimulation sim) 
                                 throws FileNotFoundException {
    this.sim = sim;
    loadEncounterData(encounterDataFilename);
  }

  private void loadEncounterData(String filename) throws FileNotFoundException {
    System.err.println("Parsing St. Andrews Encounter Data File: " + filename);
    CSVReader reader;
    reader = new CSVReader(new FileReader(filename), 
                           DELIMITER, 
                           QUOTE_CHAR,
                           NUM_ROWS_TO_SKIP);
    String[] nextLine;
    try {
      while ((nextLine = reader.readNext()) != null) {
        int id1 = Integer.parseInt(nextLine[INDEX_DEVICE_1].trim());
        int id2 = Integer.parseInt(nextLine[INDEX_DEVICE_2].trim());
        double startTime = 
                Double.parseDouble(nextLine[INDEX_ENCOUNTER_START_TIME].trim());
        double endTime = 
                Double.parseDouble(nextLine[INDEX_ENCOUNTER_END_TIME].trim());
        double uploadTime = 
                Double.parseDouble(nextLine[INDEX_ENCOUNTER_UPLOAD_TIME].trim());
        double rssiValue = 
                Double.parseDouble(nextLine[INDEX_RSSI_VALUE].trim());
        // double errorValue = 
        //         Double.parseDouble(nextLine[INDEX_ERROR_VALUE].trim());
                
        Bag people = sim.socialNetwork.getAllNodes();
        Person p1 = null;
        Person p2 = null;
        for (int i=0; i<people.numObjs; i++) {
          Person p = (Person) people.get(i);
          if (p.name == id1) {
            p1 = p;
          }
          if (p.name == id2) {
            p2 = p;
          }
        }
        if (sim.random.nextDouble() < encounterChance) {
            if (p1 != null && p2 != null) {
              double duration = endTime - startTime;
              encounters.add(new StAndrewsEncounter(p1, p2, 
                                                    startTime, endTime,
                                                    rssiValue));
            } else {
              throw new NullPointerException("Can't instantiate encounter with null person");
            }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the set of all encounters known to this encounter model.
   *
   * @return A set of StAndrewEncounter objects, each representing one encounter.
   */
  public Set<StAndrewsEncounter> getEncounters() {
    return encounters;
  }

  private void encounter(Person p1, Person p2) {
    p1.encounter(p2);
    p2.encounter(p1);
      
  }
}
