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

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Storage for Rangzen messages that uses StorageBase underneath.  If instantiated as such,
 * automatically encrypts and decrypts data before storing in Android.
 */
public class MessageStore {
  /** A handle for the underlying store */
  private StorageBase store;
  
  /** The internal key used in the underlying store for Rangzen message data. */
  private static final String MESSAGES_KEY = "RangzenMessages-";

  /** The internal key used in the underlying store for Rangzen message priorities. */
  private static final String MESSAGE_PRIORITY_KEY = "RangzenMessagePriority-";

  /**
   * The number of bins to use for storing messages.  Each bin stores 1/NUM_BINS range of priority
   * values, called the INCREMENT.  Bin 0 stores [0,INCREMENT), bin 1 stores [INCREMENT,
   * 2*INCREMENT), etc.
   */
  private static final int NUM_BINS = 5;

  /** The range increment, computed from the number of bins. */
  private static final float INCREMENT = 1.0f / (float) NUM_BINS;

  /** The min priority value. */
  private static final float MIN_PRIORITY_VALUE = 0.0f;

  /** The max priority value. */
  private static final float MAX_PRIORITY_VALUE = 2.0f;

  /** Ensures that the given priority value is in range. */
  private static void checkPriority(float priority) throws IllegalArgumentException {
    if (priority < MIN_PRIORITY_VALUE || priority > MAX_PRIORITY_VALUE) {
      throw new IllegalArgumentException("Priority " + priority + " is outside valid range of [0,1]");
    }
  }

  /**
   * Determines the bin key for a given bin number.
   *
   * @param bin The bin number.
   *
   * @return The String bin key to use for that bin.
   */
  private static String getBinKey(int bin) {
    return MESSAGES_KEY + bin;
  }

  /**
   * Determines the message priority key for a given message.
   *
   * @param msg The message.
   *
   * @return The String message priority key for that message.
   */
  private static String getMessagePriorityKey(String msg) {
    return MESSAGE_PRIORITY_KEY + msg;
  }

  /**
   * Determines the bin that corresponds to a given priority value.
   *
   * @param priority The priority for which to identify the bin number.
   *
   * @return The bin number.
   */
  private static int getBinForPriority(float priority) {
    checkPriority(priority);

    int bin = (int) (priority / INCREMENT);
    if (bin >= NUM_BINS) {
      // We should only select this bin if priority == 1.0.
      bin = NUM_BINS - 1;
    }

    return bin;
  }

  /**
   * Determines the message key to use for the bin corresponding to the given priority.
   *
   * @param priority The message for which to look up the key.
   *
   * @return A String to use as a key for the given bin.
   */
  private static String getBinKeyForPriority(float priority) {
    checkPriority(priority);

    int bin = getBinForPriority(priority);
    return getBinKey(bin);
  }

  /**
   * Creates a Rangzen message store, with a consistent application of encryption of that stored
   * data, as specified.  All messages are associated with a priority value that can be modified.
   *
   * @param activity The app instance for which to perform storage.
   * @param encryptionMode The encryption mode to use for all calls using this instance.
   *
   * TODO(barath): Add support for a storage limit, which automatically triggers garbage collection
   *               when hit.
   */
  public MessageStore(Activity activity, int encryptionMode) throws IllegalArgumentException {
    store = new StorageBase(activity, encryptionMode);
  }

  /**
   * Adds the given message with the given priority.
   *
   * @param msg The message to add.
   * @param priority The priority to associate with the message.  The priority must be [0,1].
   *
   * @return Returns true if the message was added.  If the message already exists, does not modify
   * the store and returns false.
   */
  public boolean addMessage(String msg, float priority) {
    checkPriority(priority);

    // Check whether we have the message already (perhaps in another bin).
    // TODO(barath): Consider improving performance by selecting a different key.
    String msgPriorityKey = MESSAGE_PRIORITY_KEY + msg;

    // A value less than all priorities in the store.
    final float MIN_PRIORITY = -1.0f;

    // The default value that indicates not found.
    final float NOT_FOUND = -2.0f;

    boolean found = !(store.getFloat(msgPriorityKey, NOT_FOUND) < MIN_PRIORITY);
    if (found) {
      return false;
    }
    
    // Get the existing message set for the bin, if it exists.
    String binKey = getBinKeyForPriority(priority);
    Set<String> msgs = store.getSet(binKey);
    if (msgs == null) {
      msgs = new HashSet<String>();
    }

    // Add the message with the given priority, and to the bin.
    store.putFloat(msgPriorityKey, priority);
    msgs.add(msg);
    store.putSet(binKey, msgs);

    return true;
  }

  /**
   * Removes the given message from the store.
   *
   * @param msg The message to remove.
   *
   * @return Returns true if the message was removed.  If the message was not found, returns false.
   */
  public boolean deleteMessage(String msg) {
    // TODO(barath): Implement.
    return false;
  }

  /**
   * Returns the given message's priority, if present.
   *
   * @param msg The message whose priority to retrieve.
   * @param defvalue The default value to return if not found.
   *
   * @return Returns msg's priority or defvalue if not found.
   */
  public float getMessagePriority(String msg, float defvalue) {
    return store.getFloat(getMessagePriorityKey(msg), defvalue);
  }

  /**
   * Returns the messages with highest priority values.
   *
   * @param k The number of messages to return.
   *
   * @return Returns up to k messages with the highest priority values.
   */
  public TreeMap<Float, Collection<String>> getTopK(int k) {
    TreeMap<Float, Collection<String>> topk = new TreeMap<Float, Collection<String>>();
 
binloop: for (int bin = NUM_BINS - 1; bin >= 0; bin--) {
      String binKey = getBinKey(bin);
      Set<String> msgs = store.getSet(binKey);
      if (msgs == null) continue;

      TreeMap<Float, List<String>> sortedmsgs = new TreeMap<Float, List<String>>();
      for (String m : msgs) {
        float priority = getMessagePriority(m, -1);
        if (!sortedmsgs.containsKey(priority)) {
          sortedmsgs.put(priority, new ArrayList<String>());
        }
        sortedmsgs.get(priority).add(m);
      }

      NavigableMap<Float, List<String>> descMap = sortedmsgs.descendingMap();
      for (Entry<Float, List<String>> e : descMap.entrySet()) {
        for (String m : e.getValue()) {
          if (topk.size() >= k) break binloop;

          float priority = e.getKey();
          if (!topk.containsKey(priority)) {
            topk.put(priority, new HashSet<String>());
          }
          topk.get(priority).add(m);
        }
      }
    }

    return topk;
  }
}
