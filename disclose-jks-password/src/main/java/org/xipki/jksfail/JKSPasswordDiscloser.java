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

package org.xipki.jksfail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Entry points to disclose the password.
 *
 * @author Lijun Liao
 *
 */
public class JKSPasswordDiscloser {

  public static char[] disclosePassword(
      PasswordIterator passwordIterator,
      File jksFile)
          throws IOException {
    byte[] jksBytes = Files.readAllBytes(jksFile.toPath());
    return disclosePassword(passwordIterator, jksBytes);
  }

  public static char[] disclosePassword(
      PasswordIterator passwordIterator,
      InputStream jksStream)
          throws IOException {
    byte[] jksBytes = EncryptedKeyBlob.readFully(jksStream);
    return disclosePassword(passwordIterator, jksBytes);
  }

  public static char[] disclosePassword(
      PasswordIterator passwordIterator,
      byte[] jksBytes)
          throws IOException {
    try {
      EncryptedKeyBlob blob = EncryptedKeyBlob.fromJKS(jksBytes);
      final byte[] encrKey = blob.getEncrKey();
      final int encKeyLen = encrKey.length;
      final byte[] salt = blob.getSalt();

      // fixed the expected starting bytes
      int[] lengthBytes;
      if (encKeyLen < 130) {
        int newLen = encKeyLen - 2;
        lengthBytes = new int[1];
        lengthBytes[0] = 0xff & newLen;
      } else if (encKeyLen < 259) {
        int newLen = encKeyLen - 3;
        lengthBytes = new int[2];
        lengthBytes[0] = 0x81;
        lengthBytes[1] = 0xff & newLen;
      } else {
        int newLen = encKeyLen - 4;
        lengthBytes = new int[3];
        lengthBytes[0] = 0x82;
        lengthBytes[1] = 0xff & (newLen >> 8);
        lengthBytes[2] = 0xff & newLen;
      }

      int[] expectedStart = new int[1 + lengthBytes.length + 4];
      expectedStart[0] = 0x30;
      System.arraycopy(lengthBytes, 0, expectedStart, 1, lengthBytes.length);
      System.arraycopy(
          new int[] {2, 1, 0, 0x30}, 0,
          expectedStart, 1 + lengthBytes.length, 4);

      final int expectedStartLen = expectedStart.length;

      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA1");
      } catch (NoSuchAlgorithmException ex) {
        throw new IllegalStateException(ex);
      }

      while (passwordIterator.hasNext()) {
        char[] password = passwordIterator.next();
        md.update(passwordToBytes(password));
        md.update(salt);
        byte[] xorKey = md.digest();

        boolean found = true;
        for (int i = 0; i < expectedStartLen; i++) {
          int plain = 0xff & (xorKey[i] ^ encrKey[i]);
          if (plain != expectedStart[i]) {
            found = false;
            break;
          }
        }

        if (found) {
          return password;
        }
      }

      return null;
    } finally {
      passwordIterator.close();
    }
  }

  private static byte[] passwordToBytes(char[] password) {
    byte[] bytes = new byte[password.length << 1];
    for (int i = 0, j = 0; i < password.length; i++) {
      bytes[j++] = (byte) (password[i] >> 8);
      bytes[j++] = (byte) password[i];
    }
    return bytes;
  }

}
