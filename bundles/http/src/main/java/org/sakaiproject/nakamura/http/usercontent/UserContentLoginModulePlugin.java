package org.sakaiproject.nakamura.http.usercontent;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

@Component(immediate = true, metatype = true)
@Service(value=LoginModulePlugin.class)
public class UserContentLoginModulePlugin implements LoginModulePlugin {

  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
    // remember even thought this is a service, the init will be called. do nothing here.
  }

  public boolean canHandle(Credentials credentials) {
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
      Object marker = simpleCredentials
          .getAttribute(UserContentAuthenticationHandler.TRANSFER_CREDENTIAL_MARKER);
      return (marker instanceof UserContentToken
          && marker.getClass().equals(UserContentToken.class) && ((UserContentToken) marker)
          .getUserId().equals(simpleCredentials.getUserID()));
    }
    return false;
  }

  public Principal getPrincipal(final Credentials credentials) {
    if (canHandle(credentials)) {
      return new Principal() {

        public String getName() {
          return ((SimpleCredentials) credentials).getUserID();
        }
      };
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set principals) {
  }

  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    return new AuthenticationPlugin() {
      public boolean authenticate(Credentials credentials) throws RepositoryException {
        return canHandle(credentials);
      }
    };
  }

  public int impersonate(Principal principal, Credentials credentials)
      throws RepositoryException, FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

}
