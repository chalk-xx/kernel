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
package org.sakaiproject.nakamura.activity.search;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Component(metatype = true)
@Service
@Properties({
  @Property(name = "service.vendor", value = "The Sakai Foundation"),
  @Property(name = SolrSearchConstants.REG_PROCESSOR_NAMES, value = "ActivityFeed")
})
public class ActivityFeedSearchResultProcessor implements SolrSearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActivityFeedSearchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  @Reference
  private BasicUserInfoService basicUserInfoService;

  public ActivityFeedSearchResultProcessor() {
  }

  ActivityFeedSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory) {
    if (searchServiceFactory == null) {
      throw new IllegalArgumentException(
          "Search Service Factory must be set when not using as a component");
    }
    this.searchServiceFactory = searchServiceFactory;
  }

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result)
      throws JSONException {
    String contentPath = result.getPath();
    Session session =
      StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    try {
      Content contentResult = session.getContentManager().get(contentPath);
      write.object();
      if (contentResult != null) {
        ExtendedJSONWriter.writeContentTreeToWriter(write, contentResult, true, -1);
      }
      String actor = String.valueOf(contentResult.getProperty(ActivityConstants.PARAM_ACTOR_ID));
      if (!StringUtils.isBlank(actor)) {
        Authorizable actorAuth = session.getAuthorizableManager().findAuthorizable(actor);
        if (actorAuth != null) {
          ValueMap profile = new ValueMapDecorator(basicUserInfoService.getProperties(actorAuth));
          ExtendedJSONWriter.writeValueMapInternals(write, profile);
        } else {
          LOGGER.warn("Unable to find actor for activity [{}]", contentPath);
        }
      } else {
        LOGGER.warn("Unable to find actor in activity [{}]", contentPath);
      }
      write.endObject();
    } catch (Exception e) {
      throw new JSONException(e);
    }
  }
}
