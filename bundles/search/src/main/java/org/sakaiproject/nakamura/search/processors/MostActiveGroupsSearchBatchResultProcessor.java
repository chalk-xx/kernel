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
package org.sakaiproject.nakamura.search.processors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.RowUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@Component(immediate = true, label = "MostActiveContentSearchBatchResultProcessor", description = "Formatter for most active content")
@Service(value = SearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "MostActiveGroups") })
public class MostActiveGroupsSearchBatchResultProcessor implements
    SearchBatchResultProcessor {

  @Reference
  private SearchServiceFactory searchServiceFactory;
  
  @Reference
  protected transient ProfileService profileService;

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
    Session session = request.getResourceResolver().adaptTo(Session.class);

    // count all the activity
    while (iterator.hasNext()) {
      Row row = iterator.nextRow();
      Node node = RowUtils.getNode(row, session);
      if (node.hasProperty("timestamp")) {
        Calendar timestamp = node.getProperty("timestamp").getDate();
        // TODO make the timespan of the report a parameter of the search
        Calendar thirtyDaysAgo = new GregorianCalendar();
        thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -30);
        if (timestamp.before(thirtyDaysAgo)) {
          // we stop counting once we get to the old stuff
          break;
        } else {
          String resourceId = node.getProperty("resourceId").getString();
          if (!resources.contains(new ResourceActivity(resourceId))) {
            UserManager um = AccessControlUtil.getUserManager(session);
            Authorizable au = um.getAuthorizable(resourceId);
            String resourcePath = profileService.getProfilePath(au);
            Node resourceNode = session.getNode(resourcePath);
            if (resourceNode == null) {
              // this happens if the group is not public
              continue;
            }
            String resourceName = resourceNode.getProperty("sakai:group-title").getString();
            resources.add(new ResourceActivity(resourceId, 0, resourceName));
          }
          // increment the count for this particular resource.
          resources.get(resources.indexOf(new ResourceActivity(resourceId))).activityScore++;
          
        }
      }
    }

    // write the most-used content to the JSONWriter
    Collections.sort(resources, Collections.reverseOrder());
    write.object();
    write.key("groups");
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
    private MostActiveGroupsSearchBatchResultProcessor getOuterType() {
      return MostActiveGroupsSearchBatchResultProcessor.this;
    }
  }

}
