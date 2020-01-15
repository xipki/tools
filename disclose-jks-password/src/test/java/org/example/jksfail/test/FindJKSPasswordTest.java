/*
 *
 * Copyright (c) 2013 - 2020 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.jksfail.test;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.xipki.jksfail.DictPasswordIterator;
import org.xipki.jksfail.JKSPasswordDiscloser;
import org.xipki.jksfail.MyUtil;
import org.xipki.jksfail.PasswordIterator;

/**
 * FinJKSPassword test.
 *
 * @author Lijun Liao
 *
 */
public class FindJKSPasswordTest {

  @Test
  public void fixedIteratorFindPassword() throws Exception {
    char[] password = "1234".toCharArray();
    String[] jksFilenames = {"/examples/keystore-ec.jks", "/examples/keystore-rsa.jks"};

    for (String jksFilename : jksFilenames) {
      PasswordIterator passwordIterator = new SinglePasswordIterator(password);
      InputStream jksStream = FindJKSPasswordTest.class.getResourceAsStream(jksFilename);
      byte[] jksBytes = MyUtil.readFully(jksStream);

      char[] passwordFound = JKSPasswordDiscloser.disclosePassword(passwordIterator, jksBytes);
      Assert.assertArrayEquals(jksFilename, password, passwordFound);
    }
  }

  @Test
  public void dictIteratorFindPassword() throws Exception {
    char[] password = "1234".toCharArray();
    String[] jksFilenames = {"/examples/keystore-ec.jks", "/examples/keystore-rsa.jks"};
    for (String jksFilename : jksFilenames) {
      System.out.println(new java.io.File(".").getAbsolutePath());
      PasswordIterator passwordIterator = new DictPasswordIterator(
          "src/test/resources/examples/password-dict.txt");
      InputStream jksStream = FindJKSPasswordTest.class.getResourceAsStream(jksFilename);
      byte[] jksBytes = MyUtil.readFully(jksStream);
      char[] passwordFound = JKSPasswordDiscloser.disclosePassword(passwordIterator, jksBytes);
      Assert.assertArrayEquals(jksFilename, password, passwordFound);
    }
  }

}
