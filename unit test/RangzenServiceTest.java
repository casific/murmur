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

import android.content.Intent;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import java.lang.Thread;
import java.util.Date;

@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", emulateSdk=18)
@RunWith(RobolectricTestRunner.class)
public class RangzenServiceTest {
  /** A Date before the service is started in setUp(). */
  private Date preServiceStartTime;

  private static final int AVOID_BACKGROUND_TASK_RACE_SLEEP_TIME = 200;

  /** The instance of the service. */
  private RangzenService service;
  
  /** A startid we passed to the service, which is different each time. */
  private int startId = 0;

  @Before
  public void setUp() throws Exception {
    preServiceStartTime = new Date();
    Intent intent = new Intent(Robolectric.application, RangzenService.class);
    service = new RangzenService();
    service.onCreate();
    service.onStartCommand(intent, 0, startId);
    startId++;
  }

  @After
  public void tearDown() throws Exception {
    service.onDestroy();
  }

  /**
   * Check that after starting the service, a static singleton instance exists.
   */
  @Test
  public void serviceInstanceExists() throws Exception {
    assertNotNull("No instance of RangzenService", 
            RangzenService.sRangzenServiceInstance);
  }

  /**
   * Check that a new Rangzen Service has sane start time.
   */
  @Test
  public void serviceHasRecentStartTime() throws Exception {
    Date start = RangzenService.sRangzenServiceInstance.getServiceStartTime();
    assertNotNull("Rangzen Service start time is null", start);
    assertFalse("Rangzen Service start time before start time", 
            start.before(preServiceStartTime));
    assertFalse("Rangzen Service start time after now", 
            start.after(new Date()));
  } 

  /**
   * Check that the background service thread runs after a brief time.
   */
  @Test
  public void backgroundTaskRuns() throws Exception {
    // Sleep briefly to avoid racing with the first background task.
    Thread.sleep(AVOID_BACKGROUND_TASK_RACE_SLEEP_TIME);
    assertTrue("Still 0 background tasks run after " + 
            AVOID_BACKGROUND_TASK_RACE_SLEEP_TIME + " milliseconds", 
            service.getBackgroundTasksRunCount() > 0);
  } 

  /**
   * Test that starting the service is idempotent (only the first one counts).
   */
  @Test
  public void callingOnStartCommandMultipleTimes() throws Exception {
    // Sleep briefly to avoid racing with the first background task.
    Thread.sleep(AVOID_BACKGROUND_TASK_RACE_SLEEP_TIME);

    Date startTime = service.getServiceStartTime();
    int backgroundRunCount = service.getBackgroundTasksRunCount();
    Intent intent = new Intent(Robolectric.application, RangzenService.class);
    service.onStartCommand(intent, 0, startId);
    startId++;

    assertEquals("Start time changed calling onStartCommand again", 
            startTime, service.getServiceStartTime());
    assertEquals("Background run count changed calling onStartCommand again", 
            backgroundRunCount, service.getBackgroundTasksRunCount());
  }
}
