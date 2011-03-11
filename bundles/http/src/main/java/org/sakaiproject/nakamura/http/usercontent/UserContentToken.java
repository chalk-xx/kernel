package org.sakaiproject.nakamura.http.usercontent;
 
final class UserContentToken {

  private String userId;

  public UserContentToken(String userId) {
    this.userId = userId;
  }
  
  public String getUserId() {
    return userId;
  }

}
