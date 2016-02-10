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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

/**
 * Unit tests for Rangzen's StorageBase class
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class StorageBaseTest {
  /** The instance of StorageBase we're using for tests. */
  private StorageBase store;

  /** The app instance we're using to pass to StorageBase. */
  private SlidingPageIndicator activity;

  /** Test strings we're writing into the storage system. */
  private static final String TEST_KEY = "k";
  private static final String TEST_VALUE = "v1";
  private static final String TEST_VALUE_2 = "v2";

  private static final String TEST_FLOAT_KEY = "fk";
  private static final float TEST_FLOAT_VALUE = 7.7f;

  private static final String TEST_INT_KEY = "ik";
  private static final int TEST_INT_VALUE = 512;

  private static final String TEST_LONG_KEY = "lk";
  private static final int TEST_LONG_VALUE = 1024;

  private static final String TEST_DOUBLE_KEY = "dk";
  private static final double TEST_DOUBLE_VALUE = 99.99;

  private static final String TEST_KEY_OBJECT = "ok";
  private static final int TEST_OBJECT_INT_VALUE = 7;
  private static final String TEST_OBJECT_STRING_VALUE = "foo";

  public static class SimpleObject implements Serializable {
    private static final long serialVersionUID = 0L;
    private int v;
    private String s;

    public SimpleObject(int v, String s) {
      this.v = v;
      this.s = s;
    }

    public boolean equals(Object o) {
      if (o instanceof SimpleObject) {
        SimpleObject x = (SimpleObject) o;
        return v == x.v && s.equals(x.s);
      }

      return false;
    }
  }

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();
    store = new StorageBase(activity, StorageBase.ENCRYPTION_NONE);
  }

  /**
   * Tests that we can store and retrieve a string.
   */
  @Test
  public void storeString() {
    store.put(TEST_KEY, TEST_VALUE);
    assertEquals(store.get(TEST_KEY), TEST_VALUE);
  }

  /**
   * Tests that we can store and retrieve a set of strings.
   */
  @Test
  public void storeStringSet() {
    Set<String> strings = new HashSet<String>();
    strings.add(TEST_VALUE);
    strings.add(TEST_VALUE_2);

    store.putSet(TEST_KEY, strings);
    assertEquals(store.getSet(TEST_KEY), strings);
  }

  /**
   * Tests that we can store and retrieve a float.
   */
  @Test
  public void storeFloat() {
    store.putFloat(TEST_FLOAT_KEY, TEST_FLOAT_VALUE);
    assertEquals(store.getFloat(TEST_FLOAT_KEY, -1.0f), TEST_FLOAT_VALUE, 0.1);
  }

  /**
   * Tests that we can store and retrieve an int.
   */
  @Test
  public void storeInt() {
    store.putInt(TEST_INT_KEY, TEST_INT_VALUE);
    assertEquals(store.getInt(TEST_INT_KEY, -100), TEST_INT_VALUE);
  }

  /**
   * Tests that we can store and retrieve a long.
   */
  @Test
  public void storeLong() {
    store.putLong(TEST_LONG_KEY, TEST_LONG_VALUE);
    assertEquals(store.getLong(TEST_LONG_KEY, -100), TEST_LONG_VALUE);
  }

  /**
   * Tests that we can store and retrieve a double.
   */
  @Test
  public void storeDouble() {
    store.putDouble(TEST_DOUBLE_KEY, TEST_DOUBLE_VALUE);
    assertEquals(store.getDouble(TEST_DOUBLE_KEY, -100), TEST_DOUBLE_VALUE, 0.000001);
  }

  /**
   * Tests that we can store and retrieve an object.
   */
  @Test
  public void storeObject() throws Exception {
    SimpleObject s = new SimpleObject(TEST_OBJECT_INT_VALUE, TEST_OBJECT_STRING_VALUE);
    store.putObject(TEST_KEY_OBJECT, s);
    assertEquals((SimpleObject) store.getObject(TEST_KEY_OBJECT), s);
  }
}
