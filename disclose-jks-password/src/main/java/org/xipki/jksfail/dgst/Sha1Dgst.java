package org.xipki.jksfail.dgst;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Sha1Dgst {
  
  private static AtomicBoolean printed = new AtomicBoolean();
  
  private static boolean disableOpenssl = System.getProperty("disable.ossl") != null;

  private Sha1Dgst() {
  }
  
  public static Sha1Dgst getInstance() {
    Openssl ossl = OpensslWrapper.lib();
    if (!disableOpenssl && ossl != null) {
      if (!printed.getAndSet(true)) {
        System.out.println("Use OpenSSL");
      }
      return new Sha1Dgst() {
        @Override
        public byte[] sha1(byte[] data) {
          byte[] md = new byte[20];
          ossl.SHA1(data, data.length, md);
          return md;
        }
      };
    } else {
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA1");
      } catch (NoSuchAlgorithmException ex) {
        throw new IllegalStateException(ex);
      }

      if (!printed.getAndSet(true)) {
        System.out.println("Use JDK Provider " + md.getProvider().getName());
      }
      
      return new Sha1Dgst() {
        @Override
        public byte[] sha1(byte[] data) {
          return md.digest(data);
        }
      };
    }
  }
  
  public abstract byte[] sha1(byte[] data);
  
}
