package org.sakaiproject.kernel.casauth;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

public class CasAuthentication implements AuthenticationPlugin {

  private Principal principal;

  public CasAuthentication(Principal principal) {
    this.principal = principal;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    if (credentials instanceof SimpleCredentials) {
      return ((SimpleCredentials) credentials).getUserID().equals(principal.getName());
    } else {
      throw new RepositoryException("Can't authenticate credentials of type: "
          + credentials.getClass());
    }
  }
}
