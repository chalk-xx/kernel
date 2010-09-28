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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
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
import org.sakaiproject.nakamura.api.search.UniquePathRowIterator;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "UniqueProfile") })
@Service
public class UniqueProfileSearchResultProcessor implements SearchResultProcessor {
  private static final Logger logger = LoggerFactory
      .getLogger(UniqueProfileSearchResultProcessor.class);

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

      RowIterator rows = rs.getRows();

      // Get just the home node for each result found
      RowIterator homeRowIter = new AuthorizableHomeRowIterator(rows);

      // filter the result set so that only 1 result is presented for each user/path found.
      RowIterator uniqPathIter = new UniquePathRowIterator(homeRowIter);

      // Extract the total hits from lucene
      long start = SearchUtil.getPaging(request);
      uniqPathIter.skip(start);

      // Return the result set.
      SearchResultSet srs = searchServiceFactory.getSearchResultSet(uniqPathIter);
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

    Node homeNode = row.getNode();
    String profilePath = homeNode.getPath() + "/public/authprofile";
    Session session = homeNode.getSession();
    Node profileNode = session.getNode(profilePath);
    write.object();
    ValueMap map = profileService.getProfileMap(profileNode);
    ((ExtendedJSONWriter)write).valueMapInternals(map);

    // If this is a User Profile, then include Presence data.
    if (profileNode.hasProperty("rep:userId")) {
      PresenceUtils.makePresenceJSON(write, profileNode.getProperty("rep:userId")
          .getString(), presenceService, true);
    }

    write.endObject();
  }
}
