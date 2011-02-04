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
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
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
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, metatype = true)
@Service
public class SolrSearchServiceFactoryImpl implements SolrSearchServiceFactory {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SolrSearchServiceFactoryImpl.class);
  @Reference
  private SolrServerService solrSearchSearvice;

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      String query, boolean asAnon) throws SolrSearchException {
    try {

      // separate the query from the options
      String options = "";
      int semiPos = query.lastIndexOf(';');
      if (semiPos >= 0) {
        options = query.substring(semiPos);
        query = query.substring(0, semiPos);
      }

      // apply readers restrictions.
      if (asAnon) {
        query = "+(" + query + ")  AND +readers:"
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
          query = " +(" + query + ") AND +readers:(" + StringUtils.join(readers," OR ") + " )";
        }
      }
      // cat the query and options back together
      query += options;

      SolrQuery solrQuery = new SolrQuery(query);
      long[] ranges = SolrSearchUtil.getOffsetAndSize(request);
      solrQuery.setStart((int) ranges[0]);
      solrQuery.setRows((int) ranges[1]);

      SolrServer solrServer = solrSearchSearvice.getServer();
      try {
        LOGGER.info("Performing Query {} ", URLDecoder.decode(solrQuery.toString(),"UTF-8"));
      } catch (UnsupportedEncodingException e) {
      }
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList resultList = response.getResults();
      LOGGER.info("Got {} hitsin {} ", resultList.size() , response.getElapsedTime());
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
      String query) throws SolrSearchException {
    return getSearchResultSet(request, query, false);
  }

}
