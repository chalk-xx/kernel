package org.sakaiproject.kernel.auth.trusted;

import java.security.Principal;

import javax.jcr.SimpleCredentials;

public class TrustedPrincipal implements Principal {
  private final SimpleCredentials sc;

  public TrustedPrincipal(SimpleCredentials sc) {
    this.sc = sc;
  }

  public String getName() {
    return sc.getUserID();
  }
}
