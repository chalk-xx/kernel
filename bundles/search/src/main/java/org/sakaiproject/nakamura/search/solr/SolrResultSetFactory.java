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
package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.ResultSetFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.sakaiproject.nakamura.api.templates.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 */
@Component(metatype = true)
@Service
@Property(name = "type", value = Query.SOLR)
public class SolrResultSetFactory implements ResultSetFactory {
  @Property(longValue = 100L)
  private static final String VERY_SLOW_QUERY_TIME = "verySlowQueryTime";
  @Property(longValue = 10L)
  private static final String SLOW_QUERY_TIME = "slowQueryTime";
  @Property(intValue = 100)
  private static final String DEFAULT_MAX_RESULTS = "defaultMaxResults";

  /** only used to mark the logger */
  private final class SlowQueryLogger { }

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrResultSetFactory.class);
  private static final Logger SLOW_QUERY_LOGGER = LoggerFactory.getLogger(SlowQueryLogger.class);

  @Reference
  private SolrServerService solrSearchService;

  private int defaultMaxResults = 100; // set to 100 to allow testing
  private long slowQueryThreshold;
  private long verySlowQueryThreshold;

  @Activate
  protected void activate(Map<?, ?> props) {
    defaultMaxResults = OsgiUtil.toInteger(props.get(DEFAULT_MAX_RESULTS),
        defaultMaxResults);
    slowQueryThreshold = OsgiUtil.toLong(props.get(SLOW_QUERY_TIME), 10L);
    verySlowQueryThreshold = OsgiUtil.toLong(props.get(VERY_SLOW_QUERY_TIME), 100L);
  }

  /**
   * Process a query string to search using Solr.
   *
   * @param request
   * @param query
   * @param asAnon
   * @param rs
   * @return
   * @throws SolrSearchException
   */
  public SolrSearchResultSet processQuery(SlingHttpServletRequest request, Query query,
      boolean asAnon) throws SolrSearchException {
    try {
      String queryString = query.getQueryString();
      // apply readers restrictions.
      if (asAnon) {
        queryString = "(" + queryString + ")  AND readers:" + User.ANON_USER;
      } else {
        Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
        if (!User.ADMIN_USER.equals(session.getUserId())) {
          AuthorizableManager am = session.getAuthorizableManager();
          Authorizable user = am.findAuthorizable(session.getUserId());
          Set<String> readers = Sets.newHashSet();
          for (Iterator<Group> gi = user.memberOf(am); gi.hasNext();) {
            readers.add(SearchUtil.escapeString(gi.next().getId(), Query.SOLR));
          }
          readers.add(session.getUserId());
          queryString = "(" + queryString + ") AND readers:(" + StringUtils.join(readers," OR ") + ")";
        }
      }

      SolrQuery solrQuery = buildQuery(request, queryString, query.getOptions());

      SolrServer solrServer = solrSearchService.getServer();
      if ( LOGGER.isDebugEnabled()) {
        try {
          LOGGER.debug("Performing Query {} ", URLDecoder.decode(solrQuery.toString(),"UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
      }
      long tquery = System.currentTimeMillis();
      QueryResponse response = solrServer.query(solrQuery);
      tquery = System.currentTimeMillis() - tquery;
      try {
        if ( tquery > verySlowQueryThreshold ) {
          SLOW_QUERY_LOGGER.error("Very slow solr query {} ms {} ",tquery, URLDecoder.decode(solrQuery.toString(),"UTF-8"));
        } else if ( tquery > slowQueryThreshold ) {
          SLOW_QUERY_LOGGER.warn("Slow solr query {} ms {} ",tquery, URLDecoder.decode(solrQuery.toString(),"UTF-8"));
        }
      } catch (UnsupportedEncodingException e) {
      }
      SolrSearchResultSetImpl rs = new SolrSearchResultSetImpl(response);
      if ( LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got {} hits in {} ms", rs.getSize(), response.getElapsedTime());
      }
      return rs;
    } catch (StorageClientException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (AccessDeniedException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (SolrServerException e) {
        throw new SolrSearchException(500, e.getMessage());
    }
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
    long[] ranges = SolrSearchUtil.getOffsetAndSize(request, options);
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
