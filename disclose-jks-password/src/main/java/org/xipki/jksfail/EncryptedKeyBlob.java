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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Encrypted key entry extracted from the JKS keystore.
 *
 * @author Lijun Liao
 *
 */
class EncryptedKeyBlob {

  private static final int MAGIC = 0xfeedfeed;
  private static final int VERSION_1 = 0x01;
  private static final int VERSION_2 = 0x02;

  private static final int SALT_LEN = 20; // the salt length
  private static final int DIGEST_LEN = 20;

  // defined by JavaSoft: "1.3.6.1.4.1.42.2.17.1.1"
  private static final byte[] KEY_PROTECTOR_OID =
      new byte[] {0x06, 0x0a, 0x2b, 0x06, 0x01, 0x04, 0x01, 0x2a, 0x02, 0x11, 0x01, 0x01};

  private final byte[] salt;

  private final byte[] encrKey;

  private static class PosLen {
    final int pos;
    final int len;

    PosLen(int pos, int len) {
      this.pos = pos;
      this.len = len;
    }

    @Override
    public String toString() {
      return "pos: " + pos + ", len: " + len;
    }
  }

  public EncryptedKeyBlob(byte[] salt, byte[] encrKey) {
    this.salt = salt;
    this.encrKey = encrKey;
  }

  public byte[] getSalt() {
    return salt;
  }

  public byte[] getEncrKey() {
    return encrKey;
  }

  public static EncryptedKeyBlob fromJKS(File jksFile) throws IOException {
    return fromJKS(Files.readAllBytes(jksFile.toPath()));
  }

  public static EncryptedKeyBlob fromJKS(InputStream jk) throws IOException {
    return fromJKS(readFully(jk));
  }

  public static EncryptedKeyBlob fromJKS(byte[] jksBytes) throws IOException {
    int xMagic = readInt(jksBytes, 0, 4);
    int xVersion = readInt(jksBytes, 4, 4);

    if (xMagic != MAGIC ||
        (xVersion != VERSION_1 && xVersion != VERSION_2)) {
        throw new IOException("Invalid keystore format");
    }

    int count = readInt(jksBytes, 8, 4);
    int off = 12;
    for (int i = 0; i < count; i++) {
      int tag = readInt(jksBytes, off, 4);
      off += 4;

      if (tag == 1) { // private key entry
        // skip alias
        int utfLen = readInt(jksBytes, off, 2);
        off += 2 + utfLen;

        // skip the (entry creation) date (8 bytes)
        off += 8;

        // Read the private key
        //int encryptedPrivateKeyInfoLen = bigEndianToInt(dis, off, 4);
        off += 4;

        // Parse the EncryptedPrivateKeyInfo
        if (0x30 != jksBytes[off]) {
          throw new IOException("Expected EncryptedPrivateKeyInfo");
        }
        off++;

        PosLen posLenEncryptedPrivateKeyInfo = readASN1PosLength(jksBytes, off);

        // algorithm
        off = posLenEncryptedPrivateKeyInfo.pos;
        PosLen posLenAlgorithm = readASN1PosLength(jksBytes, off);

        off = posLenAlgorithm.pos;
        if (!areEqual(jksBytes, off,
            KEY_PROTECTOR_OID, 0, KEY_PROTECTOR_OID.length) ) {
          throw new IOException("Unsupported encryption algorithm");
        }

        // encryptedData
        off = posLenAlgorithm.pos + posLenAlgorithm.len;
        if (0x04 != jksBytes[off]) {
          throw new IOException("Expected OCTET STRING");
        }
        off++;

        PosLen posLenData = readASN1PosLength(jksBytes, off);
        off = posLenData.pos;
        int len = posLenData.len;

        /*
         * Get the salt associated with this key (the first SALT_LEN bytes of
         * <code>protectedKey</code>)
         */
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(jksBytes, off, salt, 0, SALT_LEN);

        int encrKeyLen = len - SALT_LEN - DIGEST_LEN;

        // Get the encrypted key portion and store it in "encrKey"
        byte[] encrKey = new byte[encrKeyLen];
        System.arraycopy(jksBytes, off + SALT_LEN, encrKey, 0, encrKeyLen);
        return new EncryptedKeyBlob(salt, encrKey);
      } else if (tag == 2) { // trusted certificate entry
        // skip alias
        int utfLen = readInt(jksBytes, off, 2);
        off += 2 + utfLen;

        // skip the (entry creation) date (8 bytes)
        off += 8;

        // Read the trusted certificate
        if (xVersion == 2) {
            // skip the certificate type
          // skip alias
          utfLen = readInt(jksBytes, off, 2);
          off += 2 + utfLen;
        }

        // skip the certificate
        int certLen = readInt(jksBytes, off, 4);
        off += 4 + certLen;
      } else {
        throw new IOException("Unrecognized keystore entry: " +
                tag);
      }
    }

    return null;
  }

  static byte[] readFully(InputStream in) throws IOException {
    try {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      int readed = 0;
      byte[] buffer = new byte[2048];
      while ((readed = in.read(buffer)) != -1) {
        bout.write(buffer, 0, readed);
      }

      return bout.toByteArray();
    } finally {
      try {
        in.close();
      } catch (IOException ex) {
      }
    }
  }

  private static PosLen readASN1PosLength(byte[] bytes, int offset) throws IOException {
    int b0 = 0xff & bytes[offset];
    if (b0 < 127) {
      return new PosLen(offset + 2, 0xff & bytes[offset + 1]);
    } else {
      int lengthCount = b0 & 0x7;
      if (lengthCount > 4 || lengthCount < 1) {
        throw new IOException("invalid count of length bytes: " + lengthCount);
      }

      int len = readInt(bytes, offset + 1, lengthCount);
      return new PosLen(offset + 1 + lengthCount, len);
    }
  }

  private static int readInt(byte[] bs, int off, int len)
  {
      int n = 0xff & bs[off];
      for (int i = 1; i < len; i++) {
        n <<= 8;
        n |= 0xff & bs[off + i];
      }
      return n;
  }

  private static boolean areEqual(byte[] a1, int a1Pos, byte[] a2, int a2Pos, int len) {
    if (a1Pos + len > a1.length || a2Pos + len > a2.length) {
      throw new IndexOutOfBoundsException("len is too large");
    }

    for (int i = 0; i < len; i++) {
      if (a1[a1Pos + i] != a2[a2Pos + i]) {
        return false;
      }
    }

    return true;
  }

}
