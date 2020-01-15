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
import java.util.ArrayList;
import java.util.List;

/**
 * Brute force password iterator.
 *
 * @author Lijun Liao
 *
 */
public class BruteForcePasswordIterator {

  private static final String DFLT_PASSWORD_CHARS =
      "1234567890"
      + "abcdefghijklmnopqrstuvwxyz"
      + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "_.-!@*$?&%+\\/'^:(){}[]~ ,";

  /**
   * Dictionary based password iterator.
   *
   */
  private static class UnsyncBruteForceFixedLengthPasswordIterator
  implements PasswordIterator {

    private final char[] passwordChars;

    private final int n;

    private final int length;

    private final int[] indexes;

    private boolean endReached;

    UnsyncBruteForceFixedLengthPasswordIterator(int length, char[] passwordChars) {
      if (length < 1) {
        throw new IllegalArgumentException("length must not be non-positive: " + length);
      }

      if (passwordChars == null || passwordChars.length == 0) {
        this.passwordChars = DFLT_PASSWORD_CHARS.toCharArray();
      } else {
        // remove duplicated chars
        List<Character> l = new ArrayList<>(passwordChars.length);
        for (int i = 0; i <passwordChars.length; i++) {
          char c = passwordChars[i];
          if (!l.contains(c)) {
            l.add(c);
          }
        }

        this.passwordChars = new char[l.size()];
        for (int i = 0; i < l.size(); i++) {
          this.passwordChars[i] = l.get(i);
        }
      }

      this.length = length;
      this.indexes = new int[length];
      this.n = this.passwordChars.length;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean hasNext() {
      return !endReached;
    }

    @Override
    public char[] next() {
      if (endReached) {
        return null;
      }

      for (int i = length - 1; i >=0; i--) {
        indexes[i]++;
        if (indexes[i] == n) {
          if (i == 0) {
            endReached = true;
            return null;
          } else {
            indexes[i] = 0;
          }
        } else {
          break;
        }
      }

      char[] password = new char[length];
      for (int i = 0; i < length; i++) {
        password[i] = passwordChars[indexes[i]];
      }
      return password;
    }

  }

  /**
   * Dictionary based password iterator.
   *
   */
  public static class BruteForceFixedLengthPasswordIterator
  extends UnsyncBruteForceFixedLengthPasswordIterator {

    public BruteForceFixedLengthPasswordIterator(
        int length, char[] passwordChars) {
      super(length, passwordChars);
    }

    public synchronized boolean hasNext() {
      return super.hasNext();
    }

    @Override
    public synchronized char[] next() {
      return super.next();
    }

  }

  public static class BruteForceRangePasswordIterator implements PasswordIterator {

    private final int maxLength;

    private final char[] passwordChars;

    private int length;

    private UnsyncBruteForceFixedLengthPasswordIterator passwordIterator;

    public BruteForceRangePasswordIterator(
        int minLength, int maxLength, char[] passwordChars) {
      if (minLength < 1) {
        throw new IllegalArgumentException("minLength must not be non-positive: " + minLength);
      }

      if (maxLength < minLength) {
        throw new IllegalArgumentException("minLength must not be greater than maxLength");
      }

      this.maxLength = maxLength;
      this.passwordChars = passwordChars;
      this.length = minLength;
      this.passwordIterator = new UnsyncBruteForceFixedLengthPasswordIterator(
                              this.length, passwordChars);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public synchronized boolean hasNext() {
      return length < maxLength || passwordIterator.hasNext();
    }

    @Override
    public synchronized char[] next() {
      char[] password = passwordIterator.next();
      if (password == null && length < maxLength) {
        length++;
        passwordIterator = new UnsyncBruteForceFixedLengthPasswordIterator(length, passwordChars);
        password = passwordIterator.next();
      }

      return password;
    }

  }

}
