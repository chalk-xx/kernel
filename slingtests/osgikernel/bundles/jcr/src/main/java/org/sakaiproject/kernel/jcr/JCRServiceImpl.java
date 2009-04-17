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
package org.sakaiproject.kernel.jcr;

import org.sakaiproject.kernel.api.jcr.JCRService;
import org.sakaiproject.kernel.api.locking.Lock;
import org.sakaiproject.kernel.api.locking.LockManager;
import org.sakaiproject.kernel.api.locking.LockTimeoutException;
import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.api.session.SessionManagerService;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;

/**
 * A lock manager that uses a cluster replicated cache to manage the locks
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.property name="service.description" value="The JCR Service"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.jcr.JCRService"
 * @scr.reference name="lockManager"
 *                interface="org.sakaiproject.kernel.api.locking.LockManager"
 *                bind="bindLockManager" unbind="unbindLockManager"
 * @scr.reference name="repository" interface="javax.jcr.Repository" bind="bindRepository"
 *                unbind="unbindRepository"
 * @scr.reference name="cacheManagerService"
 *                interface="org.sakaiproject.kernel.api.memory.CacheManagerService"
 *                bind="bindCacheManagerService" unbind="unbindCacheManagerService"
 * @scr.reference name="sessionManagerService"
 *                interface="org.sakaiproject.kernel.api.session.SessionManagerService"
 *                bind="bindSessionManagerService" unbind="unbindSessionManagerService"
 */

public class JCRServiceImpl implements JCRService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JCRServiceImpl.class);

  public static final String DEFAULT_WORKSPACE = "sakai";

  private static final String JCR_REQUEST_CACHE = "jcr.rc";

  private static final String JCR_SESSION_HOLDER = "sh";

  private static final boolean debug = LOGGER.isDebugEnabled();

  /**
   * The injected 170 repository
   */
  private Repository repository = null;

  // private Credentials repositoryCredentialsX;

  private boolean requestScope = true;

  /**
   *
   */
  private CacheManagerService cacheManager;

  /**
   * This is the sakai lock manager that does not hit the database.
   */
  private LockManager lockManager;

  /**
   *
   */
  private SessionManagerService sessionManagerService;

  /**
   * @throws RepositoryException
   * 
   */
  public JCRServiceImpl() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#getSession()
   */
  public Session getSession() throws LoginException, RepositoryException {
    return login();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#save()
   */
  public void save() throws RepositoryException {
    if (hasActiveSession()) {
      getSession().save();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#login()
   */
  public Session login() throws LoginException, RepositoryException {
    Session session = null;
    SessionHolder sh = getSessionHolder();
    if (sh == null) {
      long t1 = System.currentTimeMillis();
      sh = new SessionHolder(sessionManagerService.getCurrentSession());
      setSesssionHolder(sh);
      if (debug) {
        LOGGER.debug("Session Start took " + (System.currentTimeMillis() - t1) + "ms");
      }
    }
    session = sh.getSession();
    return session;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#loginSystem()
   */
  public Session loginSystem() throws LoginException, RepositoryException {
    Session session = null;
    SessionHolder sh = getSessionHolder();
    if (sh == null) {
      long t1 = System.currentTimeMillis();

      sh = new SessionHolder(repository, new SakaiJCRCredentials(), DEFAULT_WORKSPACE);
      setSesssionHolder(sh);
      if (debug) {
        LOGGER.debug("Session Start took " + (System.currentTimeMillis() - t1) + "ms");
      }
    }
    session = sh.getSession();
    return session;
  }

  /**
   * @return
   */
  private Cache<SessionHolder> getRequestCache() {
    if (requestScope) {
      return cacheManager.getCache(JCR_REQUEST_CACHE, CacheScope.REQUEST);
    } else {
      return cacheManager.getCache(JCR_REQUEST_CACHE, CacheScope.THREAD);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#logout()
   */
  public void logout() throws LoginException, RepositoryException {
    clearSessionHolder();
  }

  /**
   * @param jcrSessionHolder
   * @return
   */
  private SessionHolder getSessionHolder() {
    return (SessionHolder) getRequestCache().get(JCR_SESSION_HOLDER);
  }

  /**
   * @param sh
   */
  private void setSesssionHolder(SessionHolder sh) {
    getRequestCache().put(JCR_SESSION_HOLDER, sh);
  }

  /**
   *
   */
  private void clearSessionHolder() {
    Cache<SessionHolder> cache = getRequestCache();
    SessionHolder sh = cache.get(JCR_SESSION_HOLDER);
    if (sh != null) {
      lockManager.clearLocks();
      cache.remove(JCR_SESSION_HOLDER);
    }
  }

  public void clearLocks() {
    Cache<SessionHolder> cache = getRequestCache();
    SessionHolder sh = cache.get(JCR_SESSION_HOLDER);
    if (sh != null) {
      lockManager.clearLocks();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#getRepository()
   */
  public Repository getRepository() {
    return repository;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#setCurrentSession(javax.jcr. Session)
   */
  public Session setSession(Session session) {
    Session currentSession = null;
    SessionHolder sh = getSessionHolder();
    if (sh != null) {
      currentSession = sh.getSession();
      sh.keepLoggedIn();
    }
    if (debug) {
      LOGGER.debug("Replacing " + currentSession + " with " + session);
    }
    if (session == null) {
      clearSessionHolder();
    } else {
      sh = new SessionHolder(session);
      setSesssionHolder(sh);
    }
    return currentSession;
  }

  public boolean needsMixin(Node node, String mixin) throws RepositoryException {
    return true;
    // ! node.getSession().getWorkspace().getNodeTypeManager().getNodeType(node.
    // getPrimaryNodeType().getName()).isNodeType(mixin);
  }

  public boolean hasActiveSession() {
    Session session = null;
    SessionHolder sh = getSessionHolder();
    if (sh != null) {
      session = sh.getSession();
    }
    return (session != null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#getDefaultWorkspace()
   */
  public String getDefaultWorkspace() {
    return "";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#getObservationManager()
   */
  public ObservationManager getObservationManager() {
    SakaiJCRCredentials ssp = new SakaiJCRCredentials();
    Session s = null;
    ObservationManager observationManager = null;
    try {
      s = getRepository().login(ssp);
      observationManager = s.getWorkspace().getObservationManager();
    } catch (RepositoryException e) {
      LOGGER.error("Failed to get ObservationManager from workspace");
      e.printStackTrace();
    } finally {
      try {
        s.logout();
      } catch (Exception ex) {
      }
      ;
    }
    return observationManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.jcr.JCRService#getQueryManager()
   */
  public QueryManager getQueryManager() throws RepositoryException {
    QueryManager queryManager = getSession().getWorkspace().getQueryManager();
    return queryManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * @throws LockTimeoutException
   * @see org.sakaiproject.kernel.api.jcr.JCRService#lock(javax.jcr.Node)
   */
  public Lock lock(Node node) throws RepositoryException, LockTimeoutException {
    Node lockable = node;
    while (lockable.isNew()) {
      lockable = lockable.getParent();
    }
    String lockId = null;
    try {
      lockId = StringUtils.sha1Hash(lockable.getPath());
    } catch (Exception e) {
      throw new RepositoryException("Failed to locate SHA1 Hash algoritm ", e);
    }
    Lock lock = lockManager.waitForLock(lockId);
    return lock;
  }

  public void bindLockManager(LockManager lockManager) {
    this.lockManager = lockManager;
  }

  public void unbindLockManager(LockManager lockManager) {
    this.lockManager = null;
  }

  public void bindRepository(Repository repository) {
    this.repository = repository;
  }

  public void unbindRepository(Repository repository) {
    this.repository = null;
  }

  public void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManager = cacheManagerService;
  }

  public void unbindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManager = null;
  }

  public void bindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = sessionManagerService;
  }

  public void unbindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = null;
  }
}
