package org.sakaiproject.nakamura.messagebucket;

import org.mortbay.util.ajax.Continuation;
import org.sakaiproject.nakamura.api.messagebucket.Waiter;

/**
 * Waits using a continuation
 */
public class ContinuationWaiter implements Waiter {

  private Continuation continuation;

  public ContinuationWaiter(Continuation continuation) {
    this.continuation = continuation;
  }

  public void resume() {
    continuation.resume();    
  }

}
