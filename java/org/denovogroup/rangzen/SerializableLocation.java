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
  public long realTimeNanos;
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
    this.realTimeNanos = location.getElapsedRealtimeNanos();
    this.provider = location.getProvider();
    this.speed = location.getSpeed();
    this.time = location.getTime();

    this.hasAccuracy = location.hasAccuracy();
    this.hasAltitude = location.hasAltitude();
    this.hasBearing = location.hasBearing();
    this.hasSpeed = location.hasSpeed();
  }
}
