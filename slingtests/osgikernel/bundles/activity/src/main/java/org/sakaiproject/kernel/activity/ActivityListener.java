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

import static org.sakaiproject.kernel.api.activity.ActivityConstants.ACTIVITY_FEED_NAME;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.PARAM_ACTOR_ID;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.PARAM_SOURCE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.activemq.ConnectionFactoryService;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.api.activity.ActivityUtils;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionState;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Topic;

@Component(label = "ActivityListener", description = "Listens for new activities", immediate = true, metatype = true)
public class ActivityListener implements MessageListener {

  // References/properties need for JMS
  @Reference
  protected ConnectionFactoryService connFactoryService;
  @Property(value = "vm://localhost:41000")
  public static final String BROKER_URL = "activity.brokerUrl";

  // References needed to actually deliver the activity.
  @Reference
  protected SlingRepository slingRepository;
  @Reference
  protected ConnectionManager connectionManager;
  @Reference
  protected SiteService siteService;

  public static final Logger LOG = LoggerFactory
      .getLogger(ActivityListener.class);

  private String brokerUrl = "";
  private ConnectionFactory connectionFactory;
  private Connection connection = null;

  /**
   * Start a JMS connection.
   */
  @SuppressWarnings("unchecked")
  public void activate(ComponentContext componentContext) {
    Dictionary properties = componentContext.getProperties();

    String _brokerUrl = (String) properties.get(BROKER_URL);
    try {
      boolean urlEmpty = _brokerUrl == null || _brokerUrl.trim().length() == 0;
      if (!urlEmpty) {
        if (!brokerUrl.equals(_brokerUrl)) {
          LOG.info("Creating a new ActiveMQ Connection Factory for activities");
          connectionFactory = connFactoryService.createFactory(_brokerUrl);
        }

        if (connectionFactory != null) {
          connection = connectionFactory.createConnection();
          javax.jms.Session session = connection.createSession(false,
              javax.jms.Session.AUTO_ACKNOWLEDGE);
          Topic dest = session.createTopic(ActivityConstants.EVENT_TOPIC);
          MessageConsumer consumer = session.createConsumer(dest);
          consumer.setMessageListener(this);
          connection.start();
        }
      } else {
        LOG.error("Cannot create JMS connection factory with an empty URL.");
      }
      brokerUrl = _brokerUrl;
    } catch (JMSException e) {
      LOG.error(e.getMessage(), e);
      if (connection != null) {
        try {
          connection.close();
        } catch (JMSException e1) {
        }
      }
    }
  }

  /**
   * Close the JMS connection
   */
  protected void deactivate(ComponentContext ctx) {
    if (connection != null) {
      try {
        connection.close();
      } catch (JMSException e) {
        LOG.error("Cannot close the activity JMS connection.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
   */
  public void onMessage(Message message) {
    try {
      final String activityItemPath = message
          .getStringProperty(ActivityConstants.EVENT_PROP_PATH);
      LOG.info("Processing activity: {}", activityItemPath);
      Session session = slingRepository.loginAdministrative(null);
      Node activity = (Node) session.getItem(activityItemPath);
      if (activity != null) {
        String actor = activity.getProperty(PARAM_ACTOR_ID).getString();
        if (actor == null || "".equals(actor)) {
          // we must know the actor
          throw new IllegalStateException(
              "Could not determine actor of activity: " + activity);
        }

        // We copy the activity to the creator, his/her connections and the site.
        // If the activity didn't take place in a site, nothing will happen
        // If some contacts don't have READ on the activity it won't get copied over.
        // The creator will always get a copy.

        deliverActivityToCreater(session, activity, actor);
        deliverActivityToConnections(session, activity, actor);
        deliverActivityToSite(session, activity, actor);
      } else {
        LOG.error("Could not process activity: {}", activityItemPath);
        throw new Error("Could not process activity: " + activityItemPath);
      }

    } catch (JMSException e) {
      LOG.error("Got a JMS exception in the activity listener.", e);
    } catch (RepositoryException e) {
      LOG.error("Got a repository exception in the activity listener.", e);
    }
  }

  /**
   * Writes the activity to the creator his own store.
   * 
   * @param session
   * @param activity
   * @param actor
   * @throws RepositoryException
   */
  private void deliverActivityToCreater(Session session, Node activity,
      String actor) throws RepositoryException {
    String path = ActivityUtils.getUserFeed(actor);
    deliverActivityToFeed(session, activity, path);
  }

  /**
   * Deliver an activity to a site.
   * 
   * @param session
   * @param activity
   * @param actor
   * @throws RepositoryException
   */
  protected void deliverActivityToSite(Session session, Node activity,
      String actor) throws RepositoryException {

    Node siteNode = activity;
    boolean isSiteActivity = false;
    while (siteNode.getPath() != "/") {
      if (siteService.isSite(siteNode)) {
        isSiteActivity = true;
        break;
      }
      siteNode = siteNode.getParent();
    }
    if (isSiteActivity) {
      String path = siteNode.getPath() + "/" + ACTIVITY_FEED_NAME;
      deliverActivityToFeed(session, activity, path);
    }

  }

  /**
   * Send an activity to all the contacts who have READ access on the original activity
   * 
   * @param session
   * @param activity
   * @param actor
   * @throws RepositoryException
   */
  private void deliverActivityToConnections(Session session, Node activity,
      String actor) throws RepositoryException {
    // get the users connected to the actor and distribute to them
    String activityFeedPath = null;
    List<String> connections = connectionManager.getConnectedUsers(actor,
        ConnectionState.ACCEPTED);
    if (connections != null && connections.size() > 0) {

      String activityPath = activity.getPath();
      AccessControlManager adminACM = AccessControlUtil
          .getAccessControlManager(session);
      Privilege readPriv = adminACM.privilegeFromName("jcr:read");
      Privilege[] privs = new Privilege[] { readPriv };
      for (String connection : connections) {
        // Check if this connection has READ access on the path.
        boolean allowCopy = true;
        Session userSession = null;
        try {
          final SimpleCredentials credentials = new SimpleCredentials(
              connection, "foo".toCharArray());
          userSession = session.impersonate(credentials);
          AccessControlManager userACM = AccessControlUtil
              .getAccessControlManager(userSession);
          allowCopy = userACM.hasPrivileges(activityPath, privs);
        } finally {
          // We no longer need this session anymore, release it.
          userSession.logout();
        }

        if (allowCopy) {
          // Get the activity feed for this contact and deliver it.
          activityFeedPath = PersonalUtils.getPrivatePath(connection, "/"
              + ACTIVITY_FEED_NAME);
          deliverActivityToFeed(session, activity, activityFeedPath);
        }
      }
    }
  }

  /**
   * 
   * @param session
   *          The session that should be used to do the delivering.
   * @param activity
   *          The node that represents the activity.
   * @param activityFeedPath
   *          The path that holds the feed where the activity should be delivered.
   * @throws RepositoryException
   */
  private void deliverActivityToFeed(Session session, Node activity,
      String activityFeedPath) throws RepositoryException {
    // ensure the activityFeed node with the proper type
    Node activityFeedNode = JcrUtils.deepGetOrCreateNode(session,
        activityFeedPath);
    if (activityFeedNode.isNew()) {
      activityFeedNode.setProperty(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          ActivityConstants.ACTIVITY_FEED_RESOURCE_TYPE);
      session.save();
    }
    // activityFeed exists, let's continue with delivery
    // activityFeed is a BigStore, get the hashed (real) path
    final String deliveryPath = PathUtils.toInternalHashedPath(
        activityFeedPath, activity.getName(), "");
    // ensure the parent path exists before we copy source activity
    final String parentPath = deliveryPath.substring(0, deliveryPath
        .lastIndexOf("/"));
    final Node parentNode = JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (parentNode.isNew()) {
      session.save();
    }
    // now copy the activity from the store to the feed
    copyActivityItem(session, activity.getPath(), deliveryPath);
  }

  /**
   * Copies an activity over.
   * 
   * @param session
   *          The session that should be used to do the copying.
   * @param source
   *          The path of the original activity.
   * @param destination
   *          The path where the activity should be copied to.
   * @throws RepositoryException
   */
  private void copyActivityItem(Session session, String source,
      String destination) throws RepositoryException {
    LOG.debug("copyActivityItem(Session {}, String {}, String {})",
        new Object[] { session, source, destination });
    // now copy the activity from the source to the destination
    session.getWorkspace().copy(source, destination);
    // next let's create a source property to refer back to the original item
    // in the ActivityStore
    Node feedItem = (Node) session.getItem(destination);
    feedItem.setProperty(PARAM_SOURCE, source);
    session.save();
  }

}
