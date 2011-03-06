package org.sakaiproject.nakamura.api.messagebucket;


/**
 * Indicates something went wrong with a request to the message bucket service.
 */
public class MessageBucketException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -5052522747818623572L;

  public MessageBucketException(String message, Exception e) {
    super(e.getMessage(),e);
  }

  public MessageBucketException(String message) {
    super(message);
  }


}
