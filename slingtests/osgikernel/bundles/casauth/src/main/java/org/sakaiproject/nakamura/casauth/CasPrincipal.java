package org.sakaiproject.nakamura.casauth;

import java.security.Principal;

public class CasPrincipal implements Principal {

  private String name;

  public CasPrincipal(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
