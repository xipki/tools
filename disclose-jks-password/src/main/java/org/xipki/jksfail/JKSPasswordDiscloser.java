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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry points to disclose the password.
 *
 * @author Lijun Liao
 *
 */
public class JKSPasswordDiscloser {

  private final EncryptedKeyBlob jksEncrytedKey;

  private final PasswordIterator passwordIterator;

  private final AtomicBoolean passwordFound = new AtomicBoolean(false);

  private final int nThreads;

  private char[] password;

  public JKSPasswordDiscloser(int numThreads, byte[] jksBytes,
      PasswordIterator passwordIterator) throws IOException {
    this.nThreads = Math.max(1, numThreads);
    this.jksEncrytedKey = EncryptedKeyBlob.fromJKS(jksBytes);
    this.passwordIterator = passwordIterator;
  }

  private class PasswordIteratorWrapper implements PasswordIterator {

    @Override
    public void close() throws IOException {
      passwordIterator.close();
    }

    @Override
    public boolean hasNext() {
      return passwordFound.get() ? false : passwordIterator.hasNext();
    }

    @Override
    public char[] next() {
      return passwordFound.get() ? null : passwordIterator.next();
    }

  }

  private class MyRunnable implements Runnable {

    @Override
    public void run() {
      char[] pwd;
      try {
        pwd = disclosePassword(new PasswordIteratorWrapper(), jksEncrytedKey);
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }

      if (pwd != null) {
        password = pwd;
        passwordFound.set(true);
      }
    }

  }

  public char[] disclosePassword() throws IOException {
    if (nThreads == 1) {
      password = disclosePassword(passwordIterator, jksEncrytedKey);
      return password;
    }

    MyRunnable[] runnables = new MyRunnable[nThreads];
    for (int i = 0; i < nThreads; i++) {
      runnables[i] = new MyRunnable();
    }

    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    for (MyRunnable m : runnables) {
      executor.execute(m);
    }

    executor.shutdown();
    while (true) {
      try {
        if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
          break;
        }
      } catch (InterruptedException ex) {
      }
    }

    return password;
  }

  public static char[] disclosePassword(
      PasswordIterator passwordIterator,
      byte[] jksBytes) throws IOException {
    return disclosePassword(passwordIterator, EncryptedKeyBlob.fromJKS(jksBytes));
  }

  public static char[] disclosePassword(
      PasswordIterator passwordIterator,
      EncryptedKeyBlob blob) throws IOException {
    try {
      final byte[] encrKey = blob.getEncrKey();
      final int encKeyLen = encrKey.length;
      final byte[] salt = blob.getSalt();

      // fixed the expected starting bytes
      byte[] lengthBytes;
      if (encKeyLen < 130) {
        int newLen = encKeyLen - 2;
        lengthBytes = new byte[1];
        lengthBytes[0] = (byte) newLen;
      } else if (encKeyLen < 259) {
        int newLen = encKeyLen - 3;
        lengthBytes = new byte[2];
        lengthBytes[0] = (byte) 0x81;
        lengthBytes[1] = (byte) newLen;
      } else {
        int newLen = encKeyLen - 4;
        lengthBytes = new byte[3];
        lengthBytes[0] = (byte) 0x82;
        lengthBytes[1] = (byte) (newLen >> 8);
        lengthBytes[2] = (byte) newLen;
      }

      byte[] expectedPlainStart = new byte[1 + lengthBytes.length + 4];
      expectedPlainStart[0] = 0x30;
      System.arraycopy(lengthBytes, 0, expectedPlainStart, 1, lengthBytes.length);
      System.arraycopy(
          new byte[] {2, 1, 0, 0x30}, 0,
          expectedPlainStart, 1 + lengthBytes.length, 4);

      final int expectedStartLen = expectedPlainStart.length;
      byte[] expectedHash = new byte[expectedStartLen];
      for (int i = 0; i < expectedStartLen; i++) {
        expectedHash[i] = (byte) (encrKey[i] ^ expectedPlainStart[i]);
      }

      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA1");
      } catch (NoSuchAlgorithmException ex) {
        throw new IllegalStateException(ex);
      }

      char[] password;
      while ((password = passwordIterator.next()) != null) {
        md.update(passwordToBytes(password));
        md.update(salt);
        byte[] xorKey = md.digest();

        boolean found = true;
        for (int i = 0; i < expectedStartLen; i++) {
          if (xorKey[i] != expectedHash[i]) {
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
