package org.sakaiproject.nakamura.casauth;

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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

@Component(immediate = true)
@Service
public class CasLoginModulePlugin implements LoginModulePlugin {

  @Reference
  private SlingRepository repository;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials credentials) {
    boolean result = (credentials instanceof SimpleCredentials);
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal,
      Credentials credentials)
      throws RepositoryException {
    AuthenticationPlugin plugin = null;
    if (canHandle(credentials)) {
      plugin = new CasAuthentication(principal, repository);
    }
    return plugin;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getPrincipal(javax.jcr.Credentials)
   */
  public Principal getPrincipal(Credentials credentials) {
    CasPrincipal user = null;
    if (credentials != null && credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      user = new CasPrincipal(sc.getUserID());
    }
    return user;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal arg0, Credentials arg1) throws RepositoryException,
      FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#addPrincipals(java.util.Set)
   */
  @SuppressWarnings("unchecked")
  public void addPrincipals(Set arg0) {
    // Nothing to do
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("unchecked")
  public void doInit(CallbackHandler arg0, Session arg1, Map arg2) throws LoginException {
    // Nothing to do
  }
}
