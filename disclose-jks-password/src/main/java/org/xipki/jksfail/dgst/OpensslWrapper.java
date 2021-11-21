package org.xipki.jksfail.dgst;

import jnr.ffi.LibraryLoader;

public class OpensslWrapper {
  
  public static Openssl lib() {
    return SingletonHolder.INSTANCE;
  }

  private static final class SingletonHolder {
    public static final Openssl INSTANCE = init();
    
    private static Openssl init() {
      return LibraryLoader.create(Openssl.class).load("crypto");
    }
  }
  
}
