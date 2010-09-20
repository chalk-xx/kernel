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
package org.sakaiproject.nakamura.presence.search;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.search.SakaiSearchRowIterator;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@Component
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "UniqueAuthorizableProfile") })
@Service
public class UniqueAuthorizableProfileSearchResultProcessor implements SearchResultProcessor {
  private static final Logger logger = LoggerFactory
      .getLogger(UniqueAuthorizableProfileSearchResultProcessor.class);

  @Reference
  protected PresenceService presenceService;

  @Reference
  protected ProfileService profileService;

  @Reference
  protected SearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {

    try {
      // Get the query result.
      final QueryResult rs = query.execute();

      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);

      // filter the result set so that only 1 result is presented for each user found.
      HashSet<String> processedIds = new HashSet<String>();
      ArrayList<Row> filteredRows = new ArrayList<Row>();
      RowIterator rows = rs.getRows();
      while (rows.hasNext()) {
        Row row = rows.nextRow();
        Node node = row.getNode();

        Authorizable au = findAuthorizable(node, um);

        if (au != null && !processedIds.contains(au.getID())) {
          processedIds.add(au.getID());
          filteredRows.add(row);
        }
      }

      // Do the paging on the iterator.
      SakaiSearchRowIterator iterator = new SakaiSearchRowIterator(
          searchServiceFactory.getRowIteratorFromList(filteredRows));

      // Extract the total hits from lucene
      long start = SearchUtil.getPaging(request, filteredRows.size());
      iterator.skip(start);

      // Return the result set.
      int maxResults = (int) SearchUtil.longRequestParameter(request,
          SearchConstants.PARAM_MAX_RESULT_SET_COUNT,
          SearchConstants.DEFAULT_PAGED_ITEMS);
      SearchResultSet srs = searchServiceFactory.getSearchResultSet(iterator, maxResults);
      return srs;
    } catch (RepositoryException e) {
      logger.error("Unable to perform query.", e);
      throw new SearchException(500, "Unable to perform query.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#writeNode(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.Row)
   */
  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {

    write.object();
    Node node = row.getNode();
    Session session = node.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable au = findAuthorizable(node, um);
    if (au != null) {
      ValueMap map = profileService.getProfileMap(au, node.getSession());
      ((ExtendedJSONWriter)write).valueMapInternals(map);
      PresenceUtils.makePresenceJSON(write, au.getID(), presenceService, true);
    }
    write.endObject();

  }

  private Authorizable findAuthorizable(Node node, UserManager um)
      throws RepositoryException {
    String nodeName = node.getName();
    Authorizable au = um.getAuthorizable(nodeName);

    // try to get the user ID from the path if not found in previous step
    if (au == null) {
      nodeName = getUserFromPath(node.getPath());
      au = um.getAuthorizable(nodeName);
    }

    return au;
  }

  private String getUserFromPath(String path) {
    String[] pathParts = StringUtils.split(path, "/");

    if (pathParts != null && pathParts.length >= 4) {
      return pathParts[3];
    } else {
      return null;
    }
  }
}
