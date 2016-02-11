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

import au.com.bytecode.opencsv.CSVReader;

import java.io.Reader;
import java.io.IOException;

/**
 * A CSVReader that exposes the .peek() operation to view the next row without
 * moving onto the subsequent one.
 */
public class PeekableCSVReader extends CSVReader {

  private String[] nextLine;

  /**
   * Create a new PeekableCSVReader.
   *
   * @param reader A reader attached to the CSV data we want to read.
   * @param delimiter The delimiter used in the CSV data.
   * @param quote The quote character used in the CSV data.
   * @param skip The number of lines at the top of the data to ignore (e.g. headers).
   */
  public PeekableCSVReader(Reader reader, char delimiter, char quote, int skip) {
    super(reader, delimiter, quote, skip);

    try {
      nextLine = super.readNext();
    }
    catch (IOException e) {
      e.printStackTrace();
      nextLine = null;
    }
  }

  /**
   * Return the next line as an array of string values, broken on the delimiter.
   * After this call, the position in the data will be moved to the next line.
   * (A series of calls to this method result in different data each line, finishing
   * with null when the data is out of lines.)
   *
   * @return The next line, as an array of Strings.
   */
  public String[] readNext() {
    String[] temp = nextLine;
    try {
      nextLine = super.readNext();
    }
    catch (IOException e) {
      e.printStackTrace();
      nextLine = null;
    }
    return temp;
  }

  /**
   * Return the next line as an array of string values, broken on the delimiter.
   * This method doesn't progress the position in the data - repeated calls to
   * this method will return the same line over and over again.
   *
   * @return The next line, as an array of Strings.
   */
  public String[] peek() {
    return nextLine;
  }
}
