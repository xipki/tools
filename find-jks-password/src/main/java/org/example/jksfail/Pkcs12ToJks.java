package org.example.jksfail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Enumeration;

public class Pkcs12ToJks {

  public static void main(String[] args) {
    try {
      char[] password = "1234".toCharArray();
      String file = "data/tls-ca-ec";

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

          jks.setKeyEntry(alias, p12.getKey(alias, password), password, p12.getCertificateChain(alias));
        }

        jks.store(os, password);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
