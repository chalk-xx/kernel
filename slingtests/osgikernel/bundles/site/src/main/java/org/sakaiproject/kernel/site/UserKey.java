package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.User;
import org.sakaiproject.kernel.api.site.SortField;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class UserKey extends AuthorizableKey {

  private User user;

  public UserKey(User user) throws RepositoryException {
    super(user);
    this.user = user;
  }

  public UserKey(User user, Node profileNode) throws RepositoryException {
    super(user);
    this.user = user;
    if (profileNode.hasProperty(SortField.firstName.toString())) {
      setFirstName(profileNode.getProperty(SortField.firstName.toString()).getString());
    }
    if (profileNode.hasProperty(SortField.lastName.toString())) {
      setLastName(profileNode.getProperty(SortField.lastName.toString()).getString());
    }
  }

  public User getUser() {
    return user;
  }
}
