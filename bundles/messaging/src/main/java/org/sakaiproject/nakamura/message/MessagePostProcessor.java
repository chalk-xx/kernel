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
package org.sakaiproject.nakamura.message;

import static org.sakaiproject.nakamura.api.message.MessageConstants.BOX_OUTBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.EVENT_LOCATION;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PENDINGMESSAGE_EVENT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_MESSAGEBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NONE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NOTIFIED;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_PENDING;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

@Component(immediate = true, label = "MessagePostProcessor", description = "Post Processor for Message operations", metatype = false)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Processess changes on sakai/message nodes. If the messagebox and sendstate are set to respectively outbox and pending, a message will be copied to the recipients.") })
public class MessagePostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessagePostProcessor.class);

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc} This post processor is only interested in posts to messages,
   * so it should iterate rapidly through all messages.
   * 
   * @see org.apache.sling.servlets.post.SlingPostProcessor#process(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.List)
   */
  public void process(SlingHttpServletRequest request,
      List<Modification> changes) throws Exception {
    Map<Node, String> messageMap = new HashMap<Node, String>();
    Session s = request.getResourceResolver().adaptTo(Session.class);
    for (Modification m : changes) {
      try {
        switch (m.getType()) {
        case CREATE:
        case MODIFY:
          if (s.itemExists(m.getSource())) {
            Item item = s.getItem(getMessageFromModifcation(m));
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

    List<String> handledNodes = new ArrayList<String>();
    // Check if we have any nodes that have a pending state and launch an OSGi
    // event
    for (Entry<Node, String> mm : messageMap.entrySet()) {
      Node n = mm.getKey();
      String state = mm.getValue();
      if (!handledNodes.contains(n.getPath())) {
        if (STATE_NONE.equals(state) || STATE_PENDING.equals(state)) {

          n.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);

          Dictionary<String, Object> messageDict = new Hashtable<String, Object>();
          // WARNING
          // We can't pass in the node, because the session might expire before the event gets handled
          // This does mean that the listener will have to get the node each time, and probably create a new session for each message
          // This might be heavy on performance.
          messageDict.put(EVENT_LOCATION, n.getPath());
          messageDict.put("user", request.getRemoteUser());
          LOGGER.info("Launched event for node: " + n.getPath());
          Event pendingMessageEvent = new Event(PENDINGMESSAGE_EVENT,
              messageDict);
          // Initiate an asynchronous event.
          eventAdmin.postEvent(pendingMessageEvent);
          handledNodes.add(n.getPath());
        }
      }
    }
  }

  /**
   * Gets the node for a modification.
   * 
   * @param m
   * @return
   */
  private String getMessageFromModifcation(Modification m) {
    String path = m.getSource();
    path = path.substring(0, path.lastIndexOf("/"));
    return path;
  }
}
