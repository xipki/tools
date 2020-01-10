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

import org.example.jksfail.FindJKSPassword;
import org.example.jksfail.PasswordIterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * FinJKSPassword test.
 *
 * @author Lijun Liao
 *
 */
public class FindJKSPasswordTest {

  @Test
  public void dictFindPassword() throws Exception {
    char[] password = "1234".toCharArray();
    String[] jksFilenames = {"/data/tls-ca-ec.jks", "/data/tls-ca-rsa.jks"};
    for (String jksFilename : jksFilenames) {
      try {
        PasswordIterator passwordIterator = new SinglePasswordIterator(password);
        InputStream jksStream = FindJKSPasswordTest.class.getResourceAsStream(jksFilename);
        char[] passwordFound = FindJKSPassword.dictFindPassword(passwordIterator, jksStream);
        Assert.assertArrayEquals(jksFilename, password, passwordFound);
      } catch (Throwable th) {
        Assert.fail(jksFilename + ": " + th.getMessage());
      }
    }

  }

}
