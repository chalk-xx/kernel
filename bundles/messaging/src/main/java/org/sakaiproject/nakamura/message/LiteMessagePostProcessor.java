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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.message.MessageConstants.BOX_OUTBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.EVENT_LOCATION;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PENDINGMESSAGE_EVENT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_MESSAGEBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.SAKAI_MESSAGE_RT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NONE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NOTIFIED;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_PENDING;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.api.resource.lite.SparsePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Component(immediate = true, label = "LiteMessagePostProcessor", description = "Post Processor for Message operations", metatype = false)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Processess changes on sakai/message nodes. If the messagebox and sendstate are set to respectively outbox and pending, a message will be copied to the recipients.") })
public class LiteMessagePostProcessor implements SparsePostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteMessagePostProcessor.class);

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

    Resource resource = request.getResource();
    ResourceResolver resourceResolver = request.getResourceResolver();
    if ( SparseContentResource.SPARSE_CONTENT_RT.equals(resource.getResourceSuperType()) ) {
      Session session = resource.adaptTo(Session.class);
      ContentManager contentManager = session.getContentManager();
      Map<Content, String> messageMap = new HashMap<Content, String>();
      for (Modification m : changes) {
        try {
          switch (m.getType()) {
          case CREATE:
          case MODIFY:
            String path = m.getSource();
            if ( path.lastIndexOf("@") > 0 ) {
              path = path.substring(0, path.lastIndexOf("@"));
            }
            if ( path.endsWith("/"+MessageConstants.PROP_SAKAI_MESSAGEBOX) ) {
              path = path.substring(0, path.length()-MessageConstants.PROP_SAKAI_MESSAGEBOX.length()-1);
            }

            // The Modification Source is the Resource path, and so we
            // need to translate that into a Content path.
            // TODO This is not a cheap operation. We might be better off
            // if we start including the Content path in our Modification objects.
            Resource modifiedResource = resourceResolver.getResource(path);
            if (modifiedResource == null) {
              return;
            }
            Content content = modifiedResource.adaptTo(Content.class);
            String contentPath = content.getPath();

            if (contentManager.exists(contentPath)) {
              content = contentManager.get(contentPath);
              if (content.hasProperty(SLING_RESOURCE_TYPE_PROPERTY) && content.hasProperty(PROP_SAKAI_MESSAGEBOX)) {
                if (SAKAI_MESSAGE_RT.equals(content.getProperty(SLING_RESOURCE_TYPE_PROPERTY)) &&
                    BOX_OUTBOX.equals(content.getProperty(PROP_SAKAI_MESSAGEBOX))) {
                  String sendstate;
                  if (content.hasProperty(PROP_SAKAI_SENDSTATE)) {
                    sendstate = (String) content.getProperty(PROP_SAKAI_SENDSTATE);
                  } else {
                    sendstate = STATE_NONE;
                  }
                  messageMap.put(content, sendstate);
                }
              }
            }
            break;
          }
        } catch (StorageClientException ex) {
          LOGGER.warn("Failed to process on create for {} ", m.getSource(), ex);
        } catch (AccessDeniedException ex) {
          LOGGER.warn("Failed to process on create for {} ", m.getSource(), ex);
        }
      }

      List<String> handledNodes = new ArrayList<String>();
      // Check if we have any nodes that have a pending state and launch an OSGi
      // event
      for (Entry<Content, String> mm : messageMap.entrySet()) {
        Content content = mm.getKey();
        String path = content.getPath();
        String state = mm.getValue();
        if (!handledNodes.contains(path)) {
          if (STATE_NONE.equals(state) || STATE_PENDING.equals(state)) {

            content.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);
            contentManager.update(content);

            Dictionary<String, Object> messageDict = new Hashtable<String, Object>();
            // WARNING
            // We can't pass in the node, because the session might expire before the event gets handled
            // This does mean that the listener will have to get the node each time, and probably create a new session for each message
            // This might be heavy on performance.
            messageDict.put(EVENT_LOCATION, path);
            messageDict.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
            LOGGER.debug("Launched event for message: {} ", path);
            Event pendingMessageEvent = new Event(PENDINGMESSAGE_EVENT, messageDict);
            // KERN-790: Initiate a synchronous event.
            try {
              eventAdmin.postEvent(pendingMessageEvent);
              handledNodes.add(path);
            } catch ( Exception e ) {
              LOGGER.warn("Failed to post message dispatch event, cause {} ",e.getMessage(),e);
            }
          }
        }
      }
    }
  }
}