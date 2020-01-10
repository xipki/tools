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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Enumeration;

/**
 * Util class to convert from PKCS#12 keystore to JKS keystore.
 *
 * @author Lijun Liao
 *
 */

public class Pkcs12ToJks {

  public static void main(String[] args) {
    try {
      char[] password = "1234".toCharArray();
      String file = "src/test/resources/data/tls-ca-ec";

      try (InputStream is = new FileInputStream(file + ".p12");
          OutputStream os = new FileOutputStream(file + ".jks")) {
        KeyStore p12 = KeyStore.getInstance("PKCS12");
        p12.load(is, password);

        KeyStore jks = KeyStore.getInstance("JKS");
        jks.load(null, password);

        Enumeration<String> aliases = p12.aliases();
        while (aliases.hasMoreElements()) {
          String alias = aliases.nextElement();
          if (!p12.isKeyEntry(alias)) {
            continue;
          }

          jks.setKeyEntry(alias,
              p12.getKey(alias, password), password,
              p12.getCertificateChain(alias));
        }

        jks.store(os, password);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
