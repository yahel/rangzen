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

import java.util.Date;
import java.util.Set;

/**
 * Basic storage mechanism, built upon Android's per-app storage.  If instantiated as such,
 * automatically encrypts and decrypts data before storing in Android.
 */
public class StorageBase {
  /** Specifies to not encrypt stored data in the backing store. */
  public static final int ENCRYPTION_NONE = 1;

  /**
   * Specifies to encrypt the stored data in the backing store using AES-GCM
   *
   * TODO(barath): Add support for this mode.
   */
  public static final int ENCRYPTION_AES_GCM = 2;

  /**
   * Specifies to use the default setting for encryption.
   *
   * TODO(barath): Once we have settled on the encryption default, change this setting to use
   * encryption rather than none.
   */
  public static final int ENCRYPTION_DEFAULT = ENCRYPTION_NONE;

  /** A handle for the preferences store that this instance is using to back all storage calls. */
  private SharedPreferences store;

  /** A handle for the editor that allows us to modify data in the store. */
  private SharedPreferences.Editor editor;

  /** The default local preferences file name used for storing all data. */
  private static final String STORE_FILE_NAME = "RangzenData";

  /**
   * Creates a store for any Rangzen data, with a consistent application of encryption of that
   * stored data, as specified.
   *
   * @param activity The app instance for which to perform storage.
   *
   * @param encryptionMode The encryption mode to use for all calls using this instance.
   */
  public StorageBase(Activity activity, int encryptionMode) throws IllegalArgumentException {
    // TODO(barath): Remove this check once we support more encryption modes.
    if (encryptionMode != ENCRYPTION_NONE) {
      throw new IllegalArgumentException("encryptionMode " + encryptionMode + " not supported.");
    }

    store = activity.getSharedPreferences(STORE_FILE_NAME, Activity.MODE_PRIVATE);
    editor = store.edit();
  }

  /**
   * Stores the given key-value pair in the Rangzen generic store.
   *
   * @param key The key under which to store the data.
   * @param value The value to store.
   */
  public void put(String key, String value) {
    // TODO(barath): Change this storage approach once we are encrypting.
    editor.putString(key, value);

    // TODO(barath): Consider whether we should use .commit() instead of apply().
    editor.apply();
  }

  /**
   * Stores the given set of strings in the Rangzen generic store.
   *
   * @param key The key under which to store the data.
   * @param values The values to store.
   */
  public void putSet(String key, Set<String> values) {
    // TODO(barath): Change this storage approach once we are encrypting.
    editor.putStringSet(key, values);

    // TODO(barath): Consider whether we should use .commit() instead of apply().
    editor.apply();
  }

  /**
   * Stores the given float in the Rangzen generic store.
   *
   * @param key The key under which to store the data.
   * @param value The value to store.
   */
  public void putFloat(String key, float value) {
    // TODO(barath): Change this storage approach once we are encrypting.
    editor.putFloat(key, value);

    // TODO(barath): Consider whether we should use .commit() instead of apply().
    editor.apply();
  }

  /**
   * Retrieves the value associated with the given key.
   *
   * @param key The key under which to retrieve a value from the store.
   * @return The value requested or null if not found.
   */
  public String get(String key) {
    // TODO(barath): Change this retrieval approach once we are encrypting.
    return store.getString(key, null);
  }

  /**
   * Retrieves the values associated with the given key.
   *
   * @param key The key under which to retrieve values from the store.
   * @return The values requested or null if not found.
   */
  public Set<String> getSet(String key) {
    // TODO(barath): Change this retrieval approach once we are encrypting.
    return store.getStringSet(key, null);
  }

  /**
   * Retrieves the value associated with the given key.
   *
   * @param key The key under which to retrieve a value from the store.
   * @param defvalue The default value to return if not found.
   * @return The value requested or defvalue if not found.
   */
  public float getFloat(String key, float defvalue) {
    // TODO(barath): Change this retrieval approach once we are encrypting.
    return store.getFloat(key, defvalue);
  }
}
