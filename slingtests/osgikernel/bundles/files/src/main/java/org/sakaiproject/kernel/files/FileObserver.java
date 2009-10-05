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
package org.sakaiproject.kernel.files;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

/**
 * Listens to all newly created nodes in the repository and adds some properties to it (if
 * nescecary)
 * 
 * 
 * @scr.component metatype="no" immediate="true" label="FileObserver"
 *                description="Listens to uploaded files trough webdav"
 * @scr.property name="service.description"
 *               value="Listens to uploaded files trough webdav."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class FileObserver {

  private SlingRepository slingRepository;
  public static final Logger log = LoggerFactory.getLogger(FileObserver.class);

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  private Session session = null;

  /**
   * Register the listener.
   * 
   * @param ctx
   * @throws Exception
   */
  public void activate(ComponentContext ctx) throws Exception {
    session = slingRepository.loginAdministrative(null);
    ObservationManager observationManager = session.getWorkspace()
        .getObservationManager();
    String[] types = { "nt:file" };
    observationManager.addEventListener(new EventListener() {
      public void onEvent(EventIterator eventIterator) {
        while (eventIterator.hasNext()) {
          Event event = eventIterator.nextEvent();
          if (event.getType() == Event.NODE_ADDED) {
            log.info("Node added.");
            try {
              Node node = (Node) session.getItem(event.getPath());
              if (node.getName().equals(JcrConstants.JCR_CONTENT)) {
                node = node.getParent();
              }

              if (!node.isLocked()) {

                // We only catch nodes who don't have a sling resource type property set.
                if (!node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
                  log.info("Node doesnt have a sling resourceType");
                  // Add the mixin so we can set properties on this file.
                  if (node.canAddMixin("sakai:propertiesmix")) {
                    node.addMixin("sakai:propertiesmix");
                  }
                  // Set resourcetype to sakai/file, set the sakai:filename and the
                  // sakai:user
                  node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                      FilesConstants.RT_SAKAI_FILE);
                  node.setProperty(FilesConstants.SAKAI_FILENAME, node.getName());
                  node.setProperty(FilesConstants.SAKAI_USER, event.getUserID());

                  node.save();
                }
              }
              else {
                log.info("Node was locked.\n\nn\n\n\\n\n\n\n\n\n\n\n\n");
              }

            } catch (RepositoryException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }, Event.NODE_ADDED, "/", true, null, types, true);
    log.info("Started listening for new files.");
  }

  protected void deactivate(ComponentContext componentContext) {
    if (session != null) {
      session.logout();
      session = null;
    }
  }

}
