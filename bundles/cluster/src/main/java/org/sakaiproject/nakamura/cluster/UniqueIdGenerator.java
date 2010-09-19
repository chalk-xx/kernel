package org.sakaiproject.nakamura.cluster;

import org.sakaiproject.nakamura.util.StringUtils;

import java.math.BigInteger;
import java.util.GregorianCalendar;

public class UniqueIdGenerator {

  private Object lockObject = new Object();
  private long next = 0;
  private long epoch;
  private long last = 0;
  private long micro = 0;
  private int serverId;
  private int rollover;

  public UniqueIdGenerator(int serverId) {
    this.serverId = serverId;
    GregorianCalendar calendar = new GregorianCalendar(2010, 8, 6);
    epoch = calendar.getTimeInMillis();
  }

  public String nextId() {
    return StringUtils.encode(nextIdNum().toByteArray(), StringUtils.URL_SAFE_ENCODING);
  }
  
  public BigInteger nextIdNum() {
    // single threaded this benchmarks at 0.5 ns per invocation, so rollover is possible
    synchronized (lockObject) {
      next = System.currentTimeMillis() - epoch;
      if (next == last) { // 2 in the same ms
        micro++;
      } else if (next < last) { // roll over happend in this ms
        next = last;
        micro++;
      } else { // new ms
        micro = 0;
        last = next;
      }
      // catch any rollover in micro.
      if (micro > 999) {
        rollover++;
        next++;
        micro = 0;
        last = next;
      }
    }
    // Collision analysis
    // The server number is unique in the cluster so no 2 servers with the same number can
    // exist at the same time
    // The Id Num is of the form 1SSSNNNN where SS ranges from 0 to 999 servers in a
    // cluster and NNNN is a real positive number.
    // Even when NNNN rols over to 1NNNN and again to 2NNNN there is no collision since
    // the server part of the number is prefixed
    // by 1 as in 1SSSS therefore this ID can never collide in the cluster or by rollover
    // provided we have < 9001 servers in the cluster.
    BigInteger idNum = BigInteger.valueOf(next*1000+micro);
    idNum = idNum.multiply(BigInteger.valueOf(10000));
    idNum = idNum.add(BigInteger.valueOf(serverId));
    return idNum;
  }
  
  public int getRollover() {
    return rollover;
  }

}
