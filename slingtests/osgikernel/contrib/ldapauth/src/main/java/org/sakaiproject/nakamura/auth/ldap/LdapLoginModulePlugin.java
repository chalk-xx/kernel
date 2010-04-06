package org.sakaiproject.nakamura.auth.ldap;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

/**
 *
 * @author chall
 */
public class LdapLoginModulePlugin implements LoginModulePlugin {
  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#addPrincipals(java.util.Set)
   */
  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set arg0) {
    // Nothing to do
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials cred) {
    boolean canHandle = ldapAuthenticationPlugin.canHandle(cred);
    return canHandle;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options) {
    // nothing to do
    return;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    return ldapAuthenticationPlugin;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getPrincipal(javax.jcr.Credentials)
   */
  public Principal getPrincipal(Credentials credentials) {
    Principal principal = null;
    if (credentials != null && credentials instanceof SimpleCredentials) {
      final SimpleCredentials sc = (SimpleCredentials) credentials;

      principal = new Principal() {
        public String getName() {
          return sc.getUserID();
        }
      };

    }
    return principal;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal principal, Credentials credentials) throws RepositoryException,
      FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }
}
