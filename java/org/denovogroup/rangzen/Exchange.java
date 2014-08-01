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

/**
 * Represents a single exchange between Rangzen peers.
 */
public class Exchange implements Serializable {
  /** Indicates backwards compatibility of serializability. */
  private static final long serialVersionUID = 1L;

  /** Local phone ID. */
  public String phoneid;

  /** Remote peer's phone ID. */
  public String peer_phone_id;

  /** Protocol used, e.g. Bluetooth or Wifi Direct. */
  public String protocol;

  /** The time at the start of the exchange. */
  public long start_time;

  /** The time at the end of the exchange. */
  public long end_time;

  /** Location at the start of the exchange. */
  public SerializableLocation start_location;

  /** Location at the end of the exchange. */
  public SerializableLocation end_location;

  /** String representing the Bluetooth protocol. */
  public static final String PROTOCOL_BLUETOOTH = "BluetoothProtocol";

  /**
   * Create a new exchange with the given values.
   *
   * @param start The Location at the start of the exchange.
   * @param end The Location at the end of the exchange.
   */
  public Exchange(String phoneid, String peer_phone_id, String protocol, long start_time,
      long end_time, SerializableLocation start_location, SerializableLocation end_location) {
    this.phoneid = phoneid;
    this.peer_phone_id = peer_phone_id;
    this.protocol = protocol;
    this.start_time = start_time;
    this.end_time = end_time;
    this.start_location = start_location;
    this.end_location = end_location;
  }

  /**
   * Two Exchanges are equal if they were between the same local and peer phones,
   * used the same protocol, their start locations were the same, and their
   * end locations were the same.
   *
   * Note that we consider the same exchange from each perspective to be distinct
   * exchanges, according to this .equals() method.
   *
   * @return True if the exchanges seem to represent the same exchange from the
   * same perspective.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Exchange)) {
      return false;
    } else {
      Exchange e = (Exchange) o;
      boolean startsEqual = start_location == null ? 
                            e.start_location == null : 
                            start_location.equals(e.start_location);
      boolean endsEqual = end_location == null ? 
                          e.end_location == null : 
                          end_location.equals(e.end_location);
      boolean IDsEqual = peer_phone_id == null ? 
                         e.peer_phone_id == null : 
                         peer_phone_id.equals(e.peer_phone_id);
      boolean timesEqual = start_time == e.start_time &&
                           end_time == e.end_time;

      return startsEqual && endsEqual && IDsEqual && timesEqual;
    }
  }
  
  /**
   * Return a string representing this exchange.
   */
  public String toString() {
    return String.format("Exchange | [phoneid=%s, other=%s, protocol=%s start_time=%s end_time=%s start=%s end=%s]",
                         phoneid, peer_phone_id, protocol, start_time, end_time, start_location, end_location);
  }
}
