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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class wraps PeekableCSVReader, enabling the reading of chunks of
 * lines from a CSVReader based on the value of a field. For example,
 * if the first 10 lines of a file contain the same value for the given
 * field, those 10 lines will be returned. If the subsequent 2 lines have
 * the same value for that field, they will be the next lines returned.
 */
public class CSVFieldChunkReader {

  private PeekableCSVReader csvReader;
  private int field;
  private String currentFieldValue = null;
  private List<String[]> nextChunk;
  
  /**
   * Construct a new chunk reader that operates on chunks of the same 
   * value of the field parameter.
   *
   * @param filename The CSV file to be read.
   * @param delimiter The delimiter used in the CSV file to be read.
   * @param quote The quote character used in the CSV file to be read.
   * @param skip How many lines to ignore of the file at the start (e.g. to 
   *             ignore headers.)
   * @param field The index of the column (field) of the file whose sameness
   *              should determine chunks.
   */
  public CSVFieldChunkReader(String filename, char delimiter, char quote, 
                             int skip, int field) 
          throws FileNotFoundException {

    this.csvReader = new PeekableCSVReader(new FileReader(filename), 
                                           delimiter, quote, skip);
    this.field = field;

    readyNextChunk();
  }

  /**
   * Get a list of all lines having the same value starting from the current 
   * point in the file and ending when reaching a line having a different
   * field value.
   *
   * @return A list of lines, all of which have the same value for the key field.
   */
  public List<String[]> nextChunk() {
    List<String[]> temp = nextChunk;
    readyNextChunk();
    return temp;
  }
  
  /**
   * Loads the next chunk into nextChunk.
   */
  private void readyNextChunk() {
    String[] firstLineOfChunk = csvReader.peek();
    if (firstLineOfChunk == null) {
      nextChunk = null;
    } else {
      nextChunk = new ArrayList<String[]>();
      currentFieldValue = firstLineOfChunk[field];
      String[] nextLine;
      while (csvReader.peek() != null &&
             csvReader.peek()[field].equals(currentFieldValue)) {
          nextChunk.add(csvReader.readNext());
      }
    }

  }
}
