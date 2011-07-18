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
import com.google.common.collect.ImmutableMap.Builder;
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
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.TopicIndexer;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;
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
  private static final Map<String, String> USER_WHITELISTED_PROPS;
  static {
    Builder<String, String> builder = ImmutableMap.builder();
    builder.put(UserConstants.USER_FIRSTNAME_PROPERTY, "firstName");
    builder.put(UserConstants.USER_LASTNAME_PROPERTY, "lastName");
    builder.put(UserConstants.USER_EMAIL_PROPERTY, "email");
    builder.put("type", "type");
    builder.put("sakai:tag-uuid", "taguuid");
    builder.put("sakai:tags", "tag");
    builder.put(Authorizable.LASTMODIFIED_FIELD, Content.LASTMODIFIED_FIELD);
    builder.put(UserConstants.COUNTS_LAST_UPDATE_PROP, "countLastUpdate");
    USER_WHITELISTED_PROPS = builder.build();
  }

  private final static Map<String, String> GROUP_WHITELISTED_PROPS;
  static {
    Builder<String, String> builder = ImmutableMap.builder();
    builder.put("name", "name");
    builder.put("type", "type");
    builder.put(UserConstants.GROUP_TITLE_PROPERTY, "title");
    builder.put(UserConstants.GROUP_DESCRIPTION_PROPERTY, "description");
    builder.put("sakai:tag-uuid", "taguuid");
    builder.put("sakai:tags", "tag");
    builder.put("sakai:category", "category");
    builder.put(Authorizable.LASTMODIFIED_FIELD, Content.LASTMODIFIED_FIELD);    
    builder.put(UserConstants.COUNTS_LAST_UPDATE_PROP, "countLastUpdate");
    GROUP_WHITELISTED_PROPS = builder.build();
  }

  // list of authorizables to not index
  private static final Set<String> BLACKLISTED_AUTHZ = ImmutableSet.of("admin",
    "g-contacts-admin", "anonymous", "owner");

  private static final String SAKAI_PSEUDOGROUPPARENT_PROP = "sakai:pseudogroupparent";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Reference
  protected TopicIndexer topicIndexer;

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
    /*
     * This general process can be better generalized so that a group is sent through
     * common processing and only users are indexed in "group" but that's not necessary
     * yet. The processing below just indexes what is found rather than recursing down the
     * hierarchy. This suffices as long as the inherited members are not required to be
     * indexed which will raise the cost of indexing a group.
     */
    logger.debug("GetDocuments for {} ", event);
    List<SolrInputDocument> documents = Lists.newArrayList();
    String topic = PathUtils.lastElement(event.getTopic());

    if (StoreListener.UPDATED_TOPIC.equals(topic) || StoreListener.ADDED_TOPIC.equals(topic)) {
      // get the name of the authorizable (user,group)
      String authName = String.valueOf(event.getProperty(FIELD_PATH));
      Authorizable authorizable = getAuthorizable(authName, repositorySession);
      if (authorizable != null) {
        // KERN-1822 check if the authorizable is marked to be excluded from searches
        if (Boolean.parseBoolean(String.valueOf(authorizable.getProperty(UserConstants.SAKAI_EXCLUDE)))) {
          return documents;
        }

        SolrInputDocument doc = createAuthDoc(authorizable, repositorySession);
        if (doc != null) {
          documents.add(doc);

          logger.info("{} authorizable for searching: {}", topic, authName);
        }
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
    Collection<String> retval = Collections.emptyList();
    String topic = event.getTopic();
    if (topic.endsWith(StoreListener.DELETE_TOPIC)) {
      logger.debug("GetDelete for {} ", event);
      String groupName = String.valueOf(event.getProperty(UserConstants.EVENT_PROP_USERID));
      retval = ImmutableList.of("id:" + ClientUtils.escapeQueryChars(groupName));
    } else {
      // KERN-1822 check if the authorizable is marked to be excluded from searches
      String authName = String.valueOf(event.getProperty(FIELD_PATH));
      Authorizable authorizable = getAuthorizable(authName, repositorySession);
      if (authorizable != null
          && Boolean.parseBoolean(String.valueOf(authorizable.getProperty(UserConstants.SAKAI_EXCLUDE)))) {
        retval = ImmutableList.of("id:" + ClientUtils.escapeQueryChars(authName));
      }
    }
    return retval;

  }

  // ---------- internal methods ----------
  /**
   * Create the SolrInputDocument for an authorizable.
   *
   * @param authorizable
   * @param doc
   * @param properties
   * @return The SolrInputDocument or null if authorizable shouldn't be indexed.
   */
  protected SolrInputDocument createAuthDoc(Authorizable authorizable, RepositorySession repositorySession) {
    if (!isUserFacing(authorizable)) {
      return null;
    }

    // add user properties
    String authName = authorizable.getId();

    SolrInputDocument doc = new SolrInputDocument();
    Map<String, String> fields = (authorizable.isGroup()) ? GROUP_WHITELISTED_PROPS : USER_WHITELISTED_PROPS;

    Map<String, Object> properties = authorizable.getSafeProperties();
    for (Entry<String, Object> p : properties.entrySet()) {
      if (fields.containsKey(p.getKey())) {
        doc.addField(fields.get(p.getKey()), p.getValue());
      }
    }


    // add groups to the user doc so we can find the user as a group member
    if (!authorizable.isGroup()) {
      for (String principal : authorizable.getPrincipals()) {
        Group group = (Group) getAuthorizable(principal, repositorySession);
        if (group != null && group.hasProperty(SAKAI_PSEUDOGROUPPARENT_PROP)) {
          doc.addField("group", group.getProperty(SAKAI_PSEUDOGROUPPARENT_PROP));
        }
        doc.addField("group", principal);
      }
    }

    // add users to the group doc so we can find the group by its member users
    // (and recommend similar groups based on group membership too)
    if (authorizable.isGroup()) {
      Group group = (Group) authorizable;
      for (String member : group.getMembers()) {
        doc.addField(FIELD_READERS, member);
      }
    }

    // add readers
    try {
      Session session = repositorySession.adaptTo(Session.class);
      AccessControlManager accessControlManager = session.getAccessControlManager();
      String[] principals = accessControlManager.findPrincipals(
          Security.ZONE_AUTHORIZABLES, authName, Permissions.CAN_READ.getPermission(),
          true);
      for (String principal : principals) {
        doc.addField(FIELD_READERS, principal);
      }
    } catch (StorageClientException e) {
      logger.error(e.getMessage(), e);
    }

    // add the name as the return path so we can group on it later when we search
    // for widgetdata
    doc.setField(FIELD_PATH, authName);
    doc.setField("returnpath", authName);
    // set the resource type and ID
    doc.setField(FIELD_RESOURCE_TYPE, "authorizable");
    doc.setField(FIELD_ID, authName);

    return doc;
  }

  /**
   * Check if an authorizable is user facing.
   * 
   * KERN-1607 don't include manager groups in the index KERN-1600 don't include contact
   * groups in the index
   * 
   * @param auth
   *          The authorizable to check
   * @return true if the authorizable is not blacklisted and (is not a group or (is not a
   *         managing group and has a non-blank title)). false otherwise.
   */
  protected boolean isUserFacing(Authorizable auth) {
    if (auth == null || auth.getId() == null) {
      return false;
    }

    boolean isBlacklisted = BLACKLISTED_AUTHZ.contains(auth.getId());
    boolean isNotManagingGroup = !auth.hasProperty(UserConstants.PROP_MANAGED_GROUP);
    boolean hasTitleAndNotBlank = auth.hasProperty("sakai:group-title")
        && !StringUtils.isBlank(String.valueOf(auth.getProperty("sakai:group-title")));

    return !isBlacklisted && (!auth.isGroup() || (isNotManagingGroup && hasTitleAndNotBlank));
  }

  /**
   * Get an authorizable. Convenience method to handle exceptions and processing.
   *
   * @param authName ID of the authorizable to get.
   * @param repositorySession a solr RepositorySession
   * @return Authorizable found or null if none found.
   */
  protected Authorizable getAuthorizable(String authName, RepositorySession repositorySession) {
    Authorizable authorizable = null;
    try {
      Session session = repositorySession.adaptTo(Session.class);
      AuthorizableManager authzMgr = session.getAuthorizableManager();

      // get the name of the authorizable (user,group)
      authorizable = authzMgr.findAuthorizable(authName);
    } catch (StorageClientException e) {
      logger.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage(), e);
    }
    return authorizable;
  }
}
