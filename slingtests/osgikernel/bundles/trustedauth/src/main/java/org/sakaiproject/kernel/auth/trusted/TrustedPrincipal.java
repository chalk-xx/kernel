package org.sakaiproject.kernel.auth.trusted;

import org.sakaiproject.kernel.auth.trusted.TrustedTokenServiceImpl.TrustedUser;

import java.security.Principal;

public final class TrustedPrincipal implements Principal {
  private final TrustedUser user;

  public TrustedPrincipal(TrustedUser user) {
    this.user = user;
  }

  public String getName() {
    return user.getUser();
  }
}
