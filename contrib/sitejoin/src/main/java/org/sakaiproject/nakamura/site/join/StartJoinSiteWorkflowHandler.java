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
 * specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.site.join;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;

import java.util.HashMap;

import javax.jcr.Session;

/**
 *
 * @author chall
 */
@Component
@Property(name = EventConstants.EVENT_TOPIC, value = SiteEvent.TOPIC + "startJoinWorkflow")
public class StartJoinSiteWorkflowHandler implements EventHandler {

  @Reference
  private MessagingService messagingService;

  public void handleEvent(Event event) {
    Session session;
    // #1 add user to the join requests of a site
    createPendingRequest(event);

    // #2 send message to site owner
    sendMessage(event);
  }

  private void createPendingRequest(Event event) {
    // create a node under /sites/mysite/joinrequests/u/us/user
  }

  private void sendMessage(Event event) {
    String sitePath = (String) event.getProperty(SiteEvent.SITE);
    String user = (String) event.getProperty(SiteEvent.USER);
    String group = (String) event.getProperty(SiteEvent.GROUP);
    String siteOwner = null;
    String subject = null;
    String body = null;

    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put("sakai:type", "internal");
    props.put("sakai:sendstate", "pending");
    props.put("sakai:messagebox", "outbox");
    props.put("sakai:to", siteOwner);
    props.put("sakai:from", user);
    props.put("sakai:subject", subject);
    props.put("sakai:body", body);
    props.put("_charset_", "utf-8");
    props.put("sakai:category", "invitation");

    Session session = null;
    messagingService.create(session, props);
  }

}
