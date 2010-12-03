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
package org.sakaiproject.nakamura.personal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.personal.PersonalTrackingStore;
import org.sakaiproject.nakamura.util.PathUtils;

import java.util.Calendar;

@Component(inherit = true, label = "%sakai-event.name", immediate = true)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Resource CHANGED Events."),
    @Property(name = "event.topics", value = "org/apache/sling/api/resource/Resource/CHANGED")})
public class ResourceChangeCounter implements EventHandler {
  
  @Reference
  protected PersonalTrackingStore store;

  /**
   * {@inheritDoc}
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event e) {
    // be fast
    String resourceType = (String)e.getProperty("resourceType");
    
    if (resourceIsOfInterest(resourceType)) {
      countThisEvent(e);
    }
  }

  private void countThisEvent(Event e) {
    String eventResourceType = (String)e.getProperty("resourceType");
    String userId = (String)e.getProperty("userid");
    String path = (String)e.getProperty("path");
    String activityType = "CHANGE";
    String resourceId = null;
    String resourceType = null;
    if (path != null) {
      if (path.startsWith("/_group") || path.startsWith("/_user")) {
        resourceType = path.substring(2); // trims off /_
        resourceType = resourceType.substring(0, resourceType.indexOf("/"));
        resourceId = (String)PathUtils.translateAuthorizablePath(path);
        resourceId = resourceId.substring(2); // trims off /~
        resourceId = resourceId.substring(0,resourceId.indexOf("/"));
    } else {
      if ("sakai/pooled-content".equals(eventResourceType)) {
        resourceType = "content";
        resourceId = path.substring(path.lastIndexOf("/") + 1);
      }
    }
      
    }
    store.recordActivity(resourceId, resourceType, activityType, userId, Calendar.getInstance());
    
  }

  private boolean resourceIsOfInterest(String resourceType) {
    return ("sakai/group-profile".equals(resourceType)
        || "sakai/page".equals(resourceType)
        || "sakai/pooled-content".equals(resourceType));
  }

}
