package org.sakaiproject.nakamura.http.usercontent;

import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenServiceWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class UserContentAuthenticationTokenServiceWrapper extends TrustedTokenServiceWrapper {

  public static final String TYPE = "U";

  UserContentAuthenticationTokenServiceWrapper(
      UserContentAuthenticationHandler userContentAuthenticationHandler,
      TrustedTokenService delegate) {
    super(validate(userContentAuthenticationHandler,delegate));
  }

  private static TrustedTokenService validate(
      UserContentAuthenticationHandler userContentAuthenticationHandler,
      TrustedTokenService delegate) {
    if ( !UserContentAuthenticationHandler.class.equals(userContentAuthenticationHandler.getClass()) ) {
      throw new IllegalArgumentException("Invalid use of UserContentAuthenticationTokenServiceWrapper");
    }
    return delegate;
  }

  public void addToken(HttpServletRequest request, HttpServletResponse response) {
    injectToken(request, response);
  }

  @Override
  public String getType() {
    return TYPE;
  }

}
