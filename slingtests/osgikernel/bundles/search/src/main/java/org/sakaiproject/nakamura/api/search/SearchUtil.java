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
package org.sakaiproject.nakamura.api.search;

import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_ITEMS_PER_PAGE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_PAGE;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.jackrabbit.server.index.QueryHitsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class SearchUtil {

  public static final Logger LOGGER = LoggerFactory.getLogger(SearchUtil.class);

  /**
   * This method will return a SearchResultSet that contains a paged rowIterator and the
   * total hit count from Lucene.
   * 
   * @param request
   * @param query
   * @return
   * @throws SearchException
   */
  public static SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    try {
      // Get the query result.
      QueryResult rs = query.execute();

      // Extract the total hits from lucene
      long hits = getHits(rs);

      // Do the paging on the iterator.
      RowIterator iterator = rs.getRows();
      long start = getPaging(request, hits);
      iterator.skip(start);

      // Return the result set.
      SearchResultSet srs = new AbstractSearchResultSet(iterator, hits);
      return srs;
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to perform query.");
    }

  }

  /**
   * Get the hits from a Lucene queryResult.
   * 
   * @param rs
   * @return
   */
  public static long getHits(QueryResult rs) throws SearchException {
    QueryHitsExtractor extr = new QueryHitsExtractor(rs);
    return extr.getHits();
  }

  /**
   * Check for an integer value in the request.
   * 
   * @param request
   *          The request to look in.
   * @param paramName
   *          The name of the parameter that holds the integer value.
   * @param defaultVal
   *          The default value in case the parameter is not found or is not an integer
   * @return The long value.
   */
  public static long intRequestParameter(SlingHttpServletRequest request,
      String paramName, long defaultVal) {
    RequestParameter param = request.getRequestParameter(paramName);
    if (param != null) {
      try {
        return Integer.parseInt(param.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn(paramName + "parameter (" + param.getString()
            + ") is invalid defaulting to " + defaultVal + " items ", e);
      }
    }
    return defaultVal;
  }

  /**
   * Get the starting point.
   * 
   * @param request
   * @param total
   * @return
   */
  public static long getPaging(SlingHttpServletRequest request, long total) {

    long nitems = intRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
        SearchConstants.DEFAULT_PAGED_ITEMS);
    long offset = intRequestParameter(request, PARAMS_PAGE, 0) * nitems;

    if (total < 0) {
      total = Long.MAX_VALUE;
    }
    long start = Math.min(offset, total);
    return start;
  }

  /**
   * Assumes value is the value of a parameter in a where constraint and escapes it
   * according to the spec.
   * 
   * @param value
   * @param queryLanguage
   *          The language to escape for. This can be XPATH, SQL, JCR_SQL2 or JCR_JQOM.
   *          Look at {@link Query Query}.
   * @return
   */
  public static String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)
          || queryLanguage.equals(Query.JCR_SQL2) || queryLanguage.equals(Query.JCR_JQOM)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'", "\\\\'")
            .replaceAll("'", "''").replaceAll("\"", "\\\\\"");
      } else {
        LOGGER.error("Unknown query language: " + queryLanguage);
      }
    }
    return escaped;
  }
}
