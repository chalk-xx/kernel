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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
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
@Component(immediate = true)
public class AuthorizableIndexingHandler implements IndexingHandler {
  private static final String[] DEFAULT_TOPICS = {
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.ADDED_TOPIC,
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.DELETE_TOPIC,
      StoreListener.TOPIC_BASE + "authorizables/" + StoreListener.UPDATED_TOPIC };

  // list of properties to be indexed
  private static final Map<String, String> USER_WHITELISTED_PROPS = ImmutableMap.of("firstName","firstName",
      "lastName","lastName","email","email","type","type","sakai:tag-uuid","taguuid");

  private static final Map<String, String> GROUP_WHITELISTED_PROPS = ImmutableMap.of(
      "name", "name", "type", "type", "sakai:group-title", "title", "sakai:group-description", "description","sakai:tag-uuid","taguuid");

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
    String name = (String) event.getProperty(FIELD_PATH);

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

          if (authorizable.isGroup()) {
            if (groupIsUserFacing(authorizable)) {
              // add group properties
              Map<String, String> fields = GROUP_WHITELISTED_PROPS;

              for (Entry<String, Object> p : properties.entrySet()) {
                if (fields.containsKey(p.getKey())) {
                  doc.addField(fields.get(p.getKey()), p.getValue());
                }
              }
            } else {
              // manager or contact group. no indexing to be done so exit the method
              return documents;
            }
          } else {
            // add user properties
            Map<String, String> fields = USER_WHITELISTED_PROPS;

            for (Entry<String, Object> p : properties.entrySet()) {
              if (fields.containsKey(p.getKey())) {
                doc.addField(fields.get(p.getKey()), p.getValue());
              }
            }
          }

          // add readers
          AccessControlManager accessControlManager = session.getAccessControlManager();
          String[] principals = accessControlManager.findPrincipals(
              Security.ZONE_AUTHORIZABLES, name, Permissions.CAN_READ.getPermission(),
              true);
          for (String principal : principals) {
            doc.addField(FIELD_READERS, principal);
          }

          // set the resource type and ID
          doc.setField(FIELD_RESOURCE_TYPE, "authorizable");
          doc.setField(FIELD_ID, name);

          documents.add(doc);
          logger.info("Added authorizable for searching: {}", name);
        }
      } catch (StorageClientException e) {
        logger.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.warn(e.getMessage(), e);
      }
    }
    logger.debug("Got documents {} ", documents);
    return documents;
  }

  // KERN-1607 don't include manager groups in the index
  // KERN-1600 don't include contact groups in the index
  private boolean groupIsUserFacing(Authorizable group) {
    boolean isNotManagingGroup = !group.hasProperty(UserConstants.PROP_MANAGED_GROUP);
    boolean hasTitleAndNotBlank = group.hasProperty("sakai:group-title")
        && !StringUtils.isBlank((String) group.getProperty("sakai:group-title"));

    return isNotManagingGroup && hasTitleAndNotBlank;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDeleteQueries(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<String> getDeleteQueries(RepositorySession repositorySession,
      Event event) {
    Collection<String> retval = Collections.emptyList();
    String topic = event.getTopic();
    if (topic.endsWith(StoreListener.DELETE_TOPIC) || topic.endsWith(StoreListener.UPDATED_TOPIC)) {
      logger.debug("GetDelete for {} ", event);
      String groupName = (String) event.getProperty(UserConstants.EVENT_PROP_USERID);
      retval = ImmutableList.of("id:" + ClientUtils.escapeQueryChars(groupName));
    }
    return retval;

  }
}
