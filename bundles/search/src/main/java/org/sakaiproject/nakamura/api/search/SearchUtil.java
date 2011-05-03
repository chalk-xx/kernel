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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.search.SearchServlet;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.query.Query;

/**
 *
 */
public class SearchUtil {

  public static final Logger LOGGER = LoggerFactory.getLogger(SearchUtil.class);




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
  public static long longRequestParameter(SlingHttpServletRequest request,
      String paramName, long defaultVal) {
    RequestParameter param = request.getRequestParameter(paramName);
    if (param != null) {
      try {
        return Integer.parseInt(param.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn(paramName + " parameter (" + param.getString()
            + ") is invalid; defaulting to " + defaultVal);
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
  public static long getPaging(SlingHttpServletRequest request) {

    long nitems = longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
        SearchConstants.DEFAULT_PAGED_ITEMS);
    long offset = longRequestParameter(request, PARAMS_PAGE, 0) * nitems;

    return offset;
  }

  /**
   * Assumes value is the value of a parameter in a where constraint and escapes it
   * according to the spec.
   *
   * @param value
   * @param queryLanguage
   *          The language to escape for. This can be XPATH, SQL, JCR_SQL2,
   *          JCR_JQOM, solr or sparse.
   *          Look at {@link Query Query}.
   *          For SOLR, this is nearly equivalent to ClientUtils.escapeQueryChars(value)
   *            but does not escape *, " or whitespace.
   * @return
   */
  @SuppressWarnings("deprecation") // Suppressed because we need to check depreciated methods just in case.
  public static String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)
          || queryLanguage.equals(Query.JCR_SQL2) || queryLanguage.equals(Query.JCR_JQOM)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'", "\\\\'")
            .replaceAll("'", "''").replaceAll("\"", "\\\\\"");
      } else if (queryLanguage.equals(org.sakaiproject.nakamura.api.search.solr.Query.SOLR)) {
//        escaped = ClientUtils.escapeQueryChars(value);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
          char c = value.charAt(i);
          // These characters are part of the query syntax and must be escaped
          if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')'
            || c == ':' || c == '^' || c == '[' || c == ']' || c == '{' || c == '}'
            || c == '~' || c == '?' || c == '|' || c == '&' || c == ';') {
            sb.append('\\');
          }
          sb.append(c);
        }
        escaped = sb.toString();
      } else if (queryLanguage.equals(org.sakaiproject.nakamura.api.search.solr.Query.SPARSE)) {
        escaped = value;
      } else {
        LOGGER.error("Unknown query language: " + queryLanguage);
      }
    }
    return escaped;
  }

  public static int getTraversalDepth(SlingHttpServletRequest req) {
    int maxRecursionLevels = 0;
    final String[] selectors = req.getRequestPathInfo().getSelectors();
    if (selectors != null && selectors.length > 0) {
      final String level = selectors[selectors.length - 1];
      if (!SearchServlet.TIDY.equals(level)) {
        if (SearchServlet.INFINITY.equals(level)) {
          maxRecursionLevels = -1;
        } else {
          try {
            maxRecursionLevels = Integer.parseInt(level);
          } catch (NumberFormatException nfe) {
            LOGGER.warn("Invalid recursion selector value '" + level
                + "'; defaulting to 0");
          }
        }
      }
    }
    return maxRecursionLevels;
  }

  /** Pattern to match on home paths. */
  private static Pattern homePathPattern = Pattern.compile("^(.*)(~([\\w-]*?))/");

  public static String expandHomeDirectory(String queryString) {
    Matcher homePathMatcher = homePathPattern.matcher(queryString);
    if (homePathMatcher.find()) {
      String username = homePathMatcher.group(3);
      String homePrefix = homePathMatcher.group(1);
      String userHome = LitePersonalUtils.getHomePath(username);
      userHome = ClientUtils.escapeQueryChars(userHome);
      String homePath = homePrefix + userHome + "/";
      String prefix = "";
      if (homePathMatcher.start() > 0) {
        prefix = queryString.substring(0, homePathMatcher.start());
      }
      String suffix = queryString.substring(homePathMatcher.end());
      queryString = prefix + homePath + suffix;
    }
    return queryString;
  }
}
