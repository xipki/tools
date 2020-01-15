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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
   * Dictionary based password iterator.
   *
   * @author Lijun Liao
   *
   */
  public class DictPasswordIterator implements PasswordIterator {

    private final String dictionaryFile;

    private final AtomicBoolean cancel;

    private BufferedReader reader;

    private char[] nextPassword;

    private boolean closed;

    public DictPasswordIterator(String dictionaryFile)
        throws IOException {
      this(dictionaryFile, null);
    }

    public DictPasswordIterator(String dictionaryFile, AtomicBoolean cancel)
        throws IOException {
      this.dictionaryFile = dictionaryFile;
      this.reader = new BufferedReader(new FileReader(dictionaryFile));
      this.cancel = cancel == null ? new AtomicBoolean(false) : cancel;
      readNext();
    }

    @Override
    public synchronized boolean hasNext() {
      return !(cancel.get() && nextPassword == null);
    }

    @Override
    public synchronized char[] next() {
      if (cancel.get() || nextPassword == null) {
        return null;
      }

      char[] current = nextPassword;
      readNext();
      return current;
    }

    public String getDictionaryFile() {
      return dictionaryFile;
    }

    private void readNext() {
      nextPassword = null;
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          if (line.isEmpty()) {
            continue;
          }

          nextPassword = line.toCharArray();
          break;
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public synchronized void close() throws IOException {
      if (!closed) {
        reader.close();
        closed = true;
      }
    }

  }
