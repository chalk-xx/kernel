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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(immediate = true, label = "MostActiveContentSearchBatchResultProcessor", description = "Formatter for most active content")
@Service(value = SolrSearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "LiteMostActiveContent") })
public class LiteMostActiveContentSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  public static final String STARTPAGE_PARAM = "startpage";
  public static final String NUMITEMS_PARAM = "numitems";

  private static final Logger LOG = LoggerFactory
      .getLogger(LiteMostActiveContentSearchBatchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {
    final Map<String, ResourceActivity> resources = new HashMap<String, ResourceActivity>();
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));

    // count all the activity
    LOG.debug("Computing the most active content feed.");
    while (iterator.hasNext()) {
      try {
        final Result result = iterator.next();
        final String path = result.getPath();
        final Content node = session.getContentManager().get(path);
        if (node != null) {
          final String resourceId = (String) node.getProperty("resourceId");
          if (!resources.containsKey(resourceId)) {
            final Content resourceNode = session.getContentManager().get(resourceId);
            if (resourceNode == null) {
              // this can happen if this content is no longer public
              continue;
            }
            final String resourceName = (String) resourceNode
                .getProperty(FilesConstants.POOLED_CONTENT_FILENAME);
            resources.put(resourceId, new ResourceActivity(resourceId, 0, resourceName,
                (Long) resourceNode.getProperty(FilesConstants.LAST_MODIFIED)));
          }
          // increment the count for this particular resource.
          resources.get(resourceId).activityScore++;
        }
      } catch (StorageClientException e) {
        // if something is wrong with this particular resourceNode,
        // we don't let it wreck the whole feed
        continue;
      } catch (AccessDeniedException e) {
        // if something is wrong with this particular resourceNode,
        // we don't let it wreck the whole feed
        continue;
      }
    }
    
    // KERN-1724 determine how many content items the current user can read
    long totalCanRead = 0L;
    try {
      final String queryString = "resourceType:"
          + ClientUtils.escapeQueryChars(FilesConstants.POOLED_CONTENT_RT);
      final Query query = new Query(queryString);
      final SolrSearchResultSet rs = searchServiceFactory.getSearchResultSet(request,
          query);
      if (rs != null) {
        totalCanRead = rs.getSize();
      }
    } catch (SolrSearchException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }

    // write the most-used content to the JSONWriter
    final List<ResourceActivity> resourceActivities = new ArrayList<ResourceActivity>(
        resources.values());
    Collections.sort(resourceActivities, Collections.reverseOrder());
    write.object();
    write.key("totalCanRead");
    write.value(totalCanRead);
    write.key(SolrSearchConstants.TOTAL);
    write.value(resources.size());
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
    write.key("content");
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

    private LiteMostActiveContentSearchBatchResultProcessor getOuterType() {
      return LiteMostActiveContentSearchBatchResultProcessor.this;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.sakaiproject.nakamura.api.search.solr.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {

    return searchServiceFactory.getSearchResultSet(request, query);
  }

}
