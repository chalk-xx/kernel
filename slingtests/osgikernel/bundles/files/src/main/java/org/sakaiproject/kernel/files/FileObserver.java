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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;

/**
 * @scr.component label="FileObserver" immediate="true"
 * @scr.property name="service.description" value="Observer who listens to added nodes."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class FileObserver {
  private static final Logger log = LoggerFactory.getLogger(FileObserver.class);
  private SlingRepository slingRepository;

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  private Session session = null;

  protected void activate(ComponentContext ctxt) {
    try {
      log.info("Activating service.");
      session = slingRepository.loginAdministrative(null);
      ObservationManager observationManager = session.getWorkspace()
          .getObservationManager();
      String[] types = { "nt:file" };
      observationManager.addEventListener(new EventListener() {
        public void onEvent(EventIterator eventIterator) {
          while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            if (event.getType() == Event.NODE_ADDED) {
              log.info("New event.");

              Session adminSession = null;
              try {
                String path = event.getPath();
                adminSession = slingRepository.loginAdministrative(null);
                Node node = (Node) adminSession.getItem(path);

                if (node.getName().equals(JcrConstants.JCR_CONTENT)) {
                  node = node.getParent();
                }

                path = node.getPath();

                log.info("Grabbed node: " + node.getPath());

                // If the name contains a : it's not uploaded trough webdav, so we
                // should ignore it.
                // Files starting with a dot are ignored as well.
                if (!node.getName().startsWith(".") && node.getName().indexOf(":") == -1) {
                  // We only catch nodes who don't have a sling resource type property
                  // set.
                  if (!node
                      .hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
                    log.info("Node doesn't have a sling resourceType");

                    // Thread.sleep(1000);
                    addProps(node, path, adminSession);

                  }
                }
              } catch (LoginException e) {
                log.error(e.getMessage(), e);
                e.printStackTrace();
              } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                e.printStackTrace();
              } finally {
                if (adminSession != null)
                  adminSession.logout();
              }

            }
          }
        }
      }, Event.NODE_ADDED, "/", true, null, types, true);
      log.info("Started observing changes to the repository.");
    } catch (RepositoryException e1) {
      e1.printStackTrace();
    }

  }

  protected void addProps(Node node, String path, Session adminSession)
      throws NoSuchNodeTypeException, VersionException, ConstraintViolationException,
      LockException, RepositoryException {

    while (node.isNew() || node.isLocked()) {
      log.info("Node is new/locked wait untill it is not.");
      Thread.yield();
    }

    // Add the mixin so we can set properties on this file.
    if (node.canAddMixin("sakai:propertiesmix")) {
      log.info("Adding sakai:propertiesmix.");
      node.addMixin("sakai:propertiesmix");
    }
    // Set resourcetype to sakai/file, set the sakai:filename and the
    // sakai:user
    node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        FilesConstants.RT_SAKAI_FILE);
    node.setProperty(FilesConstants.SAKAI_FILENAME, node.getName());
    node.setProperty(FilesConstants.SAKAI_USER, node.getSession().getUserID());

    log.info("Set properties.");

    saveSession(node, path, adminSession);

    log.info("Saved session.");
  }

  private void saveSession(Node node, String path, Session adminSession)
      throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
      VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    try {
      if (adminSession.hasPendingChanges())
        adminSession.save();
    } catch (InvalidItemStateException e) {
      // kep retrying...
      log.info("Failed to save session, retrying...");
      Thread.yield();
      adminSession.logout();
      adminSession = slingRepository.loginAdministrative(null);
      node = (Node) adminSession.getItem(path);
      addProps(node, path, adminSession);
    }
  }

  protected void deactivate(ComponentContext componentContext) {
    if (session != null) {
      session.logout();
      session = null;
    }
  }

}
