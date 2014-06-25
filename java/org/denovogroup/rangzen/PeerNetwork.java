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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.ScanResult;

/**
 * This class represents the network connectivity over 0 or more modalities
 * that can be used to reach a certain peer.
 */
public interface PeerNetwork {
  /**
   * Send a message. Returns immediately, sending the message asynchronously
   * as it is possible to do so given the constraints of the network media
   * (peer comes and goes, need to connect first, etc.)
   *
   * @return False if the message could not be sent, true otherwise.
   */
  public void send(String message);

  /**
   * Wait to receive a message from the peer. Currently always returns null.
   *
   * @return A byte array containing the contents of the message, or null on 
   * an error.
   */
  public byte[] receive();

  /**
   * Two PeerNetworks are equal if they communicate with the same network
   * devices.
   *
   * @return True if the PeerNetworks are equal, false otherwise.
   */
  public boolean equals(PeerNetwork other);

  /**
   * Return a copy of the PeerNetwork, referring to the same network locations.
   *
   * @return A deep copy of the PeerNetwork.
   */
  public PeerNetwork clone();

  /** A string representation of the Peer Network. */
  public String toString();

  /** 
   * If the PeerNetwork is if of a type that uses Wifi Direct, return its
   * backing WifiP2pDevice, if any. If it is not of a Wifi Direct type, or
   * if it is but isn't backed by any device, returns null.
   *
   * @return A backing WifiP2pDevice if one exists; otherwise null.
   */
  public WifiP2pDevice getWifiP2pDevice();

  /** 
   * If the PeerNetwork is if of a type that uses Hotspot mode, return its
   * backing ScanResult, if any. If it is not of a Hotspot type, or
   * if it is but isn't backed by any device, returns null.
   *
   * @return A backing ScanResult if one exists; otherwise null.
   */
  public ScanResult getScanResult();
}
