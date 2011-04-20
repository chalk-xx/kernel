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

import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component(immediate = true)
public class AuthorizableMembershipIndexingHandler extends AuthorizableIndexingHandler {
  private static final Logger logger = LoggerFactory.getLogger(AuthorizableMembershipIndexingHandler.class);

  private static final String[] DEFAULT_TOPICS = {
    UserConstants.TOPIC_GROUP_CREATED,
    StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.ADDED_TOPIC,
    StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.UPDATED_TOPIC
  };

  // ---------- SCR integration ------------------------------------------------
  @Override
  @Activate
  protected void activate(Map<?, ?> props) {
    for (String topic : DEFAULT_TOPICS) {
      topicIndexer.addHandler(topic, this);
    }
  }

  @Override
  @Deactivate
  protected void deactivate(Map<?, ?> props) {
    for (String topic : DEFAULT_TOPICS) {
      topicIndexer.removeHandler(topic, this);
    }
  }

  // ---------- IndexingHandler interface --------------------------------------
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDocuments(org.sakaiproject.nakamura.api.solr.RepositorySession, org.osgi.service.event.Event)
   */
  @Override
  public Collection<SolrInputDocument> getDocuments(RepositorySession repositorySession,
      Event event) {
    logger.debug("GetDocuments for {} ", event);

    List<SolrInputDocument> documents = Lists.newArrayList();
    try {
      Session session = repositorySession.adaptTo(Session.class);
      AuthorizableManager authzMgr = session.getAuthorizableManager();

      if (UserConstants.TOPIC_GROUP_CREATED.equals(event.getTopic())) {
        // this event should occur after the group thrashing has calmed down.
        String authName = String.valueOf(event.getProperty("userid"));
        Authorizable authorizable = authzMgr.findAuthorizable(authName);
        if (isUserFacing(authorizable)) {
          Authorizable authorizableMgrs = authzMgr.findAuthorizable(authName + "-managers");
          Group authMgrs = (Group) authorizableMgrs;
          String[] memberIds = authMgrs.getMembers();
          // update the docs for authorizables set initially
          processMembers(documents, session, memberIds);
  
          logger.info("Indexed members of authorizable [{}]", authorizable);
        }
      } else {
        String authName = String.valueOf(event.getProperty("path"));
        Authorizable authorizable = authzMgr.findAuthorizable(authName);
        if (authorizable.isGroup()) {
          // update the docs for authorizables that were added to the group
          String added = (String) event.getProperty("added");
          if (added != null) {
            processMembers(documents, session, added);
          }

          // update the docs for authorizables that were removed from the group
          String removed = (String) event.getProperty("removed");
          if (removed != null) {
            processMembers(documents, session, removed);
          }
        }

        logger.info("Added/removed members of authorizable [{}]", authorizable);
      }
    } catch (StorageClientException e) {
      logger.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage(), e);
    }
    return documents;
  }

  /**
   * Nothing to do here since we only respond to add events
   *
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDeleteQueries(org.sakaiproject.nakamura.api.solr.RepositorySession, org.osgi.service.event.Event)
   */
  @Override
  public Collection<String> getDeleteQueries(RepositorySession respositorySession,
      Event event) {
    return Collections.emptyList();
  }


  /**
   * @param event
   * @param documents
   * @param session
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  protected void processMembers(List<SolrInputDocument> documents, Session session, String... userIds) throws StorageClientException, AccessDeniedException {
    if (userIds != null && userIds.length > 0) {
      if (userIds.length == 1) {
        userIds = StringUtils.split(userIds[0], ',');
      }
      for (String userId : userIds) {
        if (!StringUtils.isBlank(userId)) {
          SolrInputDocument userDoc = createAuthDoc(userId, session);
          if (userDoc != null) {
            documents.add(userDoc);
          }
        }
      }
    }
  }
}
