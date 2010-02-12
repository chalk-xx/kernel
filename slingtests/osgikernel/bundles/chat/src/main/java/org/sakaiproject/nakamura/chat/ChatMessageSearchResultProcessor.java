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
package org.sakaiproject.nakamura.chat;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.RowUtils;
import org.sakaiproject.nakamura.util.StringUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="ChatMessageSearchResultProcessor"
 *                description="Formatter for chat message search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="ChatMessage"
 * @scr.service interface="org.sakaiproject.nakamura.api.search.SearchResultProcessor"
 */
public class ChatMessageSearchResultProcessor implements SearchResultProcessor {

  /**
   * Parses the message to a usable JSON format for the UI. Once a message gets fetched it
   * automaticly gets marked as read.
   * 
   * @param write
   * @param resultNode
   * @throws JSONException
   * @throws RepositoryException
   */
  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    ResourceResolver r = request.getResourceResolver();
    Session session = r.adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if ( aggregator != null ) {
      aggregator.add(resultNode);
    }
    write.object();

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());

    // TODO : This should probably be using an Authorizable. However, updated
    // properties were not included in this..
    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {
      // This can either be chat:userid or just userid
      String to = resultNode.getProperty(MessageConstants.PROP_SAKAI_TO).getString();
      if (to.contains("chat:")) {
        String[] rcpts = StringUtils.split(to, ',');
        for (String rcpt : rcpts) {
          if (rcpt.startsWith("chat:")) {
            to = rcpt.substring(5);
            break;
          }
        }
      }
      PersonalUtils.writeUserInfo(resultNode.getSession(), to, write, "userTo");
    }

    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
    	String from = resultNode.getProperty(MessageConstants.PROP_SAKAI_FROM).getString();
      PersonalUtils.writeUserInfo(resultNode.getSession(), from, write, "userFrom");
    }

    // List all of the properties on here.
    PropertyIterator pi = resultNode.getProperties();
    while (pi.hasNext()) {
      Property p = pi.nextProperty();
      write.key(p.getName());
      write.value(p.getString());
    }

    write.endObject();

    // Check if this message has been read already.
    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_READ)
        && resultNode.getProperty(MessageConstants.PROP_SAKAI_READ).getBoolean() != true) {
      resultNode.setProperty(MessageConstants.PROP_SAKAI_READ, true);
      resultNode.save();
    }
  }
  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return SearchUtil.getSearchResultSet(request, query);
  }
}
