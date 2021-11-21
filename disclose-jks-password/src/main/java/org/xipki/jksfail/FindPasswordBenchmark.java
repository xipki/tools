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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FinJKSPassword Benchmark.
 *
 * @author Lijun Liao
 *
 */
public class FindPasswordBenchmark {
  
  private static final String PASSWORD = "1234";
  
  private static final String JKS = 
        "/u3+7QAAAAIAAAABAAAAAQAEbWFpbgAAAW+OHlRpAAAA1DCB0TAOBgorBgEEASoCEQEBBQAEgb7p2y7H"
      + "4RNy+WLJljfoTykgUIZkxXSEckgHfyRg5M4rtWMJuGjby7Qp0mOFPexmvn7U1ABJInR5pxF1zABH8CsP"
      + "9vXqotESCvim79JR/DY9ESdexq19zOvZQ6ivKwEi/Y7UUmJ7Pa8sPBuQfaYE/akipTES/ITLInDBFd/d"
      + "l5/ZCpWKBAYE9w9bSfP6Hhae3oLxLWCrCtTqkT3IQfVmBdMdcihcPnsee8uItCegDCou+51nr/XCxieO"
      + "YwnBVq8xAAAAAQAFWC41MDkAAAG3MIIBszCCAVmgAwIBAgIBATAKBggqhkjOPQQDAjBAMQswCQYDVQQG"
      + "EwJERTEOMAwGA1UECgwFeGlwa2kxCzAJBgNVBAsMAkVDMRQwEgYDVQQDDAtUTFMgRGVtbyBDQTAgFw0x"
      + "OTA1MTAwNzI0MTdaGA8yMDU5MDUxMDA3MjQxN1owQDELMAkGA1UEBhMCREUxDjAMBgNVBAoMBXhpcGtp"
      + "MQswCQYDVQQLDAJFQzEUMBIGA1UEAwwLVExTIERlbW8gQ0EwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC"
      + "AASugH43C/aArVcXc+8QF3t8EK9j7XLDjcXOIGNRhbpYfnhoQXL27ivYUKAO6DfcoRjtExiFgiF0X+1x"
      + "p3OsQWGMo0IwQDAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBRj2wt6eIyr/TsDUsiIuES+nSyQuTAO"
      + "BgNVHQ8BAf8EBAMCAQYwCgYIKoZIzj0EAwIDSAAwRQIhAO+2xeUBHWloOWJwO1EY9dLIWFo2r0ygR4VU"
      + "3VH0oVelAiByAkTtC0B3Qq0wLdUo4jH9a5jPMMXaeTjQOfNrkeZa3HIaWTXChw5nW/wBpDHLkTmqi8HP";

  public static void main(String[] args) {
    try {
      doMain(args);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  private static void doMain(String[] args) throws Exception {
    char[] password = PASSWORD.toCharArray();

    final int count = 10 * 1000 * 1000;

    final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    
    byte[] jksBytes = Base64.getDecoder().decode(JKS);

    List<MyRunnable> runnables = new ArrayList<>(threads);
    for (int i = 0; i < threads; i++) {
      runnables.add(new MyRunnable(password, count, jksBytes));
    }

    ExecutorService service = Executors.newFixedThreadPool(threads);

    long start = System.currentTimeMillis();

    for (MyRunnable r : runnables) {
      service.execute(r);
    }

    service.shutdown();
    do {
      try {
        service.awaitTermination(10, TimeUnit.MINUTES);
      } catch (InterruptedException ex) {
      }
    } while (!service.isShutdown());

    long duration = System.currentTimeMillis() - start;
    int sum = count * threads;
    System.out.println("#threads: " + threads + ", #passwords: " + sum + ", duration: "
        + duration + " ms, speed: " + (sum * 1000L / duration) + " /s");
  }

  private static class MyRunnable implements Runnable {

    private char[] password;
    private int count;
    private byte[] jksBytes;

    public MyRunnable(char[] password, int count, byte[] jksBytes) {
      this.count = count;
      this.password = password;
      this.jksBytes = jksBytes;
    }

    @Override
    public void run() {
      LoopPasswordIterator passwordIterator = new LoopPasswordIterator(password, count);

      try {
        JKSPasswordDiscloser.disclosePassword(passwordIterator, jksBytes);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }

  }

  private static class LoopPasswordIterator implements PasswordIterator {

    private final int limit;

    private char[] password;

    private char[] incorrectPassword;

    private int count;

    public LoopPasswordIterator(char[] password, int limit) {
      this.limit = limit;
      this.password = password;
      this.incorrectPassword = Arrays.copyOf(password, password.length);
      this.incorrectPassword[0] = (char) (this.incorrectPassword[0] + 1);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public synchronized boolean hasNext() {
      return count < limit;
    }

    @Override
    public synchronized char[] next() {
      count++;
      if (count == limit) {
        return password;
      } else {
        return incorrectPassword;
      }
    }
  }
}
