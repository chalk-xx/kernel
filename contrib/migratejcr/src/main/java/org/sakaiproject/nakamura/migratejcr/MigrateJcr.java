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
package org.sakaiproject.nakamura.migratejcr;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
/**
 *
 */
@Component
public class MigrateJcr {
  private Logger LOGGER = LoggerFactory.getLogger(MigrateJcr.class);
  
  @Reference
  private SlingRepository slingRepository;
  
  @Reference
  private Repository sparseRepository;
  
  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext componentContext) {
    Dictionary componentProps = componentContext.getProperties();
    if (shouldMigrate(componentProps)) {
      try {
        migrateAuthorizables();
        migrateContentPool();
        cleanup();
      } catch (Exception e) {
        LOGGER.error("Failed data migration from JCR to Sparse.", e);
      }
    }
  }

  private void cleanup() {
    // TODO Auto-generated method stub
    
  }

  @SuppressWarnings("deprecation")
  private void migrateContentPool() throws InvalidQueryException, RepositoryException {
    LOGGER.info("beginning users and groups migration.");
    String contentPoolQuery = "//element(*, sakai:pooled-content) order by @jcr:score descending";
    javax.jcr.Session jcrSession = null;
    try {
      jcrSession = slingRepository.loginAdministrative(null);
      QueryManager qm = jcrSession.getWorkspace().getQueryManager();
      Query q = qm.createQuery(contentPoolQuery, Query.XPATH);
      QueryResult result = q.execute();
      NodeIterator resultNodes = result.getNodes();
      String nodeWord = resultNodes.getSize() == 1 ? "node" : "nodes";
      LOGGER.info("Found {} pooled content {} in Jackrabbit.", resultNodes.getSize(), nodeWord);
      while(resultNodes.hasNext()) {
        Node contentNode = resultNodes.nextNode();
        LOGGER.info(contentNode.getPath());
        movePooledContentToSparse(contentNode);
      }
    } finally {
      if (jcrSession != null) {
        jcrSession.logout();
      }
    }
    
  }

  private void movePooledContentToSparse(Node contentNode) {
    // TODO Auto-generated method stub
    
  }

  @SuppressWarnings("deprecation")
  private void migrateAuthorizables() throws RepositoryException, ClientPoolException, StorageClientException, AccessDeniedException {
    LOGGER.info("beginning users and groups migration.");
    javax.jcr.Session jcrSession = null;
    try {
      jcrSession = slingRepository.loginAdministrative(null);
      String usersQuery = "//*[@sling:resourceType='sakai/user-home'] order by @jcr:score descending";
      QueryManager qm = jcrSession.getWorkspace().getQueryManager();
      Query q = qm.createQuery(usersQuery, Query.XPATH);
      QueryResult result = q.execute();
      NodeIterator resultNodes = result.getNodes();
      String folderWord = resultNodes.getSize() == 1 ? "folder" : "folders";
      LOGGER.info("found {} user home {} in Jackrabbit.", resultNodes.getSize(), folderWord);
      while(resultNodes.hasNext()) {
        Node authHomeNode = resultNodes.nextNode();
        LOGGER.info(authHomeNode.getPath());
        moveAuthorizableToSparse(authHomeNode, AccessControlUtil.getUserManager(jcrSession));
      }
      
      String groupsQuery = "//*[@sling:resourceType='sakai/group-home'] order by @jcr:score descending";
      q = qm.createQuery(groupsQuery, Query.XPATH);
      result = q.execute();
      resultNodes = result.getNodes();
      folderWord = resultNodes.getSize() == 1 ? "folder" : "folders";
      LOGGER.info("found {} group home {} in Jackrabbit.", resultNodes.getSize(), folderWord);
      while(resultNodes.hasNext()) {
        Node groupHomeNode = resultNodes.nextNode();
        LOGGER.info(groupHomeNode.getPath());
        moveAuthorizableToSparse(groupHomeNode, AccessControlUtil.getUserManager(jcrSession));
      }
    } finally {
      if (jcrSession != null) {
        jcrSession.logout();
      }
    }
    
    
  }

  private void moveAuthorizableToSparse(Node authHomeNode, UserManager userManager) throws ClientPoolException, StorageClientException, AccessDeniedException, PathNotFoundException, RepositoryException {
    Session sparseSession = null;
    try {
      sparseSession = sparseRepository.loginAdministrative();
      AuthorizableManager authManager = sparseSession.getAuthorizableManager();
      boolean isUser = "sakai/user-home".equals(authHomeNode.getProperty("sling:resourceType").getString());
      Node profileNode = authHomeNode.getNode("public/authprofile");
      if (isUser) {
        String id = profileNode.getProperty("rep:userId").getString();
        String firstName = profileNode.getProperty("firstName").getString();
        String lastName = profileNode.getProperty("lastName").getString();
        String email = profileNode.getProperty("email").getString();
        authManager.createUser(id, id, null, ImmutableMap.of(
            "firstName", (Object)firstName,
            "lastName", (Object)lastName,
            "email", (Object)email));
        userManager.getAuthorizable(id).remove();
      }
    } finally {
      if (sparseSession != null) {
        sparseSession.logout();
      }
    }
    
    
    
  }

  @SuppressWarnings("unchecked")
  private boolean shouldMigrate(Dictionary componentProps) {
    // TODO determine whether there is any migrating to do
    return true;
  }

}
