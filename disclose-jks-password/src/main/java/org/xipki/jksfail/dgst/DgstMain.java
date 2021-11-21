package org.xipki.jksfail.dgst;

public class DgstMain {

  public DgstMain() {
  }

  public static void main(String[] args) {
    try {
      Openssl ossl = OpensslWrapper.lib();
      System.out.println(ossl);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
