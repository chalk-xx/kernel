package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.Authorizable;

import javax.jcr.RepositoryException;

public class AuthorizableKey {

  private String id;
  private Authorizable authorizable;

  public AuthorizableKey(Authorizable authorizable) throws RepositoryException {
    this.id = authorizable.getID();
    this.authorizable = authorizable;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof AuthorizableKey)) 
      return false;
    return ((AuthorizableKey)obj).getID().equals(getID());
  }

  private String getID() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public Authorizable getAuthorizable() {
    return authorizable;
  }
}
