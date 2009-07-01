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
package org.sakaiproject.kernel.message.internal;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageSearchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.message.MessageUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="InternalMessageSearchResultProcessor"
 *                description="Procsessor for internalmessage search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="InternalMessage"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class InternalMessageSearchResultProcessor extends
    MessageSearchResultProcessor implements SearchResultProcessor {

  private static final Logger LOG = LoggerFactory
      .getLogger(InternalMessageSearchResultProcessor.class);

  /**
   * Parses the message to a usable JSON format for the UI.
   * 
   * @param write
   * @param resultNode
   * @throws JSONException
   * @throws RepositoryException
   */
  public void writeNode(JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {
    write.object();

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());
    write.key("path");
    write.value(MessageUtils.getMessageUrl(resultNode.getName()));

    // TODO : This should probably be using an Authorizable. However, updated
    // properties were not included in this..
    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {
      writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_TO, "userTo");
    }

    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
      writeUserInfo(resultNode, write, MessageConstants.PROP_SAKAI_FROM,
          "userFrom");
    }

    // List all of the properties on here.
    PropertyIterator pi = resultNode.getProperties();
    while (pi.hasNext()) {
      Property p = pi.nextProperty();

      // If the path of a previous message is in here we go and retrieve that
      // node and parse it as well.
      if (p.getName().equalsIgnoreCase(
          MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE)) {
        write.key(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE);
        parsePreviousMessages(resultNode, write);

      } else {
        // These are normal properties.., just parse them.
        write.key(p.getName());
        write.value(p.getString());
      }
    }
    write.endObject();
  }

  /**
   * Parses a message we have replied one.
   * 
   * @param node
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  private void parsePreviousMessages(Node node, JSONWriter write)
      throws JSONException, ValueFormatException, PathNotFoundException,
      RepositoryException {

    String path = messagingService.getMessageStorePathFromMessageNode(node)
        + node.getProperty(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE)
            .getString();
    path = PathUtils.normalizePath(path);

    LOG.info("Getting message at {}", path);

    Session s = node.getSession();
    Node previousMessage = (Node) s.getItem(path);
    writeNode(write, previousMessage);
  }
}
