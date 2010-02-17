/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.cluster;

import static org.junit.Assert.assertFalse;

import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class IDTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(IDTest.class);
  private static long tstart;
  private static long epoch;
  private Map<BigInteger, BigInteger> hash = new ConcurrentHashMap<BigInteger, BigInteger>();
  private long prev = 0;
  private long next = 0;
  private Object lockObject = new Object();
  private int nrunning = 0;

  @BeforeClass
  public static void beforeClass() {
    GregorianCalendar calendar = new GregorianCalendar(2009,8, 21);
    epoch = calendar.getTimeInMillis();
    tstart = 99;
  }

  @Test
  public void testId() {

    for (int i = 0; i < 10; i++) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          nrunning++;
          try {
            for (int j = 0; j < 100; j++) {
              BigInteger id = getId();
              assertFalse("Failed for " + id + " after " + j, hash.containsKey(id));
              hash.put(id, id);
            }
          } finally {
            nrunning--;
          }

        }

      });
      t.start();
    }

    do {
      LOGGER.info("Running " + nrunning + " Hash Size is " + hash.size());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOGGER.info(e.getMessage(),e);
      }
    } while (nrunning > 0);

    Base64 b64 = new Base64();
    for ( Entry<BigInteger, BigInteger> e : hash.entrySet() ) {
      LOGGER.info(" Entry is "+e.getValue()+" "+ b64.encodeToString(e.getValue().toByteArray()).trim());

    }

  }

  private BigInteger getId() {
    synchronized (lockObject) {
      do {
        next = System.currentTimeMillis()-epoch;
      } while (next == prev);
      BigInteger ret = new BigInteger(String.valueOf(tstart) + String.valueOf(next));
      prev = next;
      return ret;
    }
  }

}
