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
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.ResultSetFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import java.util.Collection;
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
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SparseResultSetFactory.class);
  private static final Logger SLOW_QUERY_LOGGER = LoggerFactory.getLogger(SlowQueryLogger.class);

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
    }
    SolrSearchResultSet rs = new SparseSearchResultSet(items, defaultMaxResults);
    return rs;
    } catch (AccessDeniedException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (StorageClientException e) {
      throw new SolrSearchException(500, e.getMessage());
    } catch (ParseException e) {
      throw new SolrSearchException(500, e.getMessage());
    }
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
