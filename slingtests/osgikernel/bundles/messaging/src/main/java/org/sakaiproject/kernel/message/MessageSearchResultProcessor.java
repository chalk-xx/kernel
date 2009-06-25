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

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.personal.PersonalUtils;

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
   *          The json name that shouldbe used.
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void writeUserInfo(Node resultNode, JSONWriter write,
      String propertyName, String jsonName) throws ValueFormatException,
      PathNotFoundException, RepositoryException, JSONException {
    String user = resultNode.getProperty(propertyName).getString();
    write.key(jsonName);

    write.object();

    String path = PersonalUtils.getProfilePath(user);
    Node userNode = (Node) resultNode.getSession().getItem(path);

    PropertyIterator userPropertyIterator = userNode.getProperties();
    while (userPropertyIterator.hasNext()) {
      Property userProperty = userPropertyIterator.nextProperty();
      write.key(userProperty.getName());
      try {
        write.value(userProperty.getString());
      } catch (Exception ex) {
        Value[] vals = userProperty.getValues();
        write.array();
        for (Value val : vals) {
          write.value(val.getString());
        }
        write.endArray();
      }
    }

    write.endObject();
  }
  
  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  
}
