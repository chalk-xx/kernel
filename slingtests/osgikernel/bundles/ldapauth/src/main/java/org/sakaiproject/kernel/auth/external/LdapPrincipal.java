package org.sakaiproject.kernel.auth.external;

import org.sakaiproject.kernel.auth.external.LdapAuthenticationServlet.LdapUser;

import java.security.Principal;

public class LdapPrincipal implements Principal {
  private final LdapUser user;

  public LdapPrincipal(LdapUser user) {
    this.user = user;
  }

  public String getName() {
    return user.getUser();
  }
}
