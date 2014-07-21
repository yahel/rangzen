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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.denovogroup.rangzen.MainActivity;
import org.denovogroup.rangzen.StorageBase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
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

/**
 * Unit tests for Rangzen's ContactsGetterStorer class.
 */
@Config(manifest="./apps/rangzen/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../res/org/denovogroup/rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class ContactsGetterStorerTest {
  /** The instance of ContactsGetterStorer we're using for tests. */
  private ContactsGetterStorer getterStorer;

  /** The app instance we're using to pass to ContactsGetterStorer. */
  private MainActivity activity;

  /** Some test numbers. */
  private String[] testNumbers = {"1-(800)-975-3369",
                                  "555.555.5555",
                                  "+44 131 223 4596",
                                  "+44 (0)131 888 8888"};

  @Before
  public void setUp() throws NoSuchAlgorithmException {
    activity = Robolectric.buildActivity(MainActivity.class).create().get();
    getterStorer = new ContactsGetterStorer(activity);
  }

  /**
   * Tests a few values to ensure we're doing hashing correctly.
   * (test values from http://www.nsrl.nist.gov/testdata/, as well as from my brain).
   */
  @Test
  public void testSHA256Hash() {
    // From NIST's website (see Javadoc).
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                 getterStorer.hashString("abc"));
    assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
                 getterStorer.hashString("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"));
    StringBuilder oneMillionAs = new StringBuilder(1000000);
    for (int i=0; i<1000000; i++) {
      oneMillionAs.append("a");
    }
    assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                  getterStorer.hashString(oneMillionAs.toString()));

    // Some more arbitrarily chosen tests. I calculated the hashes using
    // > echo -n "string" | sha256sum
    // and copypasted them here. I did the same to verify that the above NIST
    // hashes came out the same with my method to ensure that my method is doing
    // the same as NIST's method (which is the same as our code's method).
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                 getterStorer.hashString(""));

    assertEquals("fa2163d2aad8d63c1665ae262f5f4f21a4d76f5bd0d18917ba5979ea3b3c07b9",
                 getterStorer.hashString("(800) 555-5555"));
  }

  /**
   * Test that normalization of phone numbers works as intended.
   */
  @Test
  public void testNormalizingPhoneNumbers() {
    assertEquals("8001234567", ContactsGetterStorer.normalizePhoneNumber("1-800-123-4567"));
    assertEquals("8001234567", ContactsGetterStorer.normalizePhoneNumber("800-123-4567"));
    assertEquals("2344567890", ContactsGetterStorer.normalizePhoneNumber("   1-(234) 456.7890 "));
    assertEquals("2344567890", ContactsGetterStorer.normalizePhoneNumber("   +1-(234) 456.7890 "));
    assertEquals("441312234596", ContactsGetterStorer.normalizePhoneNumber("+44 131 223 4596"));
    assertEquals("441312234596", ContactsGetterStorer.normalizePhoneNumber("\t+44 131 223 4596\n"));
  }

  /**
   * Test that calling normalizeHashAndStorePhoneNumbers ends up with the right
   * number of phone numbers in storage. Also tests that each of the appropriate
   * numbers' hash is retrieved.
   */
  @Test
  public void testStorePhoneNumbers() {
    Set<String> numbers = new HashSet<String>(Arrays.asList(testNumbers));
    getterStorer.normalizeHashAndStorePhoneNumbers(numbers);
    Set<String> retrievedNumbers = getterStorer.getObfuscatedPhoneNumbers();
    assertEquals(numbers.size(), retrievedNumbers.size());

    for (String number : numbers) {
      number = ContactsGetterStorer.normalizePhoneNumber(number);
      number = getterStorer.hashString(number);
      assertTrue(retrievedNumbers.contains(number));
    }
  }

  @Test
  public void testNullOnUnsetContacts() {
    assertNull(getterStorer.getObfuscatedPhoneNumbers());
  }
  
}
