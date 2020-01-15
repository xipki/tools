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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

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
    Integer bruteforceMinLen = null;
    Integer bruteforceMaxLen = null;
    String bruteforceCharsFile= null;

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
      } else if ("--bf-min-len".equals(opt) || "-l".equals(opt)) {
        bruteforceMinLen = Integer.parseInt(value);
      } else if ("--bf-max-len".equals(opt) || "-m".equals(opt)) {
        bruteforceMaxLen = Integer.parseInt(value);
      } else if ("--bf-chars".equals(opt) || "-c".equals(opt)) {
        bruteforceCharsFile = value;
      } else {
        System.out.println("Unknown option '" + opt + "'");
      }
    }

    if ((dictionaryFile == null && bruteforceMaxLen == null)
        || keystoreFile == null) {
      printUsage();
      return;
    }

    long start = System.currentTimeMillis();
    System.out.println("Started at " + new Date(start));

    try {
      byte[] jksBytes = Files.readAllBytes(Paths.get(keystoreFile));

      if (dictionaryFile != null) {
        PasswordIterator passwordIterator = new DictPasswordIterator(dictionaryFile);
        char[] password = disclose(jksBytes, passwordIterator, "Dictionary", start);
        if (password != null) {
          return;
        }
      }

      if (bruteforceMaxLen != null) {
        if (bruteforceMinLen == null) {
          bruteforceMinLen = 1;
        }

        char[] passwordChars = null;
        if (bruteforceCharsFile != null) {
          StringBuilder buf = new StringBuilder(100);
          try (BufferedReader reader =
              new BufferedReader(new FileReader(bruteforceCharsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
              buf.append(line);
            }
          }
          passwordChars = buf.toString().toCharArray();
        }

        PasswordIterator passwordIterator =
            new BruteForcePasswordIterator.BruteForceRangePasswordIterator(
                bruteforceMinLen, bruteforceMaxLen, passwordChars);
        char[] password = disclose(jksBytes, passwordIterator, "Brute-Force", start);
        if (password != null) {
          return;
        }
      }

    } catch (Exception ex) {
      System.err.println("ERROR");
      ex.printStackTrace();
    }

    System.out.println(String.format("Could not disclose password after %d ms",
        + (System.currentTimeMillis() - start) / 1000));
  }

  private static char[] disclose(
      byte[] jksBytes, PasswordIterator passwordIterator,
      String method, long startTime) throws IOException {
    JKSPasswordDiscloser discloser = new JKSPasswordDiscloser(jksBytes, passwordIterator);
    char[] password = discloser.disclosePassword();

    if (password != null) {
      System.out.println(String.format("(%s) Disclosed password '%s' in %d seconds",
          method, new String(password), (System.currentTimeMillis() - startTime) / 1000));
    }
    return password;
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
    buf.append("\t\tDictionary file containing the passwords\n");
    buf.append("\t\tExamples can be found in\n");
    buf.append("\t\t  https://github.com/danielmiessler/SecLists/tree/master/Passwords\n");
    buf.append("\t--bf-chars -c\n");
    buf.append("\t\tFile containing the brute-force password characters\n");
    buf.append("\t--bf-minlen -l\n");
    buf.append("\t\tBrute-force minimal length of the passwords\n");
    buf.append("\t--bf-minlen -m\n");
    buf.append("\t\tBrute-force maximal length of the passwords\n");
    buf.append("\t--keystore -k\n");
    buf.append("\t\tJKS keystore file\n");
    System.out.println(buf.toString());
  }

}
