package org.sakaiproject.nakamura.http.qos;

import org.mortbay.util.ajax.Continuation;
import org.sakaiproject.nakamura.api.http.qos.QoSToken;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;

public class QoSTokenImpl implements QoSToken {

  private QoSControl qoSControl;
  private int priority;

  public QoSTokenImpl(QoSControl qoSControl, ServletRequest request) {
    this.qoSControl = qoSControl;
    priority = qoSControl.getPriority(request);
  }


  public Object getMutex() {
    return this;
  }

  public void release() {
    Queue<Continuation>[] priorityQueue = qoSControl.getPriorityQueue();
    for (int p = priorityQueue.length; p-- > 0;) {
      Continuation continutaion = priorityQueue[p].poll();
      if (continutaion != null && continutaion.isResumed()) {
        continutaion.resume();// this assumes only 1 will be resumed, that may not be right and we might want to think how the proprity queues are managed.
        break;
      }
    }
    qoSControl.getSemaphore().release();
  }

  public long getSuspendTime() {
    return qoSControl.getTimeout();
  }

  public void queue(Continuation continuation) {
    qoSControl.getPriorityQueue()[priority].add(continuation);
  }

  public boolean acquire(long waitMs) throws InterruptedException {
    return qoSControl.getSemaphore().tryAcquire(waitMs, TimeUnit.MILLISECONDS);
  }

  public void acquire() throws InterruptedException {
    qoSControl.getSemaphore().acquire();
  }

}
