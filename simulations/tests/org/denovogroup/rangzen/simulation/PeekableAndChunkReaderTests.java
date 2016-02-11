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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.List;
 
/**
 * Tests of PeekableCSVReader and CSVFieldChunkReader.
 */
@RunWith(JUnit4.class)
public class PeekableAndChunkReaderTests {
  private String GOWALLA_FILENAME = "data/gowalla/loc-gowalla_totalCheckins.txt";
  private PeekableCSVReader peekableReader;
  private CSVFieldChunkReader fieldChunkReader;

  @Before
  public void setUp() throws FileNotFoundException {
    peekableReader = new PeekableCSVReader(new FileReader(GOWALLA_FILENAME), 
                                           '\t', '"', 1);

    fieldChunkReader = new CSVFieldChunkReader(GOWALLA_FILENAME, '\t', '"', 1, 0);
  }

  @Test
  public void peekableReaderTest() {
    String[] anticipated = peekableReader.peek();
    String[] actual = peekableReader.readNext();
    assertEquals("Didn't get same thing from peek and readNext()",
                 anticipated, actual);

    anticipated = peekableReader.peek();
    anticipated = peekableReader.peek();
    anticipated = peekableReader.peek();
    anticipated = peekableReader.peek();
    assertEquals("Didn't get same thing from peek and peek",
                 anticipated, anticipated);

    String[] after;
    do {
      peekableReader.readNext();
      after = peekableReader.peek();
    } while (after != null);

    assertEquals("Dowhile is written wrong", after, null);
    actual = peekableReader.readNext();
    assertEquals("End peek/read aren't both null.", after, actual);
  }

  @Test
  public void fieldChunkReaderTest() {
    List<String[]> chunk = fieldChunkReader.nextChunk();
    for (String[] line : chunk) {
      assertEquals("Line in first chunk not of person 0", 
                   0,
                   Integer.parseInt(line[0]));
    }
    chunk = fieldChunkReader.nextChunk();
    for (String[] line : chunk) {
      assertEquals("Line in second chunk not of person 1", 
                   1, 
                   Integer.parseInt(line[0]));
    }

    for (int i=2; i<100; i++) {
      chunk = fieldChunkReader.nextChunk();
      int expectedValue = -1;
      for (String[] line : chunk) {
        if (expectedValue == -1) {
          expectedValue = Integer.parseInt(line[0]);
        }
        assertEquals("Line in chunk " + i + " not of person " + i, 
                     expectedValue,
                     Integer.parseInt(line[0]));
      }
    }

  }




}
