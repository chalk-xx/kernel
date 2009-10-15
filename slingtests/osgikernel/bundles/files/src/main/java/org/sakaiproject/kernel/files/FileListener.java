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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

// TODO: Javadoc
// FIXME: is this active ? there are no SCR annotations, so it cant be running.
public class FileListener implements EventHandler {
  private static final Logger log = LoggerFactory.getLogger(FileListener.class);

  private SlingRepository slingRepository;

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {

    // Get the message
    // get the node, call up the appropriate handler and pass off based on
    // message type
    log.debug("handleEvent called");
    Session adminSession = null;
    try {
      String path = event.getProperty("path").toString();
      adminSession = slingRepository.loginAdministrative(null);
      Node node = (Node) adminSession.getItem(path);

      if (node.getName().equals(JcrConstants.JCR_CONTENT)) {
        node = node.getParent();
      }

      log.info("Grabbed node: " + node.getPath());

      // If the name contains a : it's not uploaded trough webdav, so we
      // should ignore it.
      // Files starting with a dot are ignored as well.
      if (!node.getName().startsWith(".") && node.getName().indexOf(":") == -1) {
        // We only catch nodes who don't have a sling resource type property
        // set.
        if (!node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          log.info("Node doesn't have a sling resourceType");

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
          if (adminSession.hasPendingChanges())
            adminSession.save();

          log.info("Saved session.");

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
