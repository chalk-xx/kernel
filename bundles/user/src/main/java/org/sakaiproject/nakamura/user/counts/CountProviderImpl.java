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
package org.sakaiproject.nakamura.user.counts;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(metatype = true)
@Service
public class CountProviderImpl implements CountProvider {

  /**
   * This marks the nodename for the contact store's folder.
   */
  // copied from ConnectionsConstants, can't use that class because it creates cyclical
  // dependency between connections and user
  public static final String CONTACT_STORE_NAME = "contacts";

  private static final Logger LOG = LoggerFactory.getLogger(CountProviderImpl.class);

  @Reference
  protected SolrServerService solrSearchService;

  @Reference
  protected Repository repository;

  @Property(intValue = 30)
  public static final String UPDATE_INTERVAL_MINUTES = "sakai.countProvider.updateIntervalMinutes";

  private long updateIntervalMinutes;

  private GroupMembershipCounter groupMembershipCounter = new GroupMembershipCounter();

  private ConnectionsCounter contactsCounter = new ConnectionsCounter();

  private ContentCounter contentCounter = new ContentCounter();

  private GroupMembersCounter groupMembersCounter = new GroupMembersCounter();

  public void update(Authorizable authorizable, Session session)
      throws AccessDeniedException, StorageClientException {
    if (authorizable == null || IGNORE_AUTHIDS.contains(authorizable.getId())) {
      return;
    }
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    if (authorizable != null) {
      int contentCount = getContentCount(authorizable);
      authorizable.setProperty(UserConstants.CONTENT_ITEMS_PROP, contentCount);
      if (authorizable instanceof User) {
        int contactsCount = getContactsCount(authorizable, authorizableManager);
        int groupsContact = getGroupsCount(authorizable, authorizableManager);
        authorizable.setProperty(UserConstants.CONTACTS_PROP, contactsCount);
        authorizable.setProperty(UserConstants.GROUP_MEMBERSHIPS_PROP, groupsContact);
        if (LOG.isDebugEnabled())
          LOG.debug("update User authorizable: {} with {}={}, {}={}, {}={}",
              new Object[] { authorizable.getId(), UserConstants.CONTENT_ITEMS_PROP,
                  contentCount, UserConstants.CONTACTS_PROP, contactsCount,
                  UserConstants.GROUP_MEMBERSHIPS_PROP, groupsContact });
      } else if (authorizable instanceof Group) {
        int membersCount = getMembersCount((Group) authorizable, authorizableManager);
        authorizable.setProperty(UserConstants.GROUP_MEMBERS_PROP, membersCount);
        if (LOG.isDebugEnabled())
          LOG.debug("update Group authorizable: {} with {}={}, {}={}", new Object[] {
              authorizable.getId(), UserConstants.CONTENT_ITEMS_PROP, contentCount,
              UserConstants.GROUP_MEMBERS_PROP, membersCount });
      }
      long lastUpdate = System.currentTimeMillis();
      authorizable.setProperty(UserConstants.COUNTS_LAST_UPDATE_PROP, lastUpdate);
      if (LOG.isDebugEnabled())
        LOG.debug("update authorizable: {} with {}={}", new Object[] {
            authorizable.getId(), UserConstants.COUNTS_LAST_UPDATE_PROP, lastUpdate});      
      authorizableManager.updateAuthorizable(authorizable);
    } else {
      LOG.warn("update could not get authorizable: {} from session",
          new Object[] { authorizable.getId() });
    }
  }
  
  public long getUpdateIntervalMinutes() {
    return this.updateIntervalMinutes;
  }

  private int getMembersCount(Group group, AuthorizableManager authorizableManager) throws AccessDeniedException,
      StorageClientException {
    return groupMembersCounter.count(group, authorizableManager);
  }

  private int getGroupsCount(Authorizable au, AuthorizableManager authorizableManager)
      throws AccessDeniedException, StorageClientException {
    return groupMembershipCounter.count(au,authorizableManager);
  }

  private int getContentCount(Authorizable au) throws AccessDeniedException,
      StorageClientException {
    return contentCounter.countExact(au, solrSearchService);
  }

  private int getContactsCount(Authorizable au, AuthorizableManager authorizableManager)
      throws AccessDeniedException, StorageClientException {
    return contactsCounter.count(au,authorizableManager);
  }


  // ---------- SCR integration ---------------------------------------------
  @Activate
  public void activate(Map<String, Object> properties) throws StorageClientException,
      AccessDeniedException {
    modify(properties);
  }
  @Modified
  public void modify(Map<String, Object> properties) throws StorageClientException,
      AccessDeniedException {
    updateIntervalMinutes = OsgiUtil.toLong(properties.get(UPDATE_INTERVAL_MINUTES), 30) * 60 * 1000;
  }


}
