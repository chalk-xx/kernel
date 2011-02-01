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

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.search.*;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.*;

@Component(immediate = true, label = "MostActiveContentSearchBatchResultProcessor", description = "Formatter for most active content")
@Service(value = SearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "MostActiveContent") })
public class MostActiveContentSearchBatchResultProcessor implements
    SearchBatchResultProcessor {

  private Logger logger = LoggerFactory.getLogger(MostActiveContentSearchBatchResultProcessor.class);

  @Reference
  private SearchServiceFactory searchServiceFactory;

  private final int DEFAULT_DAYS = 30;
  private final int MAXIMUM_DAYS = 90;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor#writeNodes(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.RowIterator)
   */
  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {
    List<ResourceActivity> resources = new ArrayList<ResourceActivity>();
    ResourceResolver resourceResolver = request.getResourceResolver();
    Session session = resourceResolver.adaptTo(Session.class);

    int daysAgo = deriveDateWindow(request);

    // count all the activity
    logger.info("Computing the most active content feed.");
    while (iterator.hasNext()) {
      try {
        Row row = iterator.nextRow();
        Node node = RowUtils.getNode(row, session);
        if (node.hasProperty("timestamp")) {
          Calendar timestamp = node.getProperty("timestamp").getDate();
          Calendar specifiedDaysAgo = new GregorianCalendar();
          specifiedDaysAgo.add(Calendar.DAY_OF_MONTH, -daysAgo);
          if (timestamp.before(specifiedDaysAgo)) {
            // we stop counting once we get to the old stuff
            break;
          } else {
            String resourceId = node.getProperty("resourceId").getString();
            if (!resources.contains(new ResourceActivity(resourceId))) {
              Node resourceNode = FileUtils.resolveNode(resourceId, resourceResolver);
              if (resourceNode == null) {
                // this can happen if this content is no longer public
                continue;
              }
              String resourceName = resourceNode.getProperty("sakai:pooled-content-file-name").getString();
              resources.add(new ResourceActivity(resourceId, 0, resourceName));
            }
            // increment the count for this particular resource.
            resources.get(resources.indexOf(new ResourceActivity(resourceId))).activityScore++;

          }
        }
      } catch (RepositoryException e) {
        // if something is wrong with this particular resourceNode,
        // we don't let it wreck the whole feed
        continue;
      }
    }

    // write the most-used content to the JSONWriter
    Collections.sort(resources, Collections.reverseOrder());
    write.object();
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

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    try {
      // Perform the query
      QueryResult qr = query.execute();
      RowIterator iterator = qr.getRows();

      // Return the result set.
      return searchServiceFactory.getSearchResultSet(iterator);
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to execute query.");
    }
  }

  public class ResourceActivity implements Comparable<ResourceActivity>{
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
      return Integer.valueOf(this.activityScore).compareTo(Integer.valueOf(other.activityScore));
    }
    private MostActiveContentSearchBatchResultProcessor getOuterType() {
      return MostActiveContentSearchBatchResultProcessor.this;
    }
  }

}
