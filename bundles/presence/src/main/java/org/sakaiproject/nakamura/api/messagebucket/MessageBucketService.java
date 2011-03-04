package org.sakaiproject.nakamura.api.messagebucket;



import javax.servlet.http.HttpServletRequest;


public interface MessageBucketService {


  /**
   * Get the bucket identified by the token.
   * @param token
   * @return
   * @throws MessageBucketException
   */
  MessageBucket getBucket(String token) throws MessageBucketException;

  /**
   * Generate a token for the userID in the context. This token may 
   * @param userId
   * @param context
   * @return
   * @throws MessageBucketException
   */
  String getToken(String userId, String context) throws MessageBucketException;

  String getBucketUrl(HttpServletRequest request, String context)
      throws MessageBucketException;

}
