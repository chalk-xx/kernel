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
package org.sakaiproject.nakamura.site;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;

/**
 *
 * @author chall
 */
@Component
@Property(name = EventConstants.EVENT_TOPIC, value = SiteEvent.TOPIC + "startJoinWorkflow")
public class JoinSiteWorkflowHandler implements EventHandler {

  public void handleEvent(Event event) {
    String sitePath = (String) event.getProperty(SiteEvent.SITE);
    String user = (String) event.getProperty(SiteEvent.USER);
    String group = (String) event.getProperty(SiteEvent.GROUP);

    
  }

}
