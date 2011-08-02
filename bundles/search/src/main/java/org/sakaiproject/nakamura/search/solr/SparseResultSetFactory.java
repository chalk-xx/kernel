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

import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_PAGE;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_ITEMS_PER_PAGE;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.solr.schema.TextField;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.StorageConstants;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.ResultSetFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Component(metatype = true)
@Service
@Property(name = "type", value = Query.SPARSE)
public class SparseResultSetFactory implements ResultSetFactory {
  @Property(longValue = 100L)
  private static final String VERY_SLOW_QUERY_TIME = "verySlowQueryTime";
  @Property(longValue = 10L)
  private static final String SLOW_QUERY_TIME = "slowQueryTime";
  @Property(intValue = 100)
  private static final String DEFAULT_MAX_RESULTS = "defaultMaxResults";

  private int defaultMaxResults = 100; // set to 100 to allow testing
  private long slowQueryThreshold;
  private long verySlowQueryThreshold;

  /** only used to mark the logger */
  private final class SlowQueryLogger { }
  
  private static final Logger SLOW_QUERY_LOGGER = LoggerFactory.getLogger(SlowQueryLogger.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(SparseResultSetFactory.class);

  @Activate
  protected void activate(Map<?, ?> props) {
    defaultMaxResults = OsgiUtil.toInteger(props.get(DEFAULT_MAX_RESULTS),
        defaultMaxResults);
    slowQueryThreshold = OsgiUtil.toLong(props.get(SLOW_QUERY_TIME), 10L);
    verySlowQueryThreshold = OsgiUtil.toLong(props.get(VERY_SLOW_QUERY_TIME), 100L);
  }

  /**
   * Process properties to query sparse content directly.
   *
   * @param request
   * @param query
   * @param asAnon
   * @return
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  public SolrSearchResultSet processQuery(SlingHttpServletRequest request, Query query,
      boolean asAnon) throws SolrSearchException {
    try {
    // use solr parsing to get the terms from the query string
    QueryParser parser = new QueryParser(Version.LUCENE_40, "id",
        new TextField().getQueryAnalyzer());
    org.apache.lucene.search.Query luceneQuery = parser.parse(query.getQueryString());

    Map<String, Object> props = Maps.newHashMap();
    if (luceneQuery instanceof BooleanQuery) {
      BooleanQuery boolLucQuery = (BooleanQuery) luceneQuery;

      int orCount = 0;
      List<BooleanClause> clauses = boolLucQuery.clauses();
      for (BooleanClause clause : clauses) {
        org.apache.lucene.search.Query clauseQuery = clause.getQuery();
        Map<String, Object> subOrs = Maps.newHashMap();
        // we support 1 level of nesting for OR clauses
        if (clauseQuery instanceof BooleanQuery) {
          for (BooleanClause subclause : ((BooleanQuery) clauseQuery).clauses()) {
            org.apache.lucene.search.Query subclauseQuery = subclause.getQuery();
            extractTerms(subclause, subclauseQuery, props, subOrs);
          }
          props.put("orset" + orCount, subOrs);
          orCount++;
        } else {
          extractTerms(clause, clauseQuery, props, subOrs);
          if (!subOrs.isEmpty()) {
            props.put("orset" + orCount, subOrs);
            orCount++;
          }
        }
      }
    } else {
      extractTerms(null, luceneQuery, props, null);
    }

    // add the options to the parameters but prepend _ to avoid collision
    for (Entry<String, String> option : query.getOptions().entrySet()) {
      props.put("_" + option.getKey(), option.getValue());
    }
    
    String name = query.getName();
    if ( name != null ) {
       props.put(StorageConstants.CUSTOM_STATEMENT_SET, name);
    }

    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));
    ContentManager cm = session.getContentManager();
    long tquery = System.currentTimeMillis();
    Iterable<Content> items = cm.find(props);
    tquery = System.currentTimeMillis() - tquery;
    try {
      if ( tquery > verySlowQueryThreshold ) {
        SLOW_QUERY_LOGGER.error("Very slow sparse query {} ms {} ",tquery, URLDecoder.decode(query.toString(),"UTF-8"));
      } else if ( tquery > slowQueryThreshold ) {
        SLOW_QUERY_LOGGER.warn("Slow sparse query {} ms {} ",tquery, URLDecoder.decode(query.toString(),"UTF-8"));
      }
    } catch (UnsupportedEncodingException e) {
        // quietly swallow this exception
      LOGGER.debug(e.getLocalizedMessage(), e);
    }
    SolrSearchResultSet rs = new SparseSearchResultSet(items, defaultMaxResults);
    return getResultSetWithCount(rs, props, cm);
    } catch (AccessDeniedException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (StorageClientException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (ParseException e) {
      throw new SolrSearchException(500, e.getMessage());
    }
  }

  /**
   * A standard paged OAE search returns a "total" value in the response. This may not
   * be an exact count of all potential matches, but should be enough to let
   * client-side code display a reasonable paging UX.  With a Solr search, "total" is
   * derived from SolrDocumentList's getNumFound(). But a Sparse query
   * result provides no equivalent functionality, and Sparse currently doesn't support
   * SQL-style "count" queries. As a result, the only way to find out if there are
   * more matches available is to do more searching and iterating.
   *
   * What's supplied here is a very rough negotiation between (A) leaving the client-side
   * completely in the dark and (B) retrieving every single match in the DB on every
   * single query.
   * <ul>
   * <li>If the current page's search returned more than zero results but fewer than the page size,
   * then this is the last page available. Estimate "total" as the current page offset plus
   * the current number of results.
   * <li>If the current page's search returned zero results, then we assume that any earlier pages
   * were full, and we estimate "total" as the current page offset. This may be wildly off, but it
   * should help keep client-side paging from vanishing unexpectedly.
   * <li>If the current page's search returned a full page of results, then a second query is needed
   * to hint at the remaining count. We try to retrieve a maximum-page-size's worth of results
   * as a compromise between speed and accuracy.
   * </ul>
   *
   * @param queryResultSet
   * @param props
   * @param cm
   * @return
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private SolrSearchResultSet getResultSetWithCount(SolrSearchResultSet queryResultSet,
      Map<String, Object> props, ContentManager cm) throws StorageClientException, AccessDeniedException {
    final SolrSearchResultSet finalResultSet;
    final long queryCount = queryResultSet.getSize();

    if (queryCount < 0) {
      // Negative sizes signal "more than can be counted," and require no further
      // tinkering.
      finalResultSet = queryResultSet;
    } else {
      // Solr search results include both the desired page of results and a fuller count
      // of matches. Sparse queries only return the requested page of results, with no
      // other information.
      final long count;
      long nitems = Long.valueOf(String.valueOf(props.get("_" + PARAMS_ITEMS_PER_PAGE)));
      long page = Long.valueOf(String.valueOf(props.get("_" + PARAMS_PAGE)));
      long offset = page * nitems;
      if (queryCount == 0) {
        if (page > 0) {
          // If the current page results are empty, that says nothing about whether
          // earlier pages would have been full. It's possible that there are no matches
          // at all, in which case the reported count will be even more misleading than usual.
          try {
            LOGGER.info("Empty results from paged sparse query {}", URLDecoder.decode(props.toString(),"UTF-8"));
          } catch (UnsupportedEncodingException e) {
            LOGGER.debug(e.getLocalizedMessage(), e);
          }
        }
        count = offset;
      } else {
        // Currently the only way to get a count of Sparse matches outside the specified
        // page range is to perform a larger paged query. Sparse query restrictions
        // mean that a very inaccurate count is still very likely (as compared to
        // a "count()" query in SQL).
        if (queryCount == nitems) {
          long nextOffset = offset + nitems;
          long countStartPage = nextOffset / defaultMaxResults;
          long countOffset = countStartPage * defaultMaxResults;
          props.put("_" + PARAMS_PAGE, Long.toString(countStartPage));
          props.put("_" + PARAMS_ITEMS_PER_PAGE, Integer.toString(defaultMaxResults));
          long tquery = System.currentTimeMillis();
          Iterable<Content> countItems = cm.find(props);
          tquery = System.currentTimeMillis() - tquery;
          try {
            if ( tquery > verySlowQueryThreshold ) {
              SLOW_QUERY_LOGGER.error("Very slow count retrieval from sparse query {} ms {} ",tquery, URLDecoder.decode(props.toString(),"UTF-8"));
            } else if ( tquery > slowQueryThreshold ) {
              SLOW_QUERY_LOGGER.warn("Slow count retrieval from sparse query {} ms {} ",tquery, URLDecoder.decode(props.toString(),"UTF-8"));
            }
          } catch (UnsupportedEncodingException e) {
            LOGGER.debug(e.getLocalizedMessage(), e);
          }
          long additionalCount = 0;
          final Iterator<Content> countIterator = countItems.iterator();
          while (countIterator.hasNext()) {
            countIterator.next();
            additionalCount++;
          }
          count = countOffset + additionalCount;
        } else {
          count = offset + queryCount;
        }
      }
      finalResultSet = new SearchResultSetSizeWrapper(queryResultSet, count);
    }
    return finalResultSet;
  }

  /**
   * @param clause
   * @param clauseQuery
   * @param ands
   * @param ors
   */
  private void extractTerms(BooleanClause clause,
      org.apache.lucene.search.Query clauseQuery, Map<String, Object> ands,
      Map<String, Object> ors) {
    Set<Term> terms = Sets.newHashSet();
    clauseQuery.extractTerms(terms);

    for (Term term : terms) {
      if (clause != null && clause.getOccur() == Occur.SHOULD) {
        accumulateValue(ors, term.field(), term.text());
      } else {
        accumulateValue(ands, term.field(), term.text());
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void accumulateValue(Map<String, Object> map, String key, Object val) {
    Object o = map.get(key);
    if (o != null) {
      if (o instanceof Collection) {
        ((Collection) o).add(val);
      } else {
        List<Object> os = Lists.newArrayList(o, val);
        map.put(key, os);
      }
    } else {
      map.put(key, val);
    }
  }
}
