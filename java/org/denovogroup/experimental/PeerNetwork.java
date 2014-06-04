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
package org.denovogroup.experimental;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Arrays;

/**
 * This class represents the network connectivity over 0 or more modalities
 * that can be used to reach a certain peer.
 */
public class PeerNetwork {
  /** A WifiP2pDevice (remote MAC address) associated with this Peer Network */
  WifiP2pDevice wifiP2pDevice;

  /**
   * Create a PeerNetwork with no remote network devices to talk to.
   */
  public PeerNetwork() { }

  /**
   * Create a PeerNetwork with a remote WifiP2pDevice to talk to .
   */
  public PeerNetwork(WifiP2pDevice wifiP2pDevice) {
    this.wifiP2pDevice = wifiP2pDevice;
  }

  /**
   * Send a message. Returns immediately, sending the message asynchronously
   * as it is possible to do so given the constraints of the network media
   * (peer comes and goes, need to connect first, etc.)
   *
   * @return False if the message could not be sent, true otherwise.
   */
  public void send(String message) {
  }

  /**
   * Wait to receive a message from the peer. Currently always returns null.
   *
   * @return A byte array containing the contents of the message, or null on 
   * an error.
   */
  public byte[] receive() {
    return null;
  }

  /**
   * Two PeerNetworks are equal if they communicate with the same network
   * devices.
   *
   * @return True if the PeerNetworks are equal, false otherwise.
   */
  public boolean equals(PeerNetwork other) {
    if (other == null) {
      return false;
    } else if (wifiP2pDevice != null) {
      return wifiP2pDevice.equals(other.wifiP2pDevice);
    } else {
      return wifiP2pDevice == other.wifiP2pDevice;
    }
  }

  /**
   * Return a copy of the PeerNetwork, referring to the same network locations.
   *
   * @return A deep copy of the PeerNetwork.
   */
  public PeerNetwork clone() {
    PeerNetwork clone = new PeerNetwork(new WifiP2pDevice(this.wifiP2pDevice));

    return clone;
  }
}
