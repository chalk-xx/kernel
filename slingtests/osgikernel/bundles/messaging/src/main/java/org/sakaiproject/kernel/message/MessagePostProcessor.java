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

import static org.sakaiproject.kernel.api.message.MessageConstants.BOX_OUTBOX;
import static org.sakaiproject.kernel.api.message.MessageConstants.EVENT_LOCATION;
import static org.sakaiproject.kernel.api.message.MessageConstants.PENDINGMESSAGE_EVENT;
import static org.sakaiproject.kernel.api.message.MessageConstants.PROP_SAKAI_MESSAGEBOX;
import static org.sakaiproject.kernel.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.kernel.api.message.MessageConstants.STATE_NONE;
import static org.sakaiproject.kernel.api.message.MessageConstants.STATE_NOTIFIED;
import static org.sakaiproject.kernel.api.message.MessageConstants.STATE_PENDING;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * 
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="MessagePostProcessor"
 *                description="Post Processor for Message operations" metatype="no"
 * @scr.property name="service.description" value="Post Processes message operations"
 * @scr.reference bind="bindEventAdmin" unbind="bindEventAdmin"
 *                interface="org.osgi.service.event.EventAdmin"
 * 
 */
public class MessagePostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessagePostProcessor.class);
  /**
   */
  private EventAdmin eventAdmin;

  /**
   * {@inheritDoc} This post processor is only interested in posts to messages, so it
   * should iterate rapidly through all messages.
   * 
   * @see org.apache.sling.servlets.post.SlingPostProcessor#process(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.List)
   */
  public void process(SlingHttpServletRequest request, List<Modification> changes)
      throws Exception {
    if (request.getAttribute(MessageConstants.MESSAGE_OPERATION) != null) {
      Map<Node, String> messageMap = new HashMap<Node, String>();
      Session s = request.getResourceResolver().adaptTo(Session.class);
      for (Modification m : changes) {
        try {
          switch (m.getType()) {
          case CREATE:
          case MODIFY:
            if (s.itemExists(m.getSource())) {
              Item item = s.getItem(m.getSource());
              if (item != null && item.isNode()) {
                Node n = (Node) item;
                if (n.hasProperty(PROP_SAKAI_MESSAGEBOX)) {
                  String box = n.getProperty(PROP_SAKAI_MESSAGEBOX).getString();
                  if (BOX_OUTBOX.equals(box)) {
                    String sendstate = STATE_NONE;
                    if (n.hasProperty(PROP_SAKAI_SENDSTATE)) {
                      sendstate = n.getProperty(PROP_SAKAI_SENDSTATE).getString();
                      messageMap.put(n, sendstate);
                    } else {
                      messageMap.put(n, sendstate);
                    }
                  }
                }
              }
            }
            break;
          }
        } catch (RepositoryException ex) {
          LOGGER.warn("Failed to process on create for {} ", m.getSource(), ex);
        }
      }

      for (Entry<Node, String> mm : messageMap.entrySet()) {
        Node n = mm.getKey();
        String state = mm.getValue();
        if (STATE_NONE.equals(state) || STATE_PENDING.equals(state)) {

          n.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);

          Dictionary<String, Object> messageDict = new Hashtable<String, Object>();
          messageDict.put(EVENT_LOCATION, n.getPath());
          eventAdmin.postEvent(new Event(PENDINGMESSAGE_EVENT, messageDict));
        }
      }
    }
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }
}
