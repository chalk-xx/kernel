package org.sakaiproject.nakamura.casauth;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasAuthentication implements AuthenticationPlugin {
  private SlingRepository repository;

  private static final Logger LOGGER = LoggerFactory.getLogger(CasAuthentication.class);

  private Principal principal;

  public CasAuthentication(Principal principal, SlingRepository repository) {
    this.principal = principal;
    this.repository = repository;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    final String principalName = principal.getName();
    if (credentials instanceof SimpleCredentials) {

      Session session = null;
      try {
        session = repository.loginAdministrative(null); // usage checked and ok KERN-577

        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable == null) {
          // create user
          LOGGER.debug("Createing user {}", principalName);
          userManager.createUser(principalName, RandomStringUtils.random(32),
              new Principal() {
                public String getName() {
                  return principalName;
                }
              }, PathUtils
                  .getUserPrefix(principalName, UserConstants.DEFAULT_HASH_LEVELS));
        }
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        throw (e);
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    } else {
      throw new RepositoryException("Can't authenticate credentials of type: "
          + credentials.getClass());
    }
    return ((SimpleCredentials) credentials).getUserID().equals(principalName);
  }
}
