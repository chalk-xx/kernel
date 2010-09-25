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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_HOME_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_HOME_RESOURCE_TYPE;

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
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
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

      Session session = request.getResourceResolver().adaptTo(Session.class);

      // filter the result set so that only 1 result is presented for each user found.
      HashSet<String> processedIds = new HashSet<String>();
      ArrayList<Row> filteredRows = new ArrayList<Row>();
      RowIterator rows = rs.getRows();
      while (rows.hasNext()) {
        Row row = rows.nextRow();
        Node node = row.getNode();

        try {
          Node homeNode = getHomeNode(node);
//        String[] pathParts = StringUtils.split(node.getPath(), "/", 5);
//
//        StringBuilder profilePathBuilder = new StringBuilder();
//        for (int i = 0; i < 4; i++) {
//          profilePathBuilder.append("/").append(pathParts[i]);
//        }
//        profilePathBuilder.append("/public/authprofile");
//        String profilePath = profilePathBuilder.toString();
          String homePath = homeNode.getPath();
          String profilePath = homePath + "/public/authprofile";
          if (!processedIds.contains(profilePath)) {
            try {
              Node profile = session.getNode(profilePath);
              filteredRows.add(node2Row(profile));
              processedIds.add(profilePath);
            } catch (RepositoryException e) {
              logger.warn("Unable to retrieve path: {}", profilePath);
            }
          }
        } catch (RepositoryException e) {
          logger.warn("Unable to find profile node in hierarchy {}", node.getPath());
        }
      }

      // Do the paging on the iterator.
      RowIterator iterator = searchServiceFactory.getRowIteratorFromList(filteredRows);
      int totalHits = filteredRows.size();

      // Extract the total hits from lucene
      long start = SearchUtil.getPaging(request, totalHits);
      iterator.skip(start);

      // Return the result set.
      SearchResultSet srs = searchServiceFactory.getSearchResultSet(iterator, totalHits);
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
    ValueMap map = profileService.getProfileMap(node);
    ((ExtendedJSONWriter)write).valueMapInternals(map);
    PresenceUtils.makePresenceJSON(write, node.getProperty("rep:userId").getString(),
        presenceService, true);
    write.endObject();

  }

  private Node getHomeNode(Node node) throws RepositoryException {
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)
        && (USER_HOME_RESOURCE_TYPE.equals(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString())
        || GROUP_HOME_RESOURCE_TYPE.equals(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString()))) {
      return node;
    } else {
      return getHomeNode(node.getParent());
    }
  }

  private Row node2Row(final Node node) {
    Row row = new Row() {

      public Value[] getValues() throws RepositoryException {
        return null;
      }

      public Value getValue(String propertyName) throws ItemNotFoundException,
          RepositoryException {
        return node.getProperty(propertyName).getValue();
      }

      public Node getNode() throws RepositoryException {
        return node;
      }

      public Node getNode(String arg0) throws RepositoryException {
        return node;
      }

      public String getPath() throws RepositoryException {
        return node.getPath();
      }

      public String getPath(String arg0) throws RepositoryException {
        return node.getPath();
      }

      public double getScore() throws RepositoryException {
        return -1;
      }

      public double getScore(String arg0) throws RepositoryException {
        return -1;
      }
    };
    return row;
  }
}
