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

import android.content.Context;

import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Storage for exchanges that uses StorageBase underneath. 
 */
public class ExchangeStore {
  /** A handle for the underlying store */
  private StorageBase store;
  
  /** The internal key used in the underlying store for Rangzen exchange data. */
  private static final String EXCHANGES_KEY = "RangzenExchange-";

  /** The internal key used to store the sequence number of the most recent exchange stored. */
  private static final String SEQUENCE_KEY = "RangzenExchangeSequence";

  /** Value returned to indicate that no exchanges have been stored. */
  public static final int NO_SEQUENCE_STORED = -1;

  /** Lowest (first) sequence number used to store a exchange. */
  public static final int MIN_SEQUENCE_NUMBER = 1;

  /**
   * Determines the most recently used (maximum) sequence number.
   *
   * @return The next available sequence number.
   */
  public int getMostRecentSequenceNumber() {
    int DEFAULT_INT = NO_SEQUENCE_STORED; 
    return store.getInt(SEQUENCE_KEY, DEFAULT_INT);
  }

  /**
   * Determines the next sequence number available.
   *
   * @return The next available sequence number.
   */
  private int getNextSequenceNumber() {
    int last = getMostRecentSequenceNumber();
    if (last == NO_SEQUENCE_STORED) {
      return MIN_SEQUENCE_NUMBER;
    } else {
      return last + 1;
    }
  }

  /**
   * Determines the key to use for a new exchange object.
   *
   * @return A key to be used for a new exchange object.
   */
  private String getExchangeKey() {
    return getExchangeKey(getNextSequenceNumber());
  }

  /**
   * Returns the key for the exchange with the given sequence number.
   *
   * @return The key for the exchange with the given sequence number.
   */
  private String getExchangeKey(int sequenceNumber) {
    return EXCHANGES_KEY + sequenceNumber;
  }
 
  /**
   * Creates a Rangzen exchange store, with a consistent application of encryption of that stored
   * data, as specified.
   *
   * @param context A context in which to do storage.
   * @param encryptionMode The encryption mode to use for all calls using this instance.
   */
  public ExchangeStore(Context context, int encryptionMode) throws IllegalArgumentException {
    store = new StorageBase(context, encryptionMode);
  }

  /**
   * Adds the given exchange.
   *
   * @param msg The exchange to add.
   *
   * @return Returns true if the exchange was stored.
   */
  public boolean addExchange(Exchange exchange) {
    String key = getExchangeKey();
    try {
      store.putObject(key, exchange);
      store.putInt(SEQUENCE_KEY, getNextSequenceNumber());
      return true;
    } catch (StreamCorruptedException e) {
      e.printStackTrace();
      return false;
    } catch (OptionalDataException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Get a list of all exchanges stored on this device.
   *
   * @return A list of exchanges this device has been recorded to be at.
   *
   * TODO(lerner): Use a set instead. Hashcode is always 0 on my serializable
   * exchanges though, and they're not comparable, so we can't use HashSet or TreeSet.
   */
  public List<Exchange> getAllExchanges() throws StreamCorruptedException,
      OptionalDataException, IOException, ClassNotFoundException {
    int lastSequenceNumber = getMostRecentSequenceNumber();
    if (lastSequenceNumber == NO_SEQUENCE_STORED) {
      return new ArrayList<Exchange>();
    } else {
      ArrayList<Exchange> exchanges = new ArrayList<Exchange>();
      for (int i = MIN_SEQUENCE_NUMBER; i <= lastSequenceNumber; i++) {
        exchanges.add((Exchange) store.getObject(getExchangeKey(i)));
      }
      return exchanges;
    }
  }

  /**
   * Get a list of exchanges stored on this device between the given indexes (inclusive).
   * Throws an IllegalArgumentException if either index value is out of bounds
   * or end is less than start.
   *
   * @return A list of exchanges this device has been recorded to be at.
   */
  public List<Exchange> getExchanges(int start, int end) throws StreamCorruptedException,
      OptionalDataException, IOException, ClassNotFoundException {
    int lastSequenceNumber = getMostRecentSequenceNumber();
    if (start < MIN_SEQUENCE_NUMBER || end < MIN_SEQUENCE_NUMBER ||
        start > lastSequenceNumber || end > lastSequenceNumber || end < start) {
      throw new IllegalArgumentException("Indexes [" + start + "," + end + "] out of bounds.");
    }

    ArrayList<Exchange> exchanges = new ArrayList<Exchange>();
    for (int i = start; i <= end; i++) {
      exchanges.add((Exchange) store.getObject(getExchangeKey(i)));
    }
    return exchanges;
  }
}
