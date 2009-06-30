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
package org.sakaiproject.kernel.api.message;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * 
 * 
 * @scr.component immediate="true" label="MessageSearchResultProcessor"
 *                description="Formatter for message search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class MessageSearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageSearchResultProcessor.class);

  protected MessagingService messagingService;

  /**
   * Writes userinfo out for a property in a node. Make sure that the resultNode
   * has a property with propertyName that contains a userid.
   * 
   * @param resultNode
   *          The node to look on
   * @param write
   *          The writer to write to.
   * @param propertyName
   *          The propertyname that contains the userid.
   * @param jsonName
   *          The json name that should be used.
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void writeUserInfo(Node resultNode, JSONWriter write,
      String propertyName, String jsonName) throws ValueFormatException,
      PathNotFoundException, RepositoryException, JSONException {

    try {
      String user = resultNode.getProperty(propertyName).getString();

      String path = PersonalUtils.getProfilePath(user);
      Node userNode = (Node) resultNode.getSession().getItem(path);

      PropertyIterator userPropertyIterator = userNode.getProperties();
      Map<String, Object> mapPropertiesToWrite = new HashMap<String, Object>();

      while (userPropertyIterator.hasNext()) {
        Property userProperty = userPropertyIterator.nextProperty();
        try {
          mapPropertiesToWrite.put(userProperty.getName(), userProperty
              .getValue());
        } catch (ValueFormatException ex) {
          mapPropertiesToWrite.put(userProperty.getName(), userProperty
              .getValues());
        }
      }

      // We can't have anymore exceptions from now on.
      write.key(jsonName);
      write.object();
      for (Entry<String, Object> e : mapPropertiesToWrite.entrySet()) {
        write.key(e.getKey());
        if (e.getValue() instanceof Value) {
          write.value(((Value) e.getValue()).getString());
        } else {
          write.array();

          Value[] vals = (Value[]) e.getValue();
          for (Value v : vals) {
            write.value(v.getString());
          }

          write.endArray();
        }
      }
      write.endObject();

    } catch (PathNotFoundException pnfe) {
      LOGGER.warn("Profile path not found for this user.");
    } catch (Exception ex) {
      LOGGER.warn(ex.getMessage());
    }
  }

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

}
