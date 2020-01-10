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

package org.example.jksfail.test;

import java.io.IOException;

import org.xipki.jksfail.PasswordIterator;

/**
 * Password iterator with only one constant password.
 * @author Lijun Liao
 *
 */
public class SinglePasswordIterator implements PasswordIterator {

  private char[] password;

  public SinglePasswordIterator(char[] password) {
    this.password = password;
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public synchronized boolean hasNext() {
    return password != null;
  }

  @Override
  public synchronized char[] next() {
    char[] current = password;
    password = null;
    return current;
  }

}
