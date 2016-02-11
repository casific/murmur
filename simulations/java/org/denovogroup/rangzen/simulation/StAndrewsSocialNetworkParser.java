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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A utility class that contains the logic for parsing St Andrews style data.
 *
 * @see StAndrewsSimulation
 */
public class StAndrewsSocialNetworkParser implements Serializable {

  private Map<Integer, Person> people = new HashMap<Integer, Person>();
  private Network network = new Network(UNDIRECTED);
  private MessagePropagationSimulation sim;
  private char delimiter;
  private int rowsToSkip;

  public static final boolean UNDIRECTED = false;
  public static final int INDEX_ID1 = 0;
  public static final int INDEX_ID2 = 1;

  public static final String TRUST_POLICY = 
          Person.TRUST_POLICY_SIGMOID_FRACTION_OF_FRIENDS;
  public static final int DEFAULT_ROWS_TO_SKIP = 1;
  public static final char QUOTE_CHAR = '"';
  public static final char DEFAULT_DELIMITER = ',';
  

  /**
   * Create a new parser, but based on an already-existant network.
   *
   * @param network The network to use (not from a file).
   * @param sim The simulation to which this parser belongs.
   */
  public StAndrewsSocialNetworkParser(Network network, 
                                      MessagePropagationSimulation sim) {
    this.network = network;
    this.sim = sim;
  }
  
  /**
   * Create a new parser that will parse the given filename.
   *
   * @param filename The filename containing the St Andrews dataset.
   * @param delimter The character used as a delimiter in the CSV.
   * @param rowsToSkip The number of (e.g. header) lines to ignore in the CSV.
   * @param sim The simulation to which this parser belongs.
   */
  public StAndrewsSocialNetworkParser(String filename, 
                                      char delimiter,
                                      int rowsToSkip,
                                      MessagePropagationSimulation sim) 
                                  throws FileNotFoundException {
    this.sim = sim;
    this.delimiter = delimiter;
    this.rowsToSkip = rowsToSkip;
    parseNetworkFile(filename);
  }

  /**
   * Create a new parser that will parse the given filename. Uses defaults
   * for CSV values.
   *
   * @param filename The filename containing the St Andrews dataset.
   * @param sim The simulation to which this parser belongs.
   */
  public StAndrewsSocialNetworkParser(String filename, 
                                      MessagePropagationSimulation sim) 
                                  throws FileNotFoundException {
    this.sim = sim;
    this.delimiter = DEFAULT_DELIMITER;
    this.rowsToSkip = DEFAULT_ROWS_TO_SKIP;
    parseNetworkFile(filename);
  }

  /**
   * Parse the file as a St Andrews dataset with the configured CSV parameters,
   * storing it in the parser's network object.
   */
  private void parseNetworkFile(String filename) throws FileNotFoundException {
    System.err.println("Parsing Social Network File: " + filename);
    CSVReader reader;
    reader = new CSVReader(new FileReader(filename), 
                           delimiter, 
                           QUOTE_CHAR,
                           rowsToSkip);
    String [] nextLine;
    int line = 0;
    try {
      while ((nextLine = reader.readNext()) != null) {
        line++;
        if (line % 100000 == 0) {
          System.err.print(line + ", ");
        }        
        int id1 = Integer.parseInt(nextLine[INDEX_ID1].trim());
        int id2 = Integer.parseInt(nextLine[INDEX_ID2].trim());

        Person p1;
        Person p2;
        if (!people.containsKey(id1)) {
          p1 = new Person(id1, TRUST_POLICY, sim);
          people.put(id1, p1);
          network.addNode(p1);
        } else {
          p1 = people.get(id1);
        }
        if (!people.containsKey(id2)) {
          p2 = new Person(id2, TRUST_POLICY, sim);
          people.put(id2, p2);
          network.addNode(p2);
        } else {
          p2 = people.get(id2);
        }
        network.addEdge(p1, p2, new Double(1.0));
      }
      System.err.println();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * Return the network of people generated by parsing the dataset.
   *
   * @return A network object representing the people and their relationships.
   */
  public Network getNetwork() {
    return network;
  }
}
