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
package org.sakaiproject.nakamura.connections.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.LiteProfileService;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.connections.ConnectionUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Formats connection search results. We get profile nodes from the query and make a
 * uniformed result.
 */
@Component(description = "Formatter for connection search results", label = "ConnectionFinderSearchResultProcessor")
@Properties({ @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.processor", value = "ConnectionFinder") })
@Service
public class ConnectionFinderSearchResultProcessor implements SolrSearchResultProcessor {

  private static final Logger logger = LoggerFactory
      .getLogger(ConnectionFinderSearchResultProcessor.class);

  @Reference
  LiteProfileService profileService;

  @Reference
  SolrSearchServiceFactory searchServiceFactory;

  public void writeResult(SlingHttpServletRequest request, JSONWriter writer, Result result)
      throws JSONException {
    Map<String, Collection<Object>> props = result.getProperties();
    Collection<Object> names = props.get(User.NAME_FIELD);
    if (names == null || names.size() == 0) {
      throw new IllegalArgumentException("Missing " + User.NAME_FIELD);
    }

    String user = request.getRemoteUser();
    String contactUser = (String) names.iterator().next();

    ResourceResolver resolver = request.getResourceResolver();
    Session session = resolver.adaptTo(Session.class);
    try {
      AuthorizableManager authMgr = session.getAuthorizableManager();
      Authorizable auth = authMgr.findAuthorizable(contactUser);

      String contactContentPath = ConnectionUtils.getConnectionPath(user, contactUser);
      logger.debug("getting " + contactContentPath);
      Content contactContent = resolver.getResource(contactContentPath).adaptTo(Content.class);

      int maxTraversalDepth = SearchUtil.getTraversalDepth(request);

      writer.object();
      writer.key("target");
      writer.value(contactUser);
      writer.key("profile");
      ExtendedJSONWriter.writeValueMap(writer, profileService.getCompactProfileMap(auth));
      writer.key("details");
      ExtendedJSONWriter.writeContentTreeToWriter(writer, contactContent, maxTraversalDepth);
      writer.endObject();
    } catch (StorageClientException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      String query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }
}
