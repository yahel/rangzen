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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Class that handles retrieving contacts from the contact book, gathering
 * phone numbers, hashing the phone numbers and storing them. Can also
 * return the set of stored phone number hashes.
 *
 * Use retrieveAndStoreContacts() once permission has been received from the
 * user to use the phone numbers of their contacts to retrieve them from 
 * the OS and store them in our own storage.
 *
 * Use getObfuscatedPhoneNumbers() to retrieve the list of phone number hashes
 * later on.
 */
public class ContactsGetterStorer {

  /** Tag for all Android log messages. */
  private final static String TAG = "ContactsGetterStorer";

  /** Context of the RangzenService that instantiated this object. */
  private Context context;
  
  /** Handle to StorageBase used for storing contacts. */
  private StorageBase store;

  /** Key under which to store contacts. */
  public static final String CONTACTS_KEY = "RangzenContacts";

  /** Hash algorithm to use when obfuscating contacts. */
  private static final String HASH_ALGORITHM = "SHA-256";

  /** MessageDigester to perform hashing on phone numbers. */
  private MessageDigest digester;

  /** 
   * Map of contact names to phone numbers. We drop all but the first phone number
   * for each contact.
   */
  private Map<String, String> nameToPhone = new HashMap<String, String>();

  /**
   * Create a new ContactsGetterStorer that uses the given context to retrieve
   * system resources.
   *
   * @param context A context used to retrieve system resources.
   * @throws NoSuchAlgorithmException
   */
  public ContactsGetterStorer(Context context) throws NoSuchAlgorithmException {
    this.context = context;
    this.store = new StorageBase(context, StorageBase.ENCRYPTION_NONE);
    this.digester = MessageDigest.getInstance(HASH_ALGORITHM);
  }

  /**
   * Access the phone's contacts, collect 1 phone number per name in the
   * contact book, then obfuscate and store the hashed version of those
   * phone numbers.
   *
   * Approach and code borrowed from: 
   * https://stackoverflow.com/questions/12562151/android-get-all-contacts
   */
  public void retrieveAndStoreContacts() {
    ContentResolver cr = context.getContentResolver();
    Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    if (cur.getCount() > 0) {
      while (cur.moveToNext()) {
        String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
        String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        if (Integer.parseInt(
                cur.getString(
                cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
          Cursor pCur = cr.query(
              ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
              null,
              ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
              new String[]{id}, null);
          while (pCur.moveToNext()) {
            String phoneNo = pCur.getString(
                             pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            if (!nameToPhone.containsKey(name)) {
              nameToPhone.put(name, phoneNo);
            }
          }
          pCur.close();
        }
      }
    }
    Set<String> phoneNumberSet = new HashSet<String>();
    phoneNumberSet.addAll(nameToPhone.values());
    normalizeHashAndStorePhoneNumbers(phoneNumberSet);
  }
  
  /**
   * Get the set of obfuscated phone numbers stored for this device.
   *
   * @return A set of hex-string encoded obfuscated phone numbers, or null if
   * none have eve been stored.
   */
  public Set<String> getObfuscatedPhoneNumbers() {
    Set<String> numbers = store.getSet(CONTACTS_KEY);
    return numbers;
  }

  /**
   * Normalize, hash, and store (in the StorageBase) the phone numbers given.
   *
   * @param phoneNumbers The set of phone numbers to store.
   */
  /* package */ void normalizeHashAndStorePhoneNumbers(Set<String> phoneNumbers) {
    Set<String> hashedPhoneNumberSet = new HashSet<String>();
    for (String number : phoneNumbers) {
      number = normalizePhoneNumber(number);
      hashedPhoneNumberSet.add(hashString(number));
    }
    store.putSet(CONTACTS_KEY, hashedPhoneNumberSet);
  }

  /**
   * Given a string, get its bytes and take a hash of them. The hash is of the
   * type configured in the constants for this class. The returned hash is a hex
   * string, since command line utilities tend to output hex strings, making
   * this slightly easier to debug.
   *
   * @param string A string to hash.
   * @return A hex string encoded version of the hash of the string.
   */
  /*package */ String hashString(String string) {
    byte[] output = digester.digest(string.getBytes());
    return bytesToHex(output);
  }

  /**
   * Remove all whitespace and delimiting characters (such as -+.()) from a 
   * phone number. Additionally, if the number begins with a 1, remove that too.
   * (Area codes cannot begin with 1).
   *
   * @return The given string, normalized per the rules described.
   *
   */
  /* package */ static String normalizePhoneNumber(String number) {
    number = number.replaceAll("\\s", "");
    number = number.replaceAll("[)(-.+ ]", "");
    if (number.startsWith("1")) {
      number = number.replaceFirst("1", "");
    }
    return number;
  }

  
  /** Required for below conversion function. */
  final protected static char[] hexArray = "0123456789abcdef".toCharArray();

  /**
   * Convert bytes into a hex string representing those bytes.
   * Borrowed from the first answer to:
   * https://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
   */
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}
