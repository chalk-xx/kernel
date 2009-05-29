package org.sakaiproject.kernel.auth.trusted;

import org.sakaiproject.kernel.auth.trusted.TrustedAuthenticationServlet.TrustedUser;

import java.security.Principal;

public class TrustedPrincipal implements Principal {
  private final TrustedUser user;

  public TrustedPrincipal(TrustedUser user) {
    this.user = user;
  }

  public String getName() {
    return user.getUser();
  }
}
