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
package org.sakaiproject.nakamura.activity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

@Component(metatype=true, immediate = true, inherit=true)
@Service(value=EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler for posting activities from other services.."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/activity/POSTED"}) })
public class SystemGeneratedActivityHandler implements EventHandler {


  public static final Logger LOG = LoggerFactory
      .getLogger(SystemGeneratedActivityHandler.class);
  @Reference
  private Repository repository;
  @Reference
  private ActivityService activityService;
  
  public void handleEvent(Event event) {
    
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      String path = (String) event.getProperty("path");
      String userId = (String) event.getProperty("userid");
      @SuppressWarnings("unchecked")
      final Map<String, Object> activityProperties = (Map<String, Object>) event.getProperty("attributes");
      
      final ContentManager contentManager = adminSession.getContentManager();
      Content location = contentManager.get(path);
      if ( location != null ) {
        activityService.createActivity(adminSession, location, userId, new ActivityServiceCallback() {
          
          public void processRequest(Content activtyNode) throws StorageClientException,
              ServletException, IOException, AccessDeniedException {
           
            for ( Entry<String, Object> e : activityProperties.entrySet()) {
              activtyNode.setProperty(e.getKey(), e.getValue());
            }
            contentManager.update(activtyNode); 
          }
        });
      }
      
    } catch (ClientPoolException e) {
      LOG.warn(e.getMessage(),e);
    } catch (StorageClientException e) {
      LOG.warn(e.getMessage(),e);
    } catch (AccessDeniedException e) {
      LOG.warn(e.getMessage(),e);
    } catch (ServletException e) {
      LOG.warn(e.getMessage(),e);
    } catch (IOException e) {
      LOG.warn(e.getMessage(),e);
    } finally {
      if ( adminSession != null ) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOG.warn(e.getMessage(),e);
        }
      }
    }
  }


}
