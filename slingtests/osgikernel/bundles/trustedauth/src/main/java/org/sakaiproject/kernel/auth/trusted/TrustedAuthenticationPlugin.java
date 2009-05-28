package org.sakaiproject.kernel.auth.trusted;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

public class TrustedAuthenticationPlugin implements AuthenticationPlugin {
  private final Principal principal;

  public TrustedAuthenticationPlugin(Principal principal) {
    this.principal = principal;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      if (principal.getName().equals(sc.getUserID())) {
        auth = true;
      }
    }
    return auth;
  }
}
