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

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.SAKAI_CONNECTION_PREFIX;
import static org.sakaiproject.kernel.api.connections.ConnectionConstants.SAKAI_CONNECTION_TYPES_SUFFIX;
import static org.sakaiproject.kernel.api.connections.ConnectionConstants.SAKAI_CONTACTSTORE_RT;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionOperations;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionStates;
import org.sakaiproject.kernel.api.session.SessionManagerService;
import org.sakaiproject.kernel.api.user.UserFactoryService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for doing operations with connections.
 * 
 * @scr.component immediate="true" label="Sakai Connections Service"
 *                description="Service for doing operations with connections."
 *                name
 *                ="org.sakaiproject.kernel.api.connections.ConnectionManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 * @scr.reference name="UserFactoryService"
 *                interface="org.sakaiproject.kernel.api.user.UserFactoryService"
 */
public class ConnectionManagerImpl implements ConnectionManager {

  private static final Logger LOGGER = LoggerFactory
  .getLogger(ConnectionManagerImpl.class);

  protected UserFactoryService userFactoryService;

  protected void unbindUserFactoryService(UserFactoryService userFactoryService) {
    this.userFactoryService = null;
  }

  protected void bindUserFactoryService(UserFactoryService userFactoryService) {
    this.userFactoryService = userFactoryService;
  }

  /**
   * @param store
   *          the resource from the request
   * @param userId
   *          id of the user the connection request is directed to
   * @param operation
   *          the connection operation to perform
   * @param types
   *          the types of this connection (e.g. friend of, professor of, student of)
   * @throws ConnectionException
   */
  private void handleConnectionOperation(Resource store, String userId,
      ConnectionOperations operation, String[] types) throws ConnectionException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (userId == null || "".equals(userId)) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    if (operation == null) {
      throw new IllegalArgumentException("operation cannot be null");
    }
    try {
      Session session = store.getResourceResolver().adaptTo(Session.class);
      String currentUserId = getCurrentUserId(session);
      if (!checkValidUserId(session, userId)) {
        throw new ConnectionException(404,
            "Invalid userId specified for connection: " + userId);
      }
      // contacts node
      Node contactsNode = getStoreNode(session, store);
      // contacts node of the other user
      Node otherUserNode = getLocalStoreNode(session, contactsNode.getPath(),
          userId);
      // get the state of the connection with the given user for the given node
      ConnectionStates otherUserState = getConnectionState(otherUserNode,
          userId);
      // contacts node of the current user
      Node curUserNode = getLocalStoreNode(session, contactsNode.getPath(),
          currentUserId);
      // state of the node for the current user
      ConnectionStates curUserState = getConnectionState(curUserNode, userId);
      if (ConnectionOperations.REQUEST.equals(operation)) {
        if (ConnectionStates.REQUEST.equals(otherUserState)) {
          // if we request each other then just accept both
          updateContactNodes(userId, currentUserId, otherUserNode, curUserNode,
              ConnectionStates.ACCEPT, ConnectionStates.ACCEPT);
        } else {
          if (ConnectionStates.IGNORE.equals(otherUserState)
              || ConnectionStates.REJECT.equals(otherUserState)
              || ConnectionStates.BLOCK.equals(otherUserState)) {
            // do not do anything (I think this is right)
          } else {
            // set to pending
            updateContactNodes(userId, currentUserId, otherUserNode,
                curUserNode, ConnectionStates.PENDING, ConnectionStates.REQUEST);
          }
        }

      } else if (ConnectionOperations.ACCEPT.equals(operation)) {
        if (ConnectionStates.REQUEST.equals(otherUserState)) {
          if (ConnectionStates.PENDING.equals(curUserState)) {
            updateContactNodes(userId, currentUserId, otherUserNode,
                curUserNode, ConnectionStates.ACCEPT, ConnectionStates.ACCEPT);
          }
        }

      } else if (ConnectionOperations.IGNORE.equals(operation)) {
        if (ConnectionStates.REQUEST.equals(otherUserState)) {
          setConnectionState(curUserNode, ConnectionStates.IGNORE, userId);
        }

      } else {
        // TODO handle the other states
        throw new IllegalArgumentException(
            "Don't know how to handle this operation (yet): " + operation);
      }
    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    }
  }

  /**
   * This makes the setting of the states on 2 nodes more modular to reduce code
   * repetition
   * 
   * @param otherUserId
   *          id of the user the request is directed to
   * @param currentUserId
   *          the current requesting user
   * @param otherUserNode
   *          contacts node of the user the request is directed to
   * @param curUserNode
   *          contacts node of the current requesting user
   * @param otherState
   *          state to set the otherUserNode to
   * @param curUserState
   *          state to set the curUserNode to
   * @throws ConnectionException
   */
  private void updateContactNodes(String otherUserId, String currentUserId,
      Node otherUserNode, Node curUserNode, ConnectionStates otherState,
      ConnectionStates curUserState) throws ConnectionException {
    try {
      setConnectionState(curUserNode, curUserState, otherUserId);
      setConnectionState(otherUserNode, otherState, currentUserId);
      Session curUserSession = curUserNode.getSession();
      if (curUserSession.hasPendingChanges()) {
        curUserSession.save();
      }
    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    }
  }

  /**
   * Set the state of a connection.
   * 
   * @param connectionNode
   *          the connection node
   * @param state
   *          the state
   * @param userId
   *          the id of the other user
   * @throws RepositoryException
   */
  private void setConnectionState(Node connectionNode, ConnectionStates state,
      String userId) throws RepositoryException {
    String userConnectionName = SAKAI_CONNECTION_PREFIX + userId;
    connectionNode.setProperty(userConnectionName, state.toString());
  }

  /**
   * Set the types for a connection
   * 
   * @param connectionNode
   *          the connection node
   * @param types
   *          the connection types (friend, student, etc.)
   * @param userId
   *          the id of the other user
   * @throws RepositoryException
   */
  private void setConnectionTypes(Node connectionNode, String[] types,
      String userId) throws RepositoryException {
    String userConnectionTypeName = SAKAI_CONNECTION_PREFIX + userId + SAKAI_CONNECTION_TYPES_SUFFIX;
    connectionNode.setProperty(userConnectionTypeName, types);
  }

  private boolean checkValidUserId(Session session, String userId) {
    boolean valid = false;
    Authorizable authorizable;
    try {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      authorizable = userManager.getAuthorizable(userId);
      if ( authorizable.isGroup() ) {
        // not a user
        valid = false;
      } else {
        valid = true;
      }
    } catch (Exception e) {
      valid = false;
    }
    return valid;
  }

  private String getCurrentUserId(Session session) {
    /*
     * use request.getRemoteUser() or session.getUserId() to get a session user
     * request.getResourceResolver().adaptTo(Session.class);
     */
    return session.getUserID();
    //return sessionManagerService.getCurrentUserId();
  }

  /**
   * @param localStore
   * @return
   * @throws ConnectionException
   * @throws RepositoryException
   */
  private ConnectionStates getConnectionState(Node connectionNode, String userId)
  throws ConnectionException, RepositoryException {
    if (connectionNode == null) {
      throw new ConnectionException(404, "Cant find invitation from user "
          + userId);
    }

    String userConnectionName = SAKAI_CONNECTION_PREFIX + userId;
    if (!connectionNode.hasProperty(userConnectionName)) {
      throw new ConnectionException(404, "Cant find invitation from user, "
          + userId + " (not an invitation)");
    }
    try {
      return ConnectionStates.valueOf(connectionNode.getProperty(
          userConnectionName).getString());
    } catch (IllegalArgumentException e) {
      return ConnectionStates.NONE;
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
    String localStorePath = path + PathUtils.getHashedPath(userId, 4);
    if (session.itemExists(localStorePath)) {
      Item storeItem = session.getItem(localStorePath); // JcrUtils.deepGetOrCreateNode(session, localStorePath);
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
   * @return the Node that is the root node of the store, or null if the
   *         resource does not come from a store.
   * @throws RepositoryException
   */
  private Node getStoreNode(Session session, Resource store)
  throws RepositoryException {
    String path = store.getPath();
    Node node = JcrUtils.getFirstExistingNode(session, path);
    while (!"/".equals(node.getPath())) {
      if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && SAKAI_CONTACTSTORE_RT.equals(node.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        LOGGER.debug(" {} is a connection store file, base is {}  ", path, node
            .getPath());
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  public String connect(Resource resource, String userId,
      ConnectionOperations operation, String requesterUserId)
      throws ConnectionException {
    // TODO Auto-generated method stub
    return null;
  }

  public String request(Resource resource, String userId, String[] types,
      String requesterUserId) throws ConnectionException {
    // node is the contacts node: /_user/contacts
    if (resource == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    // userId is the target user
    if (userId == null || "".equals(userId)) {
      throw new IllegalArgumentException("targetUserId cannot be null");
    }
    String targetUserId = userId;
    if (types == null) {
      // is this ok?
      types = new String[] {};
    }
    try {
      Session session = resource.getResourceResolver().adaptTo(Session.class);
      if (requesterUserId == null || "".equals(requesterUserId)) {
        // default to the current user
        requesterUserId = getCurrentUserId(session);
      }
      String contactsPath = resource.getPath();
      // fail is the supplied users are invalid
      if (!checkValidUserId(session, requesterUserId)) {
        throw new ConnectionException(404,
            "Invalid requesterUserId specified for connection: " + requesterUserId);
      }
      if (!checkValidUserId(session, targetUserId)) {
        throw new ConnectionException(404,
            "Invalid targetUserId specified for connection: " + targetUserId);
      }
      // get the contactstore nodes
      Node requesterStoreNode = getStorageNode(session, contactsPath, requesterUserId);
      Node targetStoreNode = getStorageNode(session, contactsPath, targetUserId);
      // get the contact nodes
      Node requesterNode = getStorageNode(requesterStoreNode.getSession(), requesterStoreNode.getPath(), targetUserId);
      Node targetNode = getStorageNode(targetStoreNode.getSession(), targetStoreNode.getPath(), requesterUserId);
      // TODO  many many things using the requester and target nodes
    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    }

    throw new UnsupportedOperationException("need return");
  }

  private Node getStorageNode(Session session, String basePath, String nodeName) throws ConnectionException {
    Node n = null;
    String fullNodePath = basePath + PathUtils.getHashedPath(nodeName, 4);
    try {
      if (session.itemExists(fullNodePath)) {
        Item storeItem = session.getItem(fullNodePath); // JcrUtils.deepGetOrCreateNode(session, localStorePath);
        if (storeItem.isNode()) {
          n = (Node) storeItem;
        }
      } else {
        // create the node
        JcrUtils.deepGetOrCreateNode(session, fullNodePath);
      }
    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    }
    return n;
  }

}
