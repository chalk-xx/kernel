/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AnonCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AuthenticationPluginWrapper;
import org.apache.sling.jcr.jackrabbit.server.impl.security.CallbackHandlerWrapper;
import org.apache.sling.jcr.jackrabbit.server.impl.security.TrustedCredentials;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public class SparseLoginModule extends AbstractLoginModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(SparseLoginModule.class);
  private Authenticator authenticator;
  private User user;

  @Override
  protected void doInit(CallbackHandler callbackHandler, Session session,
      @SuppressWarnings("rawtypes") Map options) throws LoginException {
    try {
      SessionImpl sessionImpl = (SessionImpl) session;
      SparseMapUserManager userManager = (SparseMapUserManager) sessionImpl
          .getUserManager();
      org.sakaiproject.nakamura.api.lite.Session sparseSession = userManager.getSession();

      LoginModulePlugin[] modules = Activator.getLoginModules();
      for (int i = 0; i < modules.length; i++) {
        modules[i].doInit(callbackHandler, session, options);
      }

      CredentialsCallback cb = new CredentialsCallback();
      try {
        callbackHandler.handle(new Callback[] { cb });
      } catch (IOException e1) {
        LOGGER.warn(e1.getMessage(), e1);
      } catch (UnsupportedCallbackException e1) {
        LOGGER.warn(e1.getMessage(), e1);
      }
      authenticator = sparseSession.getAuthenticator();
    } catch (StorageClientException e) {
      throw new LoginException(e.getMessage());
    } catch (AccessDeniedException e) {
      throw new LoginException(e.getMessage());
    } catch (RepositoryException e) {
      throw new LoginException(e.getMessage());
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#initialize(javax.security.auth.Subject,
   *      javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
   */
  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler,
      Map<String, ?> sharedState, Map<String, ?> options) {
    CallbackHandlerWrapper wrappedCallbackHandler = new CallbackHandlerWrapper(subject,
        callbackHandler);

    super.initialize(subject, wrappedCallbackHandler, sharedState, options);
  }

  @Override
  protected boolean impersonate(Principal principal, Credentials credentials)
      throws RepositoryException, LoginException {
    if (credentials instanceof AdministrativeCredentials) {
      return true;
    }
    if (credentials instanceof AnonCredentials) {
      return false;
    }

    LoginModulePlugin[] modules = Activator.getLoginModules();
    for (int i = 0; i < modules.length; i++) {
      if (modules[i].canHandle(credentials)) {
        int result = modules[i].impersonate(principal, credentials);
        if (result != LoginModulePlugin.IMPERSONATION_DEFAULT) {
          return result == LoginModulePlugin.IMPERSONATION_SUCCESS;
        }
      }
    }

    User user = authenticator.systemAuthenticate(principal.getName());
    if (user != null) {
      Subject impersSubject = getImpersonatorSubject(credentials);

      if (!impersSubject.getPrincipals(AdminPrincipal.class).isEmpty()
          || !impersSubject.getPrincipals(SystemPrincipal.class).isEmpty()) {
        return true;
      }

      String impersonators = (String) user
          .getProperty(User.IMPERSONATORS_FIELD);
      if (impersonators != null) {
        Set<String> imp = new HashSet<String>();
        Collections.addAll(imp, StringUtils.split(impersonators, ';'));
        for (Principal p : subject.getPrincipals()) {
          if (imp.contains(p.getName())) {
            return true;
          }
        }
      }
      throw new FailedLoginException("attempt by user " + principal.getName()
          + " with subjects " + impersSubject.getPrincipals() + " to impersonate "
          + credentials);
    }
    return false;
  }

  @Override
  protected Authentication getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    if (creds instanceof TrustedCredentials) {
      return new Authentication() {

        public boolean canHandle(Credentials credentials) {
          return (credentials instanceof AdministrativeCredentials)
              || (credentials instanceof AnonCredentials);
        }

        public boolean authenticate(Credentials credentials) throws RepositoryException {
          return (credentials instanceof AdministrativeCredentials)
              || (credentials instanceof AnonCredentials);
        }
      };
    }

    LoginModulePlugin[] modules = Activator.getLoginModules();
    for (int i = 0; i < modules.length; i++) {
      if (modules[i].canHandle(creds)) {
        AuthenticationPlugin pa = modules[i].getAuthentication(principal, creds);
        if (pa != null) {
          return new AuthenticationPluginWrapper(pa, modules[i]);
        }
      }
    }

    if (user != null) {
      Authentication authentication = new SparseCredentialsAuthentication(user,
          authenticator);
      if (authentication.canHandle(creds)) {
        return authentication;
      }
    } else {
      LOGGER.debug("User is null, no login being performed ");
    }
    return null;
  }

  @Override
  protected Principal getPrincipal(Credentials credentials) {
    if (credentials instanceof TrustedCredentials) {
      return ((TrustedCredentials) credentials).getPrincipal();
    }
    LoginModulePlugin[] modules = Activator.getLoginModules();
    for (int i = 0; i < modules.length; i++) {
      if (modules[i].canHandle(credentials)) {
        Principal p = modules[i].getPrincipal(credentials);
        if (p != null) {
          return p;
        }
      }
    }
    String userId = getUserID(credentials);
    LOGGER.debug("Got User ID as [{}]", userId);
    User auser = authenticator.systemAuthenticate(userId);
    if (auser != null) {
      this.user = auser;
      if (User.ADMIN_USER.equals(userId)) {
        return new AdminPrincipal(userId);
      } else if (User.SYSTEM_USER.equals(userId)) {
        return new SystemPrincipal();
      } else if (User.ANON_USER.equals(userId)) {
        return new AnonymousPrincipal();
      }
      LOGGER.debug("Sparse User Principal {}", userId);
      return new SparsePrincipal(userId, this.getClass().getName() + " credentials were "
          + ((credentials == null) ? null : credentials.getClass().getName()),
          SparseMapUserManager.USERS_PATH);
    } else {
      LOGGER.debug("No User Found in credentials  UserID[{}] Credentials[{}]", userId,
          credentials);
    }
    return null;
  }

  /**
   * Since the AbstractLoginModule getCredentials does not know anything about
   * TrustedCredentials we have to re-try here.
   */
  @Override
  protected Credentials getCredentials() {
    Credentials creds = super.getCredentials();
    if (creds == null) {
      CredentialsCallback callback = new CredentialsCallback();
      try {
        callbackHandler.handle(new Callback[] { callback });
        Credentials callbackCreds = callback.getCredentials();
        if (callbackCreds instanceof TrustedCredentials) {
          creds = callbackCreds;
        }
      } catch (UnsupportedCallbackException e) {
        LOGGER.warn("Credentials-Callback not supported try Name-Callback");
      } catch (IOException e) {
        LOGGER.error("Credentials-Callback failed: " + e.getMessage()
            + ": try Name-Callback");
      }
    }
    return creds;
  }

  /**
   * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#getPrincipals
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  protected Set getPrincipals() {
    Set principals = super.getPrincipals();
    LoginModulePlugin[] modules = Activator.getLoginModules();
    for (int i = 0; i < modules.length; i++) {
      modules[i].addPrincipals(principals);
    }
    return principals;
  }

  private static final String KEY_LOGIN_NAME = "javax.security.auth.login.name";

  protected String getUserID(Credentials credentials) {
    String userId = null;
    if (credentials != null) {
      if (credentials instanceof GuestCredentials) {
        userId = anonymousId;
      } else if (credentials instanceof SimpleCredentials) {
        userId = ((SimpleCredentials) credentials).getUserID();
      } else {
        try {
          NameCallback callback = new NameCallback("User-ID: ");
          callbackHandler.handle(new Callback[] { callback });
          userId = callback.getName();
        } catch (UnsupportedCallbackException e) {
          LOGGER.warn("Credentials- or NameCallback must be supported");
        } catch (IOException e) {
          LOGGER.error("Name-Callback failed: " + e.getMessage());
        }
      }
    }
    if (userId == null && sharedState.containsKey(KEY_LOGIN_NAME)) {
      userId = (String) sharedState.get(KEY_LOGIN_NAME);
    }

    // still no userId -> anonymousID if its has been defined.
    // TODO: check again if correct when used with 'extendedAuth'
    if (userId == null) {
      userId = anonymousId;
    }
    return userId;
  }
}
