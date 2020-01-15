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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.xipki.jksfail.JKSPasswordDiscloser;
import org.xipki.jksfail.MyUtil;
import org.xipki.jksfail.PasswordIterator;

/**
 * FinJKSPassword Benchmark.
 *
 * @author Lijun Liao
 *
 */
public class FindPasswordBenchmark {

  public static void main(String[] args) {
    char[] password = "1234".toCharArray();
    String jksFilename = "/examples/keystore-ec.jks";

    final int count = 10 * 1000 * 1000;

    final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    List<MyRunnable> runnables = new ArrayList<>(threads);
    for (int i = 0; i < threads; i++) {
      runnables.add(new MyRunnable(password, count, jksFilename));
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
    private String jksFilename;

    public MyRunnable(char[] password, int count, String jksFilename) {
      this.count = count;
      this.password = password;
      this.jksFilename = jksFilename;
    }

    @Override
    public void run() {
      LoopPasswordIterator passwordIterator = new LoopPasswordIterator(password, count);
      InputStream jksStream = FindPasswordBenchmark.class.getResourceAsStream(jksFilename);

      try {
        byte[] jksBytes = MyUtil.readFully(jksStream);
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
