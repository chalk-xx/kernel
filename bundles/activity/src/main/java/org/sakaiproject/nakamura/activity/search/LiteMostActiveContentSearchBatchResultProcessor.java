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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

@Component(immediate = true, label = "MostActiveContentSearchBatchResultProcessor", description = "Formatter for most active content")
@Service(value = SolrSearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "LiteMostActiveContent") })
public class LiteMostActiveContentSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  private static final Logger LOG = LoggerFactory
      .getLogger(LiteMostActiveContentSearchBatchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  private static final int DEFAULT_DAYS = 30;
  private static final int MAXIMUM_DAYS = 90;

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {
    final List<ResourceActivity> resources = new ArrayList<ResourceActivity>();
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));

    final int daysAgo = deriveDateWindow(request);

    // count all the activity
    LOG.debug("Computing the most active content feed.");
    while (iterator.hasNext()) {
      try {
        final Result result = iterator.next();
        final String path = result.getPath();
        final Content node = session.getContentManager().get(path);
        if (node.hasProperty("timestamp")) {
          Calendar timestamp = (Calendar) node.getProperty("timestamp");
          Calendar specifiedDaysAgo = new GregorianCalendar();
          specifiedDaysAgo.add(Calendar.DAY_OF_MONTH, -daysAgo);
          if (timestamp.before(specifiedDaysAgo)) {
            // we stop counting once we get to the old stuff
            break;
          } else {
            String resourceId = (String) node.getProperty("resourceId");
            if (!resources.contains(new ResourceActivity(resourceId))) {
              Content resourceNode = session.getContentManager().get(resourceId);
              if (resourceNode == null) {
                // this can happen if this content is no longer public
                continue;
              }
              String resourceName = (String) resourceNode
                  .getProperty("sakai:pooled-content-file-name");
              resources.add(new ResourceActivity(resourceId, 0, resourceName));
            }
            // increment the count for this particular resource.
            resources.get(resources.indexOf(new ResourceActivity(resourceId))).activityScore++;

          }
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

    // write the most-used content to the JSONWriter
    Collections.sort(resources, Collections.reverseOrder());
    write.object();
    write.key(SolrSearchConstants.TOTAL);
    write.value(resources.size());
    write.key("content");
    write.array();
    for (ResourceActivity resourceActivity : resources) {
      write.object();
      write.key("id");
      write.value(resourceActivity.id);
      write.key("name");
      write.value(resourceActivity.name);
      write.key("count");
      write.value(Long.valueOf(resourceActivity.activityScore));
      write.endObject();
    }
    write.endArray();
    write.endObject();
  }

  private int deriveDateWindow(SlingHttpServletRequest request) {
    int daysAgo = DEFAULT_DAYS;
    String requestedDaysParam = request.getParameter("days");
    if (requestedDaysParam != null) {
      try {
        int requestedDays = Integer.parseInt(requestedDaysParam);
        if ((requestedDays > 0) && (requestedDays <= MAXIMUM_DAYS)) {
          daysAgo = requestedDays;
        }
      } catch (NumberFormatException e) {
        // malformed parameter, so we'll just stick with the default number of days
      }
    }
    return daysAgo;
  }

  public class ResourceActivity implements Comparable<ResourceActivity> {
    public String id;

    public ResourceActivity(String id) {
      this.id = id;
    }

    public ResourceActivity(String id, int activityScore, String name) {
      this.id = id;
      this.activityScore = activityScore;
      this.name = name;
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

    public String name;
    public int activityScore;

    public int compareTo(ResourceActivity other) {
      return Integer.valueOf(this.activityScore).compareTo(
          Integer.valueOf(other.activityScore));
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
