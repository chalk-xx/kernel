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
package org.sakaiproject.kernel.activity;

import static org.sakaiproject.kernel.api.activity.ActivityConstants.ACTOR_PROPERTY;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.SOURCE_PROPERTY;
import static org.sakaiproject.kernel.api.personal.PersonalConstants._USER_PRIVATE;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionState;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @scr.component immediate="true" label="ActivityEventProcessor"
 *                description="ActivityEventProcessor"
 * @scr.property name="service.description" value="ActivityEventProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics" value="org/sakaiproject/kernel/activity"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 * 
 */
public class ActivityEventProcessor implements EventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ActivityEventProcessor.class);
  protected ConnectionManager connectionManager = null;

  protected SlingRepository slingRepository;

  public void handleEvent(Event event) {
    LOG.debug("handleEvent(Event {})", event);
    final String activityItemPath = (String) event.getProperty("activityItemPath");
    LOG.info("Processing activity: {}", activityItemPath);
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      Node activity = (Node) session.getItem(activityItemPath);
      if (activity != null) {
        // process activity
        // hints will be attached to the activity as to where we need to deliver
        // for example: connections, siteA, siteB
        // from Nico: /sites/siteid/activityFeed.json
        // Let's try the the simpler connections case first
        String actor = activity.getProperty(ACTOR_PROPERTY).getString();
        if (actor == null || "".equals(actor)) { // we must know the actor
          throw new IllegalStateException("Could not determine actor of activity: "
              + activity);
        }
        // TODO assume we were passed the connections hint; need to check
        deliverActivityToConnections(session, activity, actor);
      } else {
        LOG.error("Could not process activity: {}", activityItemPath);
        throw new Error("Could not process activity: " + activityItemPath);
      }

    } catch (RepositoryException e) {
      LOG.error("Could not process activity: {}", activityItemPath);
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }

  }

  private void deliverActivityToConnections(Session session, Node activity, String actor)
      throws RepositoryException {
    LOG.debug("deliverActivityToConnections(Session {}, Node {}, String {})",
        new Object[] { session, activity, actor });
    // get the users connected to the actor and distribute to them
    String activityFeedPath = null;
    List<String> connections = connectionManager.getConnectedUsers(actor,
        ConnectionState.ACCEPTED);
    if (connections == null || connections.size() <= 0) {
      LOG.debug("{} acted but has no connections; nothing to do.", actor);
    } else { // actor has connections
      for (String connection : connections) {
        // deliver to each connection
        LOG.debug("{} acted; delivering activity to connection: {}", new Object[] {
            actor, connection });
        // /_user/private is a BigStore, get the hashed path
        activityFeedPath = PathUtils.toInternalHashedPath(_USER_PRIVATE, connection,
            "/activityFeed");
        deliverActivityToFeed(session, activity, activityFeedPath);
      }
    }
  }

  private void deliverActivityToFeed(Session session, Node activity,
      String activityFeedPath) throws RepositoryException {
    // ensure the activityFeed node with the proper type
    Node activityFeedNode = JcrUtils.deepGetOrCreateNode(session, activityFeedPath);
    if (activityFeedNode.isNew()) {
      activityFeedNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          ActivityConstants.ACTIVITY_FEED_RESOURCE_TYPE);
      session.save();
    }
    // activityFeed exists, let's continue with delivery
    // activityFeed is a BigStore, get the hashed (real) path
    final String deliveryPath = PathUtils.toInternalHashedPath(activityFeedPath, UUID
        .randomUUID().toString(), "");
    // ensure the parent path exists before we copy source activity
    final String parentPath = deliveryPath.substring(0, deliveryPath.lastIndexOf("/"));
    final Node parentNode = JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (parentNode.isNew()) {
      session.save();
    }
    // now copy the activity from the store to the feed
    copyActivityItem(session, activity.getPath(), deliveryPath);
  }

  private void copyActivityItem(Session session, String source, String destination)
      throws RepositoryException {
    LOG.debug("copyActivityItem(Session {}, String {}, String {})", new Object[] {
        session, source, destination });
    // now copy the activity from the source to the destination
    session.getWorkspace().copy(source, destination);
    // next let's create a source property to refer back to the original item
    // in the ActivityStore
    Node feedItem = (Node) session.getItem(destination);
    feedItem.setProperty(SOURCE_PROPERTY, source);
    session.save();
  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }
}
