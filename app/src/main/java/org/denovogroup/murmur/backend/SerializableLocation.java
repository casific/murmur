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
package org.denovogroup.murmur.backend;

import android.location.Location;

import java.io.Serializable;

/**
 * Serializable object with location fields that can be instantiated from an
 * Android location.
 */
public class SerializableLocation implements Serializable {
  /** Indicates backwards compatibility of serializability. */
  private static final long serialVersionUID = 1L;

  public double latitude;
  public double longitude;
  public float accuracy;
  public double altitude;
  public float bearing;
  public String provider;
  public float speed;
  public long time;

  public boolean hasAccuracy;
  public boolean hasAltitude;
  public boolean hasBearing;
  public boolean hasSpeed;

  /**
   * Create a new SerialziableLocation with the values of the given Location.
   */
  public SerializableLocation(Location location) {
    this.latitude = location.getLatitude();
    this.longitude = location.getLongitude();
    this.accuracy = location.getAccuracy();
    this.altitude = location.getAltitude();
    this.bearing = location.getBearing();
    this.provider = location.getProvider();
    this.speed = location.getSpeed();
    this.time = location.getTime();

    this.hasAccuracy = location.hasAccuracy();
    this.hasAltitude = location.hasAltitude();
    this.hasBearing = location.hasBearing();
    this.hasSpeed = location.hasSpeed();
  }

  /**
   * Two SerializableLocations are equal if all of their fields are equal.
   * For floating point and double fields, we use an approximate equality.
   *
   * The other object must also be an instance of SerializableLocation.
   *
   * @param o Another object to compare to this location.
   * @return True if the given object is a SerializableLocation that seems
   * to represent the same location (and time, etc.). False otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SerializableLocation)) {
      System.err.println("Not an instance of SerializableLocation.");
      return false;
    } else {
      SerializableLocation sl = (SerializableLocation) o;
      return approxEqual(latitude, sl.latitude) &&
             approxEqual(longitude, sl.longitude) &&
             approxEqual(accuracy, sl.accuracy) &&
             approxEqual(altitude, sl.altitude) &&
             approxEqual(bearing, sl.bearing) &&
             provider.equals(sl.provider) && 
             approxEqual(speed, sl.speed) &&
             this.time == sl.time &&
             this.hasAccuracy == sl.hasAccuracy &&
             this.hasAltitude == sl.hasAltitude &&
             this.hasBearing == sl.hasBearing &&
             this.hasSpeed == sl.hasSpeed;
    }
  }

  /**
   * Determine whether two doubles are close enough, according to an arbitrarily
   * chosen threshold.
   *
   * @param a A double to compare.
   * @param b Another double to compare.
   * @return True if the given doubles are within an arbitrarily chosen threshold; false otherwise
   */
  private boolean approxEqual(double a, double b) {
    double THRESHOLD = 0.00000001;
    double c = a - b;
    System.err.println(Math.abs(c) <= THRESHOLD);
    return Math.abs(c) <= THRESHOLD;
  }

  /**
   * Return a string representing this location.
   */
  public String toString() {
    return String.format("SerializableLocation[%s %f, %f acc(%b)=%f alt(%b)=%f bear(%b)=%f speed(%b)=%f time=%d]", 
                         provider, latitude, longitude, hasAccuracy, accuracy, hasAltitude, altitude, hasBearing, bearing, hasSpeed, speed, time);
  }
}
