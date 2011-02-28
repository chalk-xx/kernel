package org.sakaiproject.nakamura.http.qos;

import org.mortbay.util.ajax.Continuation;

import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletRequest;

public class QoSControl {

  private Semaphore semaphore;
  private int priority;
  private Queue<Continuation>[] priorityQueue;
  private long timeout;

  public QoSControl(Queue<Continuation>[] priorityQueue, int nRequests, int priority,
      long timeout) {
    semaphore = new Semaphore(nRequests, true);
    this.priority = priority;
    this.priorityQueue = priorityQueue;
    this.timeout = timeout;
  }

  public Semaphore getSemaphore() {
    return semaphore;
  }

  public int getPriority(ServletRequest request) {
    return priority;
  }

  public Queue<Continuation>[] getPriorityQueue() {
    return priorityQueue;
  }

  public long getTimeout() {
    return timeout;
  }

}
