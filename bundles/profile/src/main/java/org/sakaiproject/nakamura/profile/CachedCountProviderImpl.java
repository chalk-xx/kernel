package org.sakaiproject.nakamura.profile;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.profile.CountProvider;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Dictionary;
import java.util.Map;



@Component(label = "CountProvider", description = "Provider of users' total counts of group memberships, contacts and content owned or viewed", 
    immediate=true, metatype=true)
@Service(value = CountProvider.class)

public class CachedCountProviderImpl implements CountProvider {
  
  /**
   * This marks the nodename for the contact store's folder. 
   */
  // copied from ConnectionsConstants, can't use that class because it creates cyclical dependency between connections and user
  public static final String CONTACT_STORE_NAME = "contacts";
  
  private static final Logger LOG = LoggerFactory.getLogger(CachedCountProviderImpl.class);
  
  @Reference
  private SolrServerService solrSearchService;
  
  @Property(intValue = 30)
  private static final String UPDATE_INTERVAL = "sakai.countProvider.updateIntervalMinutes";
  
  private Integer updateIntervalMinutes = null;
  
  public Map<String, Integer> getAllNewCounts(Authorizable au, Session session)
      throws AccessDeniedException, StorageClientException {

    Integer contentItemsCount = getNewContentCount(au, session);
    Map<String, Integer> countsMap  = null;
    if (au instanceof User) {
      Integer contactsCount = getNewContactsCount(au, session);
      Integer membershipsCount = getNewGroupsCount(au, session);
      countsMap = ImmutableMap.of(CONTACTS_PROP, contactsCount,
        GROUP_MEMBERSHIPS_PROP, membershipsCount, CONTENT_ITEMS_PROP, contentItemsCount);
    }
    else if (au instanceof Group) {
      Integer membersCount = getNewMembersCount((Group)au, session);
      countsMap = ImmutableMap.of(GROUP_MEMBERS_PROP, membersCount, CONTENT_ITEMS_PROP, contentItemsCount);
    }
    if (LOG.isDebugEnabled()) LOG.debug("getAllNewCounts() countsMap: {} for authorizableId {}", new Object[]{countsMap, au.getId()});
    return countsMap;
  }

  public boolean needsRefresh(Authorizable authorizable) throws AccessDeniedException,
      StorageClientException {
    boolean needsRefresh = false;
    if (authorizable.getProperty(COUNTS_LAST_UPDATE_PROP) == null || countsExpired(authorizable)) {
      needsRefresh = true;
    }
    return needsRefresh;
  }
  
  public int getNewMembersCount(Group group, Session session)
      throws AccessDeniedException, StorageClientException {
    return 6;
  }
  

  public int getNewGroupsCount(Authorizable authorizable, Session session) throws AccessDeniedException, StorageClientException {
    return 3;
  }

  public int getNewContentCount(Authorizable authorizable, Session session)
      throws AccessDeniedException, StorageClientException {
    return 4;
  }

  public int getNewContactsCount(Authorizable au, Session session)
  throws AccessDeniedException, StorageClientException {
    return 5;
  }
  
  // not implemented yet
  public int getNewContactsCount1(Authorizable au, Session session)
      throws AccessDeniedException, StorageClientException {
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils
        .adaptToSession(session);
    String userID = au.getId();
    String store = LitePersonalUtils.getHomePath(userID) + "/"
        + CONTACT_STORE_NAME;
    store = ISO9075.encodePath(store);
    String queryString = "path:" + ClientUtils.escapeQueryChars(store)
        + " AND resourceType:sakai/contact AND state:(ACCEPTED OR INVITED OR PENDING)";
//    Query query = new Query(queryString);
    SolrServer solrServer = solrSearchService.getServer();
    SolrQuery solrQuery = new SolrQuery(queryString);
    
    
//    SolrSearchResultSet resultSet = searchServiceFactory.getSearchResultSet(request,
//        query, false);
    QueryResponse response;
    try {
      response = solrServer.query(solrQuery);
      // which result type to use?? -- see LiteMeServlet
      // have to use vanilla solr due to circular dependencvy on nakamura.search
      SolrDocumentList resultList = response.getResults();
//      SolrSearchResultSetImpl rs = new SolrSearchResultSetImpl(response);
//      Iterator<Result> resultIterator = rs.getResultSetIterator();
      LOG.info("Got {} hits in {} ms", resultList.size() , response.getElapsedTime());
      for (SolrDocument solrDocument : resultList) {
        LOG.info("  solrDocument=" + solrDocument.getFieldValuesMap());
        String fullPath = (String) solrDocument.getFirstValue("path");
        String userId = PathUtils.getAuthorizableId(fullPath);
//        matchingIds.add(userId);
      }
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return 99;
  }
  
  private boolean countsExpired(Authorizable authorizable) {
    boolean countsExpired = false;
    Long lastMillis = (Long) authorizable.getProperty(COUNTS_LAST_UPDATE_PROP);
    if (lastMillis != null) {
      long updateMillis = lastMillis + updateIntervalMinutes * 60 * 1000;
      long nowMillis = new Date().getTime();
      countsExpired =  nowMillis > updateMillis;
    }
    else {
      countsExpired = true;
    }
    return countsExpired;
  }
  
  // ---------- SCR integration ---------------------------------------------
  public void activate(ComponentContext componentContext) {
    LOG.info("CountProviderImpl.activate()");
    @SuppressWarnings("rawtypes")
    Dictionary props = componentContext.getProperties();
    Integer _updateIntervalMinutes = (Integer) props.get(UPDATE_INTERVAL);
    if (_updateIntervalMinutes != null) {
      this.updateIntervalMinutes = _updateIntervalMinutes;
    }
    else {
        LOG.error("SMTP retry interval not set.");
    }
  }

  protected void deactivate(ComponentContext ctx) {
    LOG.info("CountProviderImpl.deactivate()");
  }

}
