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
package org.denovogroup.rangzen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Demonstrating how to use OpenCSV.
 */
@RunWith(JUnit4.class)
public class OpenCSVTest {
  @Test
  public void testRead() throws IOException {
    String csv = "one,two,three\none,five,six\none,2.0,'124124.1241241.12,12412,1,4'";
    InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(csv.getBytes("UTF-8")));
    CSVReader reader = new CSVReader(isr, ',', '\'');
    List<String[]> rows = reader.readAll();
    for (String[] row : rows) {
      assertEquals("one", row[0]);
      assertEquals(3, row.length);
    }

  }

  @Test
  public void testWrite() throws IOException { 
    String[] row1 = new String[] { "oneone", "onetwo", "onethree" };
    String[] row2 = new String[] { "twoone", "twotwo", "twothree" };

    StringWriter sWriter = new StringWriter();
    CSVWriter writer = new CSVWriter(sWriter, ',', '\'');

    writer.writeNext(row1);
    writer.writeNext(row2);

    String written = sWriter.toString();

    CSVReader reader = new CSVReader(new StringReader(written), ',', '\'');
    List<String[]> rows = reader.readAll();
    assertTrue(Arrays.equals(rows.get(0), row1));
    assertTrue(Arrays.equals(rows.get(1), row2));
  }
}
