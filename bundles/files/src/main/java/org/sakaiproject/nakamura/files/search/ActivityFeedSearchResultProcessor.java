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
package org.sakaiproject.nakamura.files.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Stack;

/**
 * Returns results from the activity feed search in reverse order
 */
@Component(immediate = true, metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_PROCESSOR_NAMES, value = "Resource"),
    @Property(name = "ActivityFeed", boolValue = true) })
@Service
public class ActivityFeedSearchResultProcessor implements SolrSearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ActivityFeedSearchResultProcessor.class);
  @Reference
  private SolrSearchServiceFactory searchServiceFactory;
  
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      String queryString) throws SolrSearchException {
    LOGGER.info("returning search results for the activity feed.");
    final SolrSearchResultSet resultSet = searchServiceFactory.getSearchResultSet(request, queryString);
    // return the results in reverse order
    // TODO BL120 we're only doing this because sort is not working on the solr search
    return new SolrSearchResultSet() {

      public Iterator<Result> getResultSetIterator() {
        return new Iterator<Result>() {
          private Stack<Result> stack;
          {
            stack = new Stack<Result>();
            Iterator<Result> resultIterator = resultSet.getResultSetIterator();
            while (resultIterator.hasNext()) {
              stack.push(resultIterator.next());
            }
          }

          public boolean hasNext() {
            return !stack.isEmpty();
          }

          public Result next() {
            return stack.pop();
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }};
      }

      public long getSize() {
        return resultSet.getSize();
      }};
  }
  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result)
      throws JSONException {
    String contentPath = (String) result.getFirstValue("path");
    Session session =
      StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    try {
      Content contentResult = session.getContentManager().get(contentPath);
      ExtendedJSONWriter.writeContentTreeToWriter(write, contentResult, -1);
    } catch (Exception e) {
      throw new JSONException(e);
    }
  }

}
