package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.profile.CountProvider;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

@Component(immediate = true, metatype = true)
@Service(value = CountProvider.class)
public class CountProviderImpl implements CountProvider {

  /**
   * This marks the nodename for the contact store's folder.
   */
  // copied from ConnectionsConstants, can't use that class because it creates cyclical
  // dependency between connections and user
  public static final String CONTACT_STORE_NAME = "contacts";

  private static final Logger LOG = LoggerFactory.getLogger(CountProviderImpl.class);

  @Reference
  private SolrServerService solrSearchService;

  @Reference
  private Repository repository;

  @Property(intValue = 30)
  private static final String UPDATE_INTERVAL = "sakai.countProvider.updateIntervalMinutes";

  private long updateInterval;

  private GroupMembershipCounter groupMembershipCounter = new GroupMembershipCounter();

  private ConnectionsCounter contactsCounter = new ConnectionsCounter();

  private ContentCounter contentCounter = new ContentCounter();

  private GroupMembersCounter groupMembersCounter = new GroupMembersCounter();

  public void update(Authorizable requestAu) throws AccessDeniedException,
      StorageClientException {

    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      Authorizable au = authorizableManager.findAuthorizable(requestAu.getId());
      if (au != null) {
        int contentCount = getContentCount(au);
        requestAu.setProperty(CONTENT_ITEMS_PROP, contentCount);
        au.setProperty(CONTENT_ITEMS_PROP, contentCount);
        if (au instanceof User) {
          int contactsCount = getContactsCount(au, authorizableManager);
          int groupsContact = getGroupsCount(au, authorizableManager);
          au.setProperty(CONTACTS_PROP, contactsCount);
          au.setProperty(GROUP_MEMBERSHIPS_PROP, groupsContact);
          requestAu.setProperty(CONTACTS_PROP, contactsCount);
          requestAu.setProperty(GROUP_MEMBERSHIPS_PROP, groupsContact);
          if (LOG.isDebugEnabled())
            LOG.debug("update User authorizable: {} with {}={}, {}={}, {}={}",
                new Object[] { requestAu.getId(), CONTENT_ITEMS_PROP, contentCount,
                    CONTACTS_PROP, contactsCount, GROUP_MEMBERSHIPS_PROP, groupsContact });
        } else if (au instanceof Group) {
          int membersCount = getMembersCount((Group) au);
          au.setProperty(GROUP_MEMBERS_PROP, membersCount);
          requestAu.setProperty(GROUP_MEMBERS_PROP, membersCount);
          if (LOG.isDebugEnabled())
            LOG.debug("update Group authorizable: {} with {}={}, {}={}", new Object[] {
                requestAu.getId(), CONTENT_ITEMS_PROP, contentCount, GROUP_MEMBERS_PROP,
                membersCount });
        }
        long lastUpdate = System.currentTimeMillis();
        au.setProperty(COUNTS_LAST_UPDATE_PROP, lastUpdate);
        // only update the Authorizable associated with the admin session.
        // NB we have updated the requestAuthorizable
        authorizableManager.updateAuthorizable(au);
        authorizableManager.updateAuthorizable(requestAu);
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
    Long lastMillis = (Long) authorizable.getProperty(COUNTS_LAST_UPDATE_PROP);
    if (lastMillis != null) {
      long updateMillis = lastMillis + updateInterval;
      long nowMillis = System.currentTimeMillis();
      return nowMillis > updateMillis;
    }
    return true;
  }

  private int getMembersCount(Group group) throws AccessDeniedException,
      StorageClientException {
    return groupMembersCounter.count(group);
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
  public void activate(ComponentContext componentContext) throws StorageClientException,
      AccessDeniedException {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> props = componentContext.getProperties();
    updateInterval = OsgiUtil.toInteger(props.get(UPDATE_INTERVAL), 30) * 60 * 1000;
  }

  @Deactivate
  protected void deactivate(ComponentContext ctx) throws ClientPoolException {

  }

}
