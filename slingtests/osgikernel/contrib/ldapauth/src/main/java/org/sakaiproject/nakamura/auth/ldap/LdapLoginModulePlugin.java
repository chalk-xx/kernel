package org.sakaiproject.nakamura.auth.ldap;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

/**
 * The <code>LdapLoginModulePlugin</code> is a LoginModulePlugin which handles
 * <code>SimpleCredentials</code> attributed with the special authentication
 * data provided by the {@link org.sakaiproject.nakamura.auth.ldap.LdapAuthenticationHandler}.
 */
@Component(enabled = false)
@Service
public class LdapLoginModulePlugin implements LoginModulePlugin {
  @Reference
  private LdapAuthenticationPlugin authPlugin;

  public LdapLoginModulePlugin() {
  }

  LdapLoginModulePlugin(LdapAuthenticationPlugin authPlugin) {
    this.authPlugin = authPlugin;
  }
  
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
    boolean canHandle = false;
    if (cred instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) cred;
      if (sc.getUserID() != null && sc.getUserID().length() > 0
          && sc.getPassword() != null && sc.getPassword().length > 0) {
        canHandle = true;
      }
    }
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
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    return authPlugin;
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
