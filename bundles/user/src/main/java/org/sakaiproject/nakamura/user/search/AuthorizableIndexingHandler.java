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
package org.sakaiproject.nakamura.user.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.TopicIndexer;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 */
@Component
public class AuthorizableIndexingHandler implements IndexingHandler {
  // TODO should this be "authorizables" or "user","groups"?
  private static final String[] DEFAULT_TOPICS = {
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.ADDED_TOPIC,
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.DELETE_TOPIC,
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.UPDATED_TOPIC };

  // list of properties to be indexed
  private static final Set<String> WHITELISTED_PROPS = ImmutableSet.of("name",
      "firstName", "lastName", "email", "type");

  // list of authorizables to not index
  private static final Set<String> BLACKLISTED_AUTHZ = ImmutableSet.of("admin",
      "g-contacts-admin", "anonymous", "owner");

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Reference
  private TopicIndexer topicIndexer;

  // ---------- SCR integration ------------------------------------------------
  @Activate
  protected void activate(Map<?, ?> props) {
    for (String topic : DEFAULT_TOPICS) {
      topicIndexer.addHandler(topic, this);
    }
  }

  @Deactivate
  protected void deactivate(Map<?, ?> props) {
    for (String topic : DEFAULT_TOPICS) {
      topicIndexer.removeHandler(topic, this);
    }
  }

  // ---------- IndexingHandler interface --------------------------------------
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDocuments(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<SolrInputDocument> getDocuments(RepositorySession repositorySession,
      Event event) {
    logger.debug("GetDocuments for {} ", event);
    // get the name of the authorizable (user,group)
    String name = (String) event.getProperty("path");

    // stop processing if the user isn't to be indexed
    if (BLACKLISTED_AUTHZ.contains(name)) {
      return Collections.emptyList();
    }

    List<SolrInputDocument> documents = Lists.newArrayList();
    if (!StringUtils.isBlank(name)) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        AuthorizableManager authzMgr = session.getAuthorizableManager();
        Authorizable authorizable = authzMgr.findAuthorizable(name);
        if (authorizable != null) {
          SolrInputDocument doc = new SolrInputDocument();

          Map<String, Object> properties = authorizable.getSafeProperties();

          for (Entry<String, Object> p : properties.entrySet()) {
            if (WHITELISTED_PROPS.contains(p.getKey())) {
              doc.addField(p.getKey(), p.getValue());
            }
          }

          String path = (String) properties.get("path");

          // TODO should the path or username be used here?
          for (String principal : getReadingPrincipals(session, name)) {
            doc.addField("readers", principal);
          }

          String id = path;
          if (Authorizable.isAGroup(properties)) {
            id = name;
          }
          doc.addField("id", id);
          documents.add(doc);
        }
      } catch (ClientPoolException e) {
        logger.warn(e.getMessage(), e);
      } catch (StorageClientException e) {
        logger.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.warn(e.getMessage(), e);
      }
    }
    logger.debug("Got documents {} ", documents);
    return documents;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDeleteQueries(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<String> getDeleteQueries(RepositorySession repositorySession,
      Event event) {
    logger.debug("GetDelete for {} ", event);
    String groupName = (String) event.getProperty(UserConstants.EVENT_PROP_USERID);
    return ImmutableList.of("id:" + groupName);
  }

  // ---------- internal methods -----------------------------------------------
  /**
   * Gets the principals that can read content at a given path.
   *
   * @param session
   * @param path
   *          The path to check.
   * @return {@link String[]} of principal names that can read {@link path}. An empty
   *         array is returned if no principals can read the path.
   * @throws StorageClientException
   */
  private String[] getReadingPrincipals(Session session, String path)
      throws StorageClientException {
    AccessControlManager accessControlManager = session.getAccessControlManager();
    return accessControlManager.findPrincipals(Security.ZONE_AUTHORIZABLES, path,
        Permissions.CAN_READ.getPermission(), true);
  }
}
