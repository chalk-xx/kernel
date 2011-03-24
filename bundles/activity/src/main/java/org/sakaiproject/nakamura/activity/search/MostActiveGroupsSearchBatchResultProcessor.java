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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(immediate = true, label = "MostActiveGroupSearchBatchResultProcessor", description = "Formatter for most active groups")
@Service
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "MostActiveGroups") })
public class MostActiveGroupsSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  public static final String STARTPAGE_PARAM = "startpage";
  public static final String NUMITEMS_PARAM = "numitems";

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> results) throws JSONException {
    final Map<String, ResourceActivity> resources = new HashMap<String, ResourceActivity>();
    final ResourceResolver resolver = request.getResourceResolver();
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    while (results.hasNext()) {
      final Result result = results.next();
      final String path = result.getPath();
      final Resource resource = resolver.getResource(path);
      final Content content = resource.adaptTo(Content.class);
      if (content != null) {
        final String resourceId = (String) content.getProperty("resourceId");
        if (!resources.containsKey(resourceId)) {
          final String resourcePath = LitePersonalUtils.getProfilePath(resourceId);
          Content resourceContent = null;
          try {
            resourceContent = session.getContentManager().get(resourcePath);
          } catch (Exception e) {
            // this happens if the group is not public
            // or if the group path simply doesn't exist
            continue;
          }
          final String resourceName = (String) resourceContent
              .getProperty("sakai:group-title");
          resources.put(resourceId, new ResourceActivity(resourceId, 0, resourceName,
              (Long) resourceContent.getProperty(FilesConstants.LAST_MODIFIED)));
        }
        // increment the count for this particular resource.
        resources.get(resourceId).activityScore++;
      }
    }

    // write the most-used content to the JSONWriter
    final List<ResourceActivity> resourceActivities = new ArrayList<ResourceActivity>(
        resources.values());
    Collections.sort(resourceActivities, Collections.reverseOrder());
    write.object();
    write.key(SolrSearchConstants.TOTAL);
    write.value(resourceActivities.size());
    final RequestParameter startpageP = request.getRequestParameter(STARTPAGE_PARAM);
    int startpage = (startpageP != null) ? Integer.valueOf(startpageP.getString()) : 1;
    startpage = (startpage < 1) ? 1 : startpage;
    write.key(STARTPAGE_PARAM);
    write.value(startpage);
    final RequestParameter numitemsP = request.getRequestParameter(NUMITEMS_PARAM);
    int numitems = (numitemsP != null) ? Integer.valueOf(numitemsP.getString())
                                      : SolrSearchConstants.DEFAULT_PAGED_ITEMS;
    numitems = (numitems < 1) ? SolrSearchConstants.DEFAULT_PAGED_ITEMS : numitems;
    write.key(NUMITEMS_PARAM);
    write.value(numitems);
    final int beginPosition = (startpage * numitems) - numitems;
    write.key("groups");
    write.array();
    if (beginPosition < resourceActivities.size()) {
      int count = 0;
      for (int i = beginPosition; i < resourceActivities.size() && count < numitems; i++) {
        final ResourceActivity resourceActivity = resourceActivities.get(i);
        write.object();
        write.key("id");
        write.value(resourceActivity.id);
        write.key("name");
        write.value(resourceActivity.name);
        write.key("count");
        write.value(Long.valueOf(resourceActivity.activityScore));
        write.endObject();
        count++;
      }
    }
    write.endArray();
    write.endObject();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    // Return the result set.
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  public class ResourceActivity implements Comparable<ResourceActivity> {
    public final String id;
    public final String name;
    public final Long lastModified;
    public Integer activityScore;

    public ResourceActivity(String id, int activityScore, String name, long lastModified) {
      this.id = id;
      this.activityScore = activityScore;
      this.name = name;
      this.lastModified = lastModified;
    }

    @Override
    public String toString() {
      return "ResourceActivity(" + id + ", " + activityScore + ", " + name + ", "
          + lastModified + ")";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof ResourceActivity))
        return false;
      ResourceActivity other = (ResourceActivity) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      return true;
    }

    public int compareTo(ResourceActivity other) {
      if (this.activityScore.equals(other.activityScore)) {
        return this.lastModified.compareTo(other.lastModified);
      } else {
        return this.activityScore.compareTo(other.activityScore);
      }
    }

    private MostActiveGroupsSearchBatchResultProcessor getOuterType() {
      return MostActiveGroupsSearchBatchResultProcessor.this;
    }
  }
}
