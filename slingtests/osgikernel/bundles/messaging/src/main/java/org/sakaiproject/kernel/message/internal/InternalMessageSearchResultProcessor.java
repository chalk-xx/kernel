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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="MessageSearchResultProcessor"
 *                description="Formatter for message search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Message"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class InternalMessageSearchResultProcessor implements
    SearchResultProcessor {

  private static final Logger LOG = LoggerFactory
      .getLogger(InternalMessageSearchResultProcessor.class);

  private UserManager userManager;

  public void output(JSONWriter write, QueryResult result, int nitems)
      throws RepositoryException, JSONException {
    NodeIterator resultNodes = result.getNodes();
    int i = 1;
    while (resultNodes.hasNext() && i <= nitems) {
      Node resultNode = resultNodes.nextNode();

      if (userManager == null) {
        Session session = resultNode.getSession();
        this.userManager = AccessControlUtil.getUserManager(session);
      }

      parseMessage(write, resultNode);
      i++;
    }

  }

  /**
   * Parses the message to a usable JSON format for the UI.
   * 
   * @param write
   * @param resultNode
   * @throws JSONException
   * @throws RepositoryException
   */
  private void parseMessage(JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {
    write.object();

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());
    write.key("path");
    write.value(messagingService.getMessagePathFromMessageStore(resultNode));

    if (resultNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {

      // TODO: Add the user info for this message.
      String to = resultNode.getProperty(MessageConstants.PROP_SAKAI_TO)
          .getString();
      write.key("userTo");
      Authorizable userAuthorizable = userManager.getAuthorizable(to);

      write.object();
    
      write.key("properties");
      write.array();
      Iterator iterator = userAuthorizable.getPropertyNames();
      while (iterator.hasNext()) {
        write.object();

        Object o = iterator.next();
        write.key(o.toString());
        Value[] values = userAuthorizable.getProperty(o.toString());
        write.array();

        for (Value val : values) {
          write.value(val.getString());
        }
        write.endArray();

        write.endObject();
      }
      write.endArray();
      write.endObject();

    }

    write.key("userFrom");
    write.value("{}");

    // List all of the properties on here.
    PropertyIterator pi = resultNode.getProperties();
    while (pi.hasNext()) {
      Property p = pi.nextProperty();
      if (p.getName().equalsIgnoreCase(
          MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE)) {
        write.key(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE);
        parsePreviousMessages(resultNode, write);

      } else {
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
    parseMessage(write, previousMessage);
  }

  private MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

}
