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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.sling.jcr.jackrabbit.server.impl.security.standard.EntryCollectorImpl;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

/**
 * This EntryCollector implementation uses a principal manager to check each
 * potential principal against a dynamic principal manager to see if the ACE
 * should be included in the resolved entry.
 */
public class DynamicEntryCollector extends EntryCollectorImpl {

  private static final Logger LOG = LoggerFactory.getLogger(DynamicEntryCollector.class);
  private DynamicPrincipalManager dynamicPrincipalManager;
  private LRUMap staticPrincipals = new LRUMap(1000);

  /**
   * Construct this type of EntryCollector with a principal manager.
   * 
   * @param systemSession
   * @throws RepositoryException
   * @throws UnsupportedRepositoryOperationException
   */
  public DynamicEntryCollector(DynamicPrincipalManager dynamicPrincipalManager)
      throws UnsupportedRepositoryOperationException, RepositoryException {
    this.dynamicPrincipalManager = dynamicPrincipalManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.standard.EntryCollectorImpl#hasPrincipal(java.lang.String,
   *      org.apache.jackrabbit.core.NodeImpl, java.util.Map)
   */
  @Override
  protected boolean hasPrincipal(String principalName, NodeImpl aclNode,
      Map<String, List<AccessControlEntry>> princToEntries) {
    if (super.hasPrincipal(principalName, aclNode, princToEntries)) {
      return true;
    }
    /*
     * Principals that don't have a 'dynamic=true' property will not be resolved
     * dynamically. We cache principals that are found not to be dynamic. The
     * cache is never invalidated because it is assumed that principals will not
     * be included in ACLs until their dynamic/static status has been set, and
     * that setting will not be modified subsequently.
     */
    if (staticPrincipals.containsKey(principalName)) {
      LOG.debug("Principal " + principalName + " is cached static - not resolving dynamically");
      return false;
    }
    Session session = aclNode.getSession();
    if (session instanceof JackrabbitSession) {
      JackrabbitSession jcrSession = (JackrabbitSession) session;
      try {
        boolean dynamic = false;
        UserManager manager = jcrSession.getUserManager();
        Authorizable principal = manager.getAuthorizable(principalName);
        if (principal.hasProperty("dynamic")) {
          Value[] dyn = principal.getProperty("dynamic");
          if (dyn != null && dyn.length > 0 && ("true".equals(dyn[0].getString()))) {
            LOG.debug("Found dynamic principal " + principalName);
            dynamic = true;
          }
        }
        if (!dynamic) {
          LOG.debug("Found static principal " + principalName + ". Caching");
          staticPrincipals.put(principalName, true);
          return false;
        }
      } catch (AccessDeniedException e) {
        LOG.error("Unable to determine group status", e);
      } catch (UnsupportedRepositoryOperationException e) {
        LOG.error("Unable to access user manager", e);
      } catch (RepositoryException e) {
        LOG.error("Unable to access user manager", e);
      }
    }
    return dynamicPrincipalManager.hasPrincipalInContext(principalName, aclNode);
  }

}
