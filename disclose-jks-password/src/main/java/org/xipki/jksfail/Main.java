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

/**
 * Main class for the command line to disclose the password.
 *
 * @author Lijun Liao
 *
 */
public class Main {

  public static void main(String[] args) {
    int n = (args == null) ? 0 : args.length;
    if (n == 0 || (n > 0 && args[0].equals("--help"))) {
      printUsage();
      return;
    }

    String dictionaryFile = null;
    String keystoreFile = null;
    for (int i = 0; i < n; i += 2) {
      if (i == n - 1) {
        printUsage();
        return;
      }

      String opt = args[i];
      String value = args[i + 1];

      if ("--dict".equals(opt) || "-d".equals(opt)) {
        dictionaryFile = value;
      } else if ("--keystore".equals(opt) || "-k".equals(opt)) {
        keystoreFile = value;
      }
    }

    if (dictionaryFile == null || keystoreFile == null) {
      printUsage();
      return;
    }

    try {
      PasswordIterator passwordIterator = new DictPasswordIterator(dictionaryFile);
      char[] password =
          JKSPasswordDiscloser.disclosePassword(passwordIterator, new File(keystoreFile));
      if (password == null) {
        System.out.println("Could not disclose password");
      } else {
        System.out.println("Disclosed password " + new String(password));
      }
    } catch (Exception ex) {
      System.err.println("ERROR");
      ex.printStackTrace();
    }
  }

  private static void printUsage() {
    StringBuilder buf = new StringBuilder();
    buf.append("DESCRIPTION\n");
    buf.append("\tDisclose JKS keystore password\n");
    buf.append("SYNTAX\n");
    buf.append("\tWindows: run [options]\n");
    buf.append("\t  Linux: ./run.sh [options]\n");
    buf.append("\t--help\n");
    buf.append("\t\tPrint this usage\n");
    buf.append("\t--dict -d\n");
    buf.append("\t\tDictionary file containing passwords\n");
    buf.append("\t--keystore -k\n");
    buf.append("\t\tJKS keystore file\n");
    System.out.println(buf.toString());
  }

}
