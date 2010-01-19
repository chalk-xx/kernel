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

package org.sakaiproject.kernel.discussion;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchException;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchResultSet;
import org.sakaiproject.kernel.api.search.SearchUtil;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Formats sakai/page nodes.
 * 
 * @scr.component immediate="true" label="DiscussionInitialPostSearchResultProcessor"
 *                description="Formatter for pages search results."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="DiscussionInitialPost"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class DiscussionInitialPostSearchResultProcessor implements
    SearchResultProcessor {

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    if (aggregator != null) {
      aggregator.add(node);
    }
    write.object();
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    if (node.hasProperty(DiscussionConstants.PROP_MARKER)) {
      write.key(DiscussionConstants.PROP_MARKER);
      write
          .value(node.getProperty(DiscussionConstants.PROP_MARKER).getString());
    }
    write.endObject();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return SearchUtil.getSearchResultSet(request, query);
  }
}