package org.sakaiproject.nakamura.api.messagebucket;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A message bucket contains stuff to be transported
 */
public interface MessageBucket {

  /**
   * @return true if the bucket has content that can be distributed
   */
  boolean isReady();

  /**
   * @param waiter to be added to the list of waiters waiting
   */
  void addWaiter(Waiter waiter);

  /**
   * @param waiter to be removed from the list of waiters waiting.
   */
  void removeWaiter(Waiter waiter);

  /**
   * @param response send the contents of the bucket out over the response.
   * @throws MessageBucketException 
   */
  void send(HttpServletResponse response) throws  MessageBucketException;

  /**
   * Unbind the request that was bound to this bucket using this token.
   * @param token the token that was used to bind with.
   * @param request the request which was bound using the token
   */
  void unbind(String token, HttpServletRequest request);

  void bind(String token, HttpServletRequest request);

}
