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
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@Component
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "User") })
@Service(value = SearchBatchResultProcessor.class)
public class UserSearchBatchResultProcessor implements SearchBatchResultProcessor {

  @Reference
  protected transient PresenceService presenceService;

  @Reference
  protected transient ProfileService profileService;


  @Reference
  protected transient SearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#writeNode(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.Row)
   */
  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator rows) throws JSONException, RepositoryException {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    UserManager um = AccessControlUtil.getUserManager(session);

    HashSet<String> processedIds = new HashSet<String>();

    while (rows.hasNext()) {
      Row row = rows.nextRow();
      Node node = row.getNode();
      String userID = node.getName();
      Authorizable au = um.getAuthorizable(userID);

      // try to get the user ID from the path if not found in previous step
      if (au == null) {
        userID = getUserFromPath(node.getPath());
        au = um.getAuthorizable(userID);
      }

      if (au != null && !processedIds.contains(userID)) {
        processedIds.add(userID);
        write.object();
        ValueMap map = profileService.getProfileMap(au, session);
        ((ExtendedJSONWriter) write).valueMapInternals(map);
        PresenceUtils.makePresenceJSON(write, userID, presenceService, true);
        write.endObject();
      }
    }
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
