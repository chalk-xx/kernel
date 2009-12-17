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
package org.sakaiproject.kernel.message;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Row;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="MessageSearchResultProcessor"
 *                description="Procsessor for message search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Message"
 * @scr.property name="sakai.seach.resourcetype" value="sakai/message"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class MessageSearchResultProcessor implements SearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageSearchResultProcessor.class);

  protected MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  /**
   * Parses the message to a usable JSON format for the UI.
   * 
   * @param write
   * @param resultNode
   * @throws JSONException
   * @throws RepositoryException
   */
  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if ( aggregator != null ) {
      aggregator.add(resultNode);
    }
    writeNode(request, write, resultNode);
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {

    write.object();

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());

    // TODO : This should probably be using an Authorizable. However, updated
    // properties were not included in this..
    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {
      PersonalUtils.writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_TO,
          "userTo");
    }

    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
      PersonalUtils.writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_FROM,
          "userFrom");
    }

    // List all of the properties on here.
    PropertyIterator pi = resultNode.getProperties();
    while (pi.hasNext()) {
      Property p = pi.nextProperty();

      // If the path of a previous message is in here we go and retrieve that
      // node and parse it as well.
      if (p.getName().equalsIgnoreCase(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE)) {
        write.key(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE);
        parsePreviousMessages(request, write, resultNode);

      } else {
        // These are normal properties.., just parse them.
        write.key(p.getName());
        if (p.getDefinition().isMultiple()) {
          Value[] values = p.getValues();
          write.array();
          for (Value value : values) {
            write.value(value.getString());
          }
          write.endArray();
        } else {
          write.value(p.getString());
        }
      }
    }
    write.endObject();
  }

  /**
   * Parse a message we have replied on.
   * 
   * @param request
   * @param node
   * @param write
   * @param excerpt
   * @throws JSONException
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private void parsePreviousMessages(SlingHttpServletRequest request, JSONWriter write,
      Node node) throws JSONException, ValueFormatException, PathNotFoundException,
      RepositoryException {

    Session s = node.getSession();
    String id = node.getProperty(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE)
        .getString();
    String path = messagingService.getFullPathToMessage(s.getUserID(), id, s);
    /*
     * String path = messagingService.getMessageStorePathFromMessageNode(node) + "/" +
     * node.getProperty(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE).getString(); path =
     * PathUtils.normalizePath(path);
     */

    LOGGER.info("Getting message at {}", path);

    Node previousMessage = (Node) s.getItem(path);
    writeNode(request, write, previousMessage);
  }

}
