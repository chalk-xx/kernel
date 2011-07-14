package org.sakaiproject.nakamura.auth.trusted;

public class AuthenticatedAction {

  protected static final String REDIRECT = "r";
  private String action;

  public void setAction(String action) {
    this.action = action;
    
  }

  public boolean isRedirect() {
    return REDIRECT.equals(action);
  }


}
