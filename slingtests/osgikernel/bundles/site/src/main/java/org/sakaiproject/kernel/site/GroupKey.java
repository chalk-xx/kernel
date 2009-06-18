package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.Group;

import javax.jcr.RepositoryException;

public class GroupKey extends AuthorizableKey {

  private Group group;
  
  public GroupKey(Group group) throws RepositoryException {
    super(group);
    this.group = group;
  }
  
  public Group getGroup() {
    return group;
  }
  
}
