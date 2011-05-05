package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
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
import java.util.Map;

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

  private Session adminSession;

  private AuthorizableManager authorizableManager;

  @Property(intValue = 30)
  private static final String UPDATE_INTERVAL = "sakai.countProvider.updateIntervalMinutes";

  private long updateInterval;

  public void update(Authorizable requestAu) throws AccessDeniedException,
      StorageClientException {
    Authorizable au = authorizableManager.findAuthorizable(requestAu.getId());
    if (au != null) {
      int contentCount = getContentCount(au);
      requestAu.setProperty(CONTENT_ITEMS_PROP, contentCount);
      au.setProperty(CONTENT_ITEMS_PROP, contentCount);
      Map<String, Integer> countsMap = null;
      if (au instanceof User) {
        int contactsCount = getContactsCount(au);
        int groupsContact = getGroupsCount(au);
        au.setProperty(CONTACTS_PROP, contactsCount);
        au.setProperty(GROUP_MEMBERSHIPS_PROP, groupsContact);
        requestAu.setProperty(CONTACTS_PROP, contactsCount);
        requestAu.setProperty(GROUP_MEMBERSHIPS_PROP, groupsContact);
      } else if (au instanceof Group) {
        int membersCount = getMembersCount((Group) au);
        au.setProperty(GROUP_MEMBERS_PROP, membersCount);
        requestAu.setProperty(GROUP_MEMBERS_PROP, membersCount);
      }
      if (LOG.isDebugEnabled())
        LOG.debug("getAllNewCounts() countsMap: {} for authorizableId {}", new Object[] {
            countsMap, au.getId() });
      long lastUpdate = System.currentTimeMillis();
      au.setProperty(COUNTS_LAST_UPDATE_PROP, lastUpdate);
      requestAu.setProperty(COUNTS_LAST_UPDATE_PROP, lastUpdate);
      // only update the Authorizable associated with the admin session.
      // NB we have updated the requestAuthorizable
      authorizableManager.updateAuthorizable(au);
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
    // this may not be absolutely correct
    return group.getMembers().length;
  }

  private int getGroupsCount(Authorizable authorizable) throws AccessDeniedException,
      StorageClientException {
    // this may not be absolutely correct.
    return authorizable.getPrincipals().length;
  }

  private int getContentCount(Authorizable au) throws AccessDeniedException,
      StorageClientException {
    // find the content where the user has been made either a viewer or a manager
    String userID = ClientUtils.escapeQueryChars(au.getId());
    // pooled-content-manager, pooled-content-viewer
    String queryString = "resourceType:sakai/pooled-content AND (viewer:" + userID
        + " OR manager:" + userID + ")";
    return getCount(queryString);
  }

  private int getContactsCount(Authorizable au) throws AccessDeniedException,
      StorageClientException {
    // find the number of contacts in the current users store that are in state Accepted,
    // invited or pending.
    String userID = au.getId();
    Authorizable g =   authorizableManager.findAuthorizable("g-contacts-"+userID);
    if ( g instanceof Group ) {
      return ((Group) g).getMembers().length;
    }
    return 0;
  }

  /**
   * @param queryString
   * @return the count of results, we assume if they are returned the user can read them
   *         and we do not iterate through the entire set to check.
   */
  private int getCount(String queryString) {
    SolrServer solrServer = solrSearchService.getServer();
    SolrQuery solrQuery = new SolrQuery(queryString);

    QueryResponse response;
    try {
      response = solrServer.query(solrQuery);
      return (int) response.getResults().getNumFound();
    } catch (SolrServerException e) {
      LOG.warn(e.getMessage(), e);
    }
    return 0;
  }

  // ---------- SCR integration ---------------------------------------------
  @Activate
  public void activate(ComponentContext componentContext) throws StorageClientException,
      AccessDeniedException {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> props = componentContext.getProperties();
    updateInterval = OsgiUtil.toInteger(props.get(UPDATE_INTERVAL), 30) * 30 * 1000;

    adminSession = repository.loginAdministrative();
    authorizableManager = adminSession.getAuthorizableManager();
  }

  @Deactivate
  protected void deactivate(ComponentContext ctx) throws ClientPoolException {
    authorizableManager = null;
    adminSession.logout();
    adminSession = null;

  }

}
