package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.User;

import javax.jcr.RepositoryException;

public class UserKey extends AuthorizableKey {

  private User user;

  public UserKey(User user) throws RepositoryException {
    super(user);
    this.user = user;
  }

  public User getUser() {
    return user;
  }
  
}
