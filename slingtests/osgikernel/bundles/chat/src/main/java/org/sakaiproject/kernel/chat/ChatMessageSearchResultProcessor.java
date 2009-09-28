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
package org.sakaiproject.kernel.chat;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="ChatMessageSearchResultProcessor"
 *                description="Formatter for chat message search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="ChatMessage"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
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
  public void writeNode(JSONWriter write, Node resultNode) throws JSONException,
      RepositoryException {
    write.object();

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());

    // TODO : This should probably be using an Authorizable. However, updated
    // properties were not included in this..
    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {
      PersonalUtils.writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_TO, "userTo");
    }

    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
      PersonalUtils.writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_FROM, "userFrom");
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
}
