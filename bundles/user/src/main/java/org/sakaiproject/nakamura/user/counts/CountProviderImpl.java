package org.sakaiproject.nakamura.user.counts;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
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
  private static final String UPDATE_INTERVAL = "sakai.countProvider.updateIntervalMinutes";

  private long updateInterval;

  private GroupMembershipCounter groupMembershipCounter = new GroupMembershipCounter();

  private ConnectionsCounter contactsCounter = new ConnectionsCounter();

  private ContentCounter contentCounter = new ContentCounter();

  private GroupMembersCounter groupMembersCounter = new GroupMembersCounter();

  public void update(Authorizable requestAu) throws AccessDeniedException,
      StorageClientException {
    if ( requestAu == null || IGNORE_AUTHIDS.contains(requestAu.getId())) {
      return;
    }
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      Authorizable au = authorizableManager.findAuthorizable(requestAu.getId());
      if (au != null) {
        int contentCount = getContentCount(au);
        requestAu.setProperty(UserConstants.CONTENT_ITEMS_PROP, contentCount);
        au.setProperty(UserConstants.CONTENT_ITEMS_PROP, contentCount);
        if (au instanceof User) {
          int contactsCount = getContactsCount(au, authorizableManager);
          int groupsContact = getGroupsCount(au, authorizableManager);
          au.setProperty(UserConstants.CONTACTS_PROP, contactsCount);
          au.setProperty(UserConstants.GROUP_MEMBERSHIPS_PROP, groupsContact);
          requestAu.setProperty(UserConstants.CONTACTS_PROP, contactsCount);
          requestAu.setProperty(UserConstants.GROUP_MEMBERSHIPS_PROP, groupsContact);
          if (LOG.isDebugEnabled())
            LOG.debug("update User authorizable: {} with {}={}, {}={}, {}={}",
                new Object[] { requestAu.getId(), UserConstants.CONTENT_ITEMS_PROP, contentCount,
                UserConstants.CONTACTS_PROP, contactsCount, UserConstants.GROUP_MEMBERSHIPS_PROP, groupsContact });
        } else if (au instanceof Group) {
          int membersCount = getMembersCount((Group) au, authorizableManager);
          au.setProperty(UserConstants.GROUP_MEMBERS_PROP, membersCount);
          requestAu.setProperty(UserConstants.GROUP_MEMBERS_PROP, membersCount);
          if (LOG.isDebugEnabled())
            LOG.debug("update Group authorizable: {} with {}={}, {}={}", new Object[] {
                requestAu.getId(), UserConstants.CONTENT_ITEMS_PROP, contentCount, UserConstants.GROUP_MEMBERS_PROP,
                membersCount });
        }
        long lastUpdate = System.currentTimeMillis();
        au.setProperty(UserConstants.COUNTS_LAST_UPDATE_PROP, lastUpdate);
        requestAu.setProperty(UserConstants.COUNTS_LAST_UPDATE_PROP, lastUpdate);
        // only update the Authorizable associated with the admin session.
        // NB we have updated the requestAuthorizable
        authorizableManager.updateAuthorizable(au);
      } else {
        LOG.warn("update could not get authorizable: {} from adminSession",
            new Object[] { requestAu.getId() });
      }
    } finally {
      if ( adminSession != null ) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOG.warn(e.getMessage(),e);
        }
      }
    }
  }

  public boolean needsRefresh(Authorizable authorizable) throws AccessDeniedException,
      StorageClientException {
    if (authorizable != null && !IGNORE_AUTHIDS.contains(authorizable.getId())) {
      Long lastMillis = (Long) authorizable.getProperty(UserConstants.COUNTS_LAST_UPDATE_PROP);
      if (lastMillis != null) {
        long updateMillis = lastMillis + updateInterval;
        long nowMillis = System.currentTimeMillis();
        LOG.debug("Last Udpate last:{} interval:{} updateafter:{} needsupdate:{}  {} ",
            new Object[] { lastMillis, updateInterval, updateMillis,
                (updateMillis - nowMillis), (nowMillis > updateMillis) });
        return nowMillis > updateMillis;
      }
      return true;
    }
    return false;
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
    updateInterval = OsgiUtil.toLong(properties.get(UPDATE_INTERVAL), 30) * 60 * 1000;
  }


}
