package org.xipki.jksfail.dgst;

import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;

public interface Openssl {
  // unsigned char *SHA1(const unsigned *d, size_t n, unsigned char *md)
  int SHA1(
      @In byte[] d,
      @In int n,
      @Out byte[] md);

}
