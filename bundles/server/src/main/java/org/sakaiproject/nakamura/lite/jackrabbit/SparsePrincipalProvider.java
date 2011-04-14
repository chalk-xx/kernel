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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.ConcurrentLRUMap;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.util.PreemptiveIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class SparsePrincipalProvider implements PrincipalProvider {

  private EveryonePrincipal everyonePrincipal;
  /** Option name for the max size of the cache to use */
  public static final String MAXSIZE_KEY = "cacheMaxSize";
  /** Option name to enable negative cache entries (see JCR-2672) */
  public static final String NEGATIVE_ENTRY_KEY = "cacheIncludesNegative";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(SparsePrincipalProvider.class);

  /** flag indicating if the instance has not been {@link #close() closed} */
  private boolean initialized;

  /** the principal cache */
  private ConcurrentLRUMap<String, Principal> cache = new ConcurrentLRUMap<String, Principal>();
  private Repository sparseRepository;
  private org.sakaiproject.nakamura.api.lite.Session session;
  private AuthorizableManager authorizableManager;
  private AccessControlManager accesControlManager;

  /**
   * Creates a new DefaultPrincipalProvider reading the principals from the storage below
   * the given security root node.
   * 
   * @throws RepositoryException
   *           if an error accessing the repository occurs.
   */
  public SparsePrincipalProvider() throws RepositoryException {
    try {
      sparseRepository = SparseRepositoryHolder.getSparseRepositoryInstance();
      session = sparseRepository.loginAdministrative();
      authorizableManager = session.getAuthorizableManager();
      accesControlManager = session.getAccessControlManager();
      everyonePrincipal = EveryonePrincipal.getInstance();
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    }

  }

  public boolean canReadPrincipal(Session session, Principal principal) {
    try {
      Authorizable authorizable = authorizableManager.findAuthorizable(session
          .getUserID());
      if (authorizable != null) {
        return accesControlManager.can(authorizable, Security.ZONE_AUTHORIZABLES,
            principal.getName(), Permissions.CAN_READ);
      }
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return false;
  }

  public PrincipalIterator findPrincipals(String simpleFilter) {
    return findPrincipals(simpleFilter, PrincipalManager.SEARCH_TYPE_ALL);
  }

  @SuppressWarnings("unchecked")
  public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {

    checkInitialized();
    try {
      switch (searchType) {
      case PrincipalManager.SEARCH_TYPE_GROUP:
        return new SparsePrincipalIterator(authorizableManager.findAuthorizable(
            "rep:principalName", simpleFilter,
            org.sakaiproject.nakamura.api.lite.authorizable.Authorizable.class));
      case PrincipalManager.SEARCH_TYPE_NOT_GROUP:
        return new SparsePrincipalIterator(authorizableManager.findAuthorizable(
            "rep:principalName", simpleFilter,
            org.sakaiproject.nakamura.api.lite.authorizable.Authorizable.class));
      case PrincipalManager.SEARCH_TYPE_ALL:
        return new SparsePrincipalIterator(authorizableManager.findAuthorizable(
            "rep:principalName", simpleFilter,
            org.sakaiproject.nakamura.api.lite.authorizable.Authorizable.class));
      default:
        throw new IllegalArgumentException("Invalid searchType");
      }
    } catch (StorageClientException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    return PrincipalIteratorAdapter.EMPTY;
  }

  public PrincipalIterator getGroupMembership(final Principal principal) {
    final List<String> memberIds = new ArrayList<String>();
    try {
      org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
          .findAuthorizable(principal.getName());
      if (a == null) {
        return PrincipalIteratorAdapter.EMPTY;
      }
      Collections.addAll(memberIds, a.getPrincipals());
    } catch (AccessDeniedException e) {
      LOGGER.debug(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    if (everyonePrincipal.isMember(principal)
        && !memberIds.contains(everyonePrincipal.getName())) {
      memberIds.add(everyonePrincipal.getName());
      addToCache(principal);
    }

    return new PrincipalIteratorAdapter(new PreemptiveIterator<Principal>() {

      private int p = 0;
      private Principal prin = null;

      protected boolean internalHasNext() {
        while (p < memberIds.size()) {
          String id = memberIds.get(p);
          p++;
          try {
            if (everyonePrincipal.getName().equals(id)) {
              prin = everyonePrincipal;
              return true;
            } else {
              org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
                  .findAuthorizable(id);
              if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
                for (String pid : a.getPrincipals()) {
                  if (!memberIds.contains(pid)) {
                    memberIds.add(pid);
                  }
                }
                if (cache.containsKey(id)) {
                  prin = cache.get(id);
                } else {
                  prin = new SparsePrincipal(a, this.getClass().getName());
                  addToCache(prin);
                }
                return true;
              } else if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.User) {
                if (cache.containsKey(id)) {
                  prin = cache.get(id);
                } else {
                  prin = new SparsePrincipal(a, this.getClass().getName());
                  addToCache(prin);
                }
                return true;
              }
            }
          } catch (AccessDeniedException e) {
            LOGGER.info(e.getMessage(), e);
          } catch (StorageClientException e) {
            LOGGER.info(e.getMessage(), e);
          }
        }
        return false;
      }

      protected Principal internalNext() {
        return prin;
      }

    });
  }

  /**
   * @see PrincipalProvider#getPrincipals(int)
   * @param searchType
   *          Any of the following search types:
   *          <ul>
   *          <li>{@link PrincipalManager#SEARCH_TYPE_GROUP}</li>
   *          <li>{@link PrincipalManager#SEARCH_TYPE_NOT_GROUP}</li>
   *          <li>{@link PrincipalManager#SEARCH_TYPE_ALL}</li>
   *          </ul>
   * @see PrincipalProvider#getPrincipals(int)
   */
  public PrincipalIterator getPrincipals(int searchType) {
    return findPrincipals(null, searchType);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This implementation uses the user and node resolver to find the appropriate nodes.
   * 
   * @throws RepositoryException
   */
  protected Principal providePrincipal(String principalName) throws RepositoryException {
    // check for 'everyone'
    if (everyonePrincipal.getName().equals(principalName)) {
      return everyonePrincipal;
    }
    if (User.ADMIN_USER.equals(principalName)) {
      return new AdminPrincipal(User.ADMIN_USER);
    }
    if (User.ANON_USER.equals(principalName)) {
      return new AnonymousPrincipal();
    }
    org.sakaiproject.nakamura.api.lite.authorizable.Authorizable ath;
    try {
      ath = authorizableManager.findAuthorizable(principalName);
      if (ath != null) {
        return new SparsePrincipal(ath, this.getClass().getName());
      }
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
    return null;
  }

  public void close() {
    try {
      session.logout();
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * {@link #providePrincipal(String)} is called, if no matching entry is present in the
   * cache.<br>
   * NOTE: If the cache is enabled to contain negative entries (see
   * {@link #NEGATIVE_ENTRY_KEY} configuration option), the cache will also store negative
   * matches (as <code>null</code> values) in the principal cache.
   */
  public Principal getPrincipal(String principalName) {
    checkInitialized();
    if (cache.containsKey(principalName)) {
      return (Principal) cache.get(principalName);
    }
    Principal principal = null;
    try {
      principal = providePrincipal(principalName);
    } catch (RepositoryException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    if (principal != null) {
      cache.put(principalName, principal);
    }
    return principal;
  }

  /**
   * Check if the instance has been closed {@link #close()}.
   * 
   * @throws IllegalStateException
   *           if this instance was closed.
   */
  protected void checkInitialized() {
    if (!initialized) {
      throw new IllegalStateException("Not initialized.");
    }
  }

  /**
   * Clear the principal cache.
   */
  protected void clearCache() {
    cache.clear();
  }

  /**
   * Add an entry to the principal cache.
   * 
   * @param principal
   *          to be cached.
   */
  protected void addToCache(Principal principal) {

  }

  /**
   * @see PrincipalProvider#init(java.util.Properties)
   */
  public synchronized void init(Properties options) {
    if (initialized) {
      throw new IllegalStateException("already initialized");
    }

    int maxSize = Integer.parseInt(options.getProperty(MAXSIZE_KEY, "1000"));
    cache = new ConcurrentLRUMap<String, Principal>(maxSize);

    initialized = true;
  }

}
