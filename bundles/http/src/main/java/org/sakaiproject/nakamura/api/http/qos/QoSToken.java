package org.sakaiproject.nakamura.api.http.qos;

import org.mortbay.util.ajax.Continuation;

/**
 * A QoSToken is a class that can be added to the request as an attribute before the QoSFilter processes the request to modify the way in which the 
 * request us handled.
 */
public interface QoSToken {
  /**
   * The name of the attribute where this object must be added to the request
   */
  public static final String CONTROL_ATTR = QoSToken.class.getName();

  /**
   * @return a lock object to prevent concurrent access to the continuation
   */
  Object getMutex();

  /**
   * Perform any necessary release operations to release any semaphores, or otherwise, that were aquired. This method should also notify other requests that are suspended waiting on resources.
   */
  void release();

  /**
   * @return the max time in ms that this request should be suspended.
   */
  long getSuspendTime();

  /**
   * Aquire a semaphore to allow this request to continue.
   * @param waitMs the time in ms to wait for the semaphore processing to complete (dont confuse with sustend time which is the total time the request may wait for)
   * @return true of a semaphore was granted
   * @throws InterruptedException
   */
  boolean acquire(long waitMs) throws InterruptedException;

  /**
   * Acquire already acquired semaphore.
   * @throws InterruptedException
   */
  void acquire() throws InterruptedException;

  /**
   * Queue a continuation pending more resources
   * @param continuation
   */
  void queue(Continuation continuation);

}
