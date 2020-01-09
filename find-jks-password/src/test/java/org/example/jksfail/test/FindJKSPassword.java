package org.example.jksfail.test;

import java.io.File;
import java.security.MessageDigest;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.example.jksfail.EncryptedKeyBlob;

public class FindJKSPassword {
  static HexBinaryAdapter hex = new HexBinaryAdapter();

  public static void main(String[] args) {
    byte[] passwd = hex.unmarshal("0031003200330034");
    try {
    EncryptedKeyBlob blob = EncryptedKeyBlob.fromJKS(new File("data/tls-ca-rsa.jks"));

    f(passwd, blob);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static void f(byte[] passwd, EncryptedKeyBlob blob) {
    try  {
      MessageDigest digest = MessageDigest.getInstance("SHA1");
        digest.update(passwd);
        digest.update(blob.getSalt());
        byte[] xorkey = digest.digest();
  
        int plain0 = xorkey[0] ^ blob.getEncrKey()[0];
        int plain1 = xorkey[1] ^ blob.getEncrKey()[1];
        System.out.println(plain0 + " " + plain1);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
