package org.xipki.jksfail.dgst;

import jnr.ffi.LibraryLoader;

public class OpensslWrapper {
  
  public static Openssl lib() {
    return SingletonHolder.INSTANCE;
  }

  private static final class SingletonHolder {
    public static final Openssl INSTANCE = init();
    
    private static Openssl init() {
      Openssl ossl = null;
      try {
        ossl = LibraryLoader.create(Openssl.class).load("crypto2");
        ossl.SHA1(new byte[1], 1, new byte[20]);
      } catch (Throwable t) {
        ossl = null;
      }
      return ossl;
    }
  }
  
}
