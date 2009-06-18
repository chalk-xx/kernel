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
package org.sakaiproject.kernel.connections;

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.SAKAI_CONNECTIONSTORE_RT;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * 
 */
public class ConnectionManagerImpl implements ConnectionManager {

  /**
   *
   */
  private static final String SAKAI_INVITIATION = "sakai:invitiation";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionManagerImpl.class);

  /**
   * {@inheritDoc}
   * 
   * @throws ConnectionException
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#accept(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void accept(Resource store, String userId) throws ConnectionException {
    try {
      Session session = store.getResourceResolver().adaptTo(Session.class);

      Node node = getStoreNode(session, store);

      Node localStore = getLocalStoreNode(session, node.getPath(), userId);

      InviteState invite = getInvitationState(localStore, userId);

      if (InviteState.REQUESTED.equals(invite)) {
        Node remoteStore = getRemoteStoreNode(localStore, userId);
        InviteState remoteState = getInvitationState(localStore, userId);
        if (InviteState.PENDING.equals(remoteStore)) {
          setInvitationState(remoteStore, InviteState.ACCEPTED);
          setInvitationState(localStore, InviteState.ACCEPTED);
          Session remoteSession = remoteStore.getSession();
          if (remoteSession.hasPendingChanges()) {
            remoteSession.save();
          }
        }

      }

    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    }
  }

  /**
   * Set the invitation state of a connection.
   * @param connectionNode the connection node
   * @param state the state
   * @throws RepositoryException
   */
  private void setInvitationState(Node connectionNode, InviteState state)
      throws RepositoryException {
    connectionNode.setProperty(SAKAI_INVITIATION, state.toString());
  }

  /**
   * Get the remote invitation from the 
   * @param userId 
   * @return
   */
  private Node getRemoteStoreNode(Node connectionNode, String userId) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @param localStore
   * @return
   * @throws ConnectionException
   * @throws RepositoryException
   */
  private InviteState getInvitationState(Node connectionNode, String userId)
      throws ConnectionException, RepositoryException {
    if (connectionNode == null) {
      throw new ConnectionException(404, "Cant find invitation from user " + userId);
    }

    if (!connectionNode.hasProperty("sakai:invitation")) {
      throw new ConnectionException(404, "Cant find invitaiton from user, " + userId
          + " (not an invitation)");
    }
    try {
      return InviteState.valueOf(connectionNode.getProperty(SAKAI_INVITIATION)
          .getString());
    } catch (IllegalArgumentException e) {
      return InviteState.NONE;
    }

  }

  /**
   * Get the a node within the store.
   * 
   * @param session
   *          the current session.
   * @param path
   * @param userId
   * @return
   * @throws RepositoryException
   */
  private Node getLocalStoreNode(Session session, String path, String userId)
      throws RepositoryException {
    String localStorePath = path + PathUtils.getHashedPath(userId, 3);
    if (session.itemExists(localStorePath)) {
      Item storeItem = session.getItem(localStorePath);
      if (storeItem.isNode()) {
        return (Node) storeItem;
      }
    }
    return null;
  }

  /**
   * Gets the base node of the store.
   * 
   * @param session
   *          the session to use to perform the search with.
   * @param store
   *          a resource within the store.
   * @return the Node that is the root node of the store, or null if the resource does not
   *         come from a store.
   * @throws RepositoryException
   */
  private Node getStoreNode(Session session, Resource store) throws RepositoryException {
    String path = store.getPath();
    Node node = JcrUtils.getFirstExistingNode(session, path);
    while (!"/".equals(node.getPath())) {
      if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && SAKAI_CONNECTIONSTORE_RT.equals(node.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        LOGGER
            .debug(" {} is a connection store file, base is {}  ", path, node.getPath());
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#block(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void block(Resource store, String userId) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#cancel(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void cancel(Resource store, String userId) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#ignore(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void ignore(Resource store, String userId) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#invite(org.apache.sling.api.resource.Resource,
   *      java.lang.String, java.lang.String)
   */
  public String invite(Resource store, String userId, String type) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#reject(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void reject(Resource store, String userId) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.connections.ConnectionManager#remove(org.apache.sling.api.resource.Resource,
   *      java.lang.String)
   */
  public void remove(Resource store, String userId) {
    // TODO Auto-generated method stub

  }

}
