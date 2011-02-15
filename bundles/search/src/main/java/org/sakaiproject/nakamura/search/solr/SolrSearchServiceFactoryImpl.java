package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, metatype = true)
@Service
public class SolrSearchServiceFactoryImpl implements SolrSearchServiceFactory {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SolrSearchServiceFactoryImpl.class);
  @Reference
  private SolrServerService solrSearchService;

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query, boolean asAnon) throws SolrSearchException {
    try {
      String queryString = query.getQueryString();
      // apply readers restrictions.
      if (asAnon) {
        queryString = "(" + queryString + ")  AND readers:"
            + org.sakaiproject.nakamura.api.lite.authorizable.User.ANON_USER;
      } else {
        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (!org.sakaiproject.nakamura.api.lite.authorizable.User.ADMIN_USER
            .equals(session.getUserID())) {
          UserManager userManager = AccessControlUtil.getUserManager(session);
          User user = (User) userManager.getAuthorizable(session.getUserID());
          Set<String> readers = Sets.newHashSet();
          for (Iterator<Group> gi = user.memberOf(); gi.hasNext();) {
            readers.add(gi.next().getID());
          }
          readers.add(session.getUserID());
          queryString = "(" + queryString + ") AND readers:(" + StringUtils.join(readers," OR ") + ")";
        }
      }

      SolrQuery solrQuery = buildQuery(request, queryString, query.getOptions());

      SolrServer solrServer = solrSearchService.getServer();
      try {
        LOGGER.info("Performing Query {} ", URLDecoder.decode(solrQuery.toString(),"UTF-8"));
      } catch (UnsupportedEncodingException e) {
      }
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList resultList = response.getResults();
      LOGGER.info("Got {} hits in {} ms", resultList.size() , response.getElapsedTime());
      return new SolrSearchResultSetImpl(response);
    } catch (SolrServerException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SolrSearchException(500, e.getMessage());
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SolrSearchException(500, e.getMessage());
    }
  }

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return getSearchResultSet(request, query, false);
  }

  /**
   * @param request
   * @param query
   * @param queryString
   * @return
   */
  private SolrQuery buildQuery(SlingHttpServletRequest request, String queryString,
      Map<String, String> options) {
    // build the query
    SolrQuery solrQuery = new SolrQuery(queryString);
    long[] ranges = SolrSearchUtil.getOffsetAndSize(request);
    solrQuery.setStart((int) ranges[0]);
    solrQuery.setRows((int) ranges[1]);

    // add in some options
    if (options != null) {
      for (Entry<String, String> option : options.entrySet()) {
        String key = option.getKey();
        String val = option.getValue();
        if (CommonParams.SORT.equals(key)) {
          parseSort(solrQuery, val);
        } else {
          solrQuery.set(key, val);
        }
      }
    }
    return solrQuery;
  }

  /**
   * @param options
   * @param solrQuery
   * @param val
   */
  private void parseSort(SolrQuery solrQuery, String val) {
    String[] sort = StringUtils.split(val);
    switch (sort.length) {
      case 1:
      solrQuery.setSortField(sort[0], ORDER.asc);
      break;
    case 2:
      String sortOrder = sort[1].toLowerCase();
      ORDER o = ORDER.asc;
      try {
        o = ORDER.valueOf(sortOrder);
      } catch ( IllegalArgumentException a) {
        if ( sortOrder.startsWith("d") ) {
          o = ORDER.desc;
        } else {
          o = ORDER.asc;
        }
      }
      solrQuery.setSortField(sort[0], o);
      break;
    default:
      LOGGER.warn("Expected the sort option to be 1 or 2 terms. Found: {}", val);
    }
  }
}
