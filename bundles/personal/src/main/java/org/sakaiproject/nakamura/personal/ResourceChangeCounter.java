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
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.personal.PersonalTrackingStore;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

@Component(inherit = true, label = "%sakai-event.name", immediate = true)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Resource ADDED and UPDATED Events."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/lite/content/ADDED",
        "org/sakaiproject/nakamura/lite/content/UPDATED" }) })
public class ResourceChangeCounter implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceChangeCounter.class);
  protected static final String SAKAI_POOLED_CONTENT = "sakai/pooled-content";
  
  @Reference
  protected PersonalTrackingStore store;
  
  @Reference
  protected Repository repository;

  /**
   * {@inheritDoc}
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {
    // be fast
    final String path = (String) event.getProperty("path");
    if (path != null) {
      Session adminSession = null;
      try {
        adminSession = repository.loginAdministrative();
        final Content content = adminSession.getContentManager().get(path);
        if (content.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          final String resourceType = (String) content
              .getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
          if (resourceIsOfInterest(resourceType)) {
            countThisEvent(event, resourceType, adminSession);
          }
        }
      } catch (ClientPoolException e) {
        LOG.error(e.getLocalizedMessage(), e);
      } catch (StorageClientException e) {
        LOG.error(e.getLocalizedMessage(), e);
      } catch (AccessDeniedException e) {
        LOG.error(e.getLocalizedMessage(), e);
      } finally {
        if (adminSession != null) {
          try {
            adminSession.logout();
          } catch (ClientPoolException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new IllegalStateException(e);
          }
        }
      }
    }
  }

  private void countThisEvent(final Event e, final String eventResourceType,
      final Session session) throws AccessDeniedException, StorageClientException {

    String userId = (String)e.getProperty("userid");
    String path = (String)e.getProperty("path");
    String activityType = "CHANGE";
    String resourceId = null;
    String resourceType = null;
    if (path != null) {
      if (path.startsWith("a:")) {
        resourceId = PathUtils.getAuthorizableId(path);
        final Authorizable az = session.getAuthorizableManager().findAuthorizable(
            resourceId);
        if (az != null) {
          resourceType = (az instanceof Group) ? "group" : "user";
        }
      } else {
        if (SAKAI_POOLED_CONTENT.equals(eventResourceType)) {
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
        || SAKAI_POOLED_CONTENT.equals(resourceType));
  }

}
