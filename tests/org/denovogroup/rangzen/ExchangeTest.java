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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;
 
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Tests for the Exchange class.
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18,
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class ExchangeTest {

  /** Exchange under test. */
  private Exchange exchange;

  /** Output stream passed to exchange under test. */
  private PipedOutputStream outputStream;
  /** Input stream passed to exchange under test. */
  private PipedInputStream inputStream;

  /** Attached to the output stream passed to the exchange so we hear it. */
  private PipedOutputStream testOutputStream;
  /** Attached to the input stream passed to the exchange so we can talk to it. */
  private PipedInputStream testInputStream;

  /** A message store to pass to the exchange. */
  private MessageStore messageStore;
  /** A friend store to pass to the exchange. */
  private FriendStore friendStore;

  /** A callback to provide to the Exchange under test. */
  private ExchangeCallback callback ;

  /** Runs before each test. */
  @Before
  public void setUp() throws IOException {
    outputStream = new PipedOutputStream();
    inputStream = new PipedInputStream();
    testOutputStream = new PipedOutputStream();
    testInputStream = new PipedInputStream();

    // We'll hear what the exchange says via testInputStream,
    // and we can send data to it on testOutputStream.
    testOutputStream.connect(inputStream);
    outputStream.connect(testInputStream);

    SlidingPageIndicator context = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();

    messageStore = new MessageStore(context, StorageBase.ENCRYPTION_DEFAULT); 
    friendStore = new FriendStore(context, StorageBase.ENCRYPTION_DEFAULT); 

    callback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
      }

      @Override
      public void failure(Exchange exchange, String reason) {
      }
    };
  }   

  @Test
  public void asInitiatorOK() throws IOException {
    exchange = new Exchange(inputStream,
                            outputStream,
                            true,
                            friendStore,
                            messageStore,
                            callback);

    // We send the second message now, but it won't be read until after 
    // the exchange sends its first message as Alice.
    testOutputStream.write(Exchange.SECOND_DEMO_MESSAGE.getBytes());

    // Start the exchange.
    exchange.execute(true);

    // Read the message from the exchange. Expect to receive FIRST_DEMO_MESSAGE.
    ByteBuffer recvBuffer = ByteBuffer.allocate(Exchange.FIRST_DEMO_MESSAGE.length());
    do {
      int b = testInputStream.read();
      if (b == -1) {
        assertNotEquals("Premature EOF, not enough bytes read from Exchange.",
                    Exchange.FIRST_DEMO_MESSAGE.length(),
                    recvBuffer.position());
      }
      recvBuffer.put((byte)b);
    } while (recvBuffer.position() != Exchange.FIRST_DEMO_MESSAGE.length());
    assertEquals(Exchange.FIRST_DEMO_MESSAGE, new String(recvBuffer.array()));
  }

  @Test
  public void notAsInitiatorOK() throws IOException {
    exchange = new Exchange(inputStream,
                            outputStream,
                            false,
                            friendStore,
                            messageStore,
                            callback);
    // Send the initiating message along the pipe.
    testOutputStream.write(Exchange.FIRST_DEMO_MESSAGE.getBytes());

    // Start the exchange
    exchange.execute(true);

    // Read the message from the exchange. Expect to receive SECOND_DEMO_MESSAGE.
    ByteBuffer recvBuffer = ByteBuffer.allocate(Exchange.FIRST_DEMO_MESSAGE.length());
    do {
      int b = testInputStream.read();
      if (b == -1) {
        assertNotEquals("Premature EOF, not enough bytes read from Exchange.",
                        Exchange.SECOND_DEMO_MESSAGE.length(),
                        recvBuffer.position());
      }
      recvBuffer.put((byte)b);
    } while (recvBuffer.position() != Exchange.SECOND_DEMO_MESSAGE.length());
    assertEquals(Exchange.SECOND_DEMO_MESSAGE, new String(recvBuffer.array()));
  }

  /**
   * Test the static utility method that grabs the first four bytes from an
   * input stream and returns their value as an int.
   */
  @Test
  public void testPopLength() throws IOException {
    int testValue = 42;
    PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream outputStream = new PipedOutputStream();
    outputStream.connect(inputStream);
    
    ByteBuffer b = ByteBuffer.allocate(4);
    b.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    b.putInt(testValue);
    outputStream.write(b.array());
    assertEquals(testValue, Exchange.popLength(inputStream));
  }
}
