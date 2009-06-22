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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.connections.ConnectionConstants;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionOperations;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionStates;
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
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class ConnectionManagerImpl implements ConnectionManager {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionManagerImpl.class);

  protected SlingRepository slingRepository;
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  /**
   * Check to see if a userId is actually a valid one
   * @param session the JCR session
   * @param userId the userId to check
   * @return
   */
  private boolean checkValidUserId(Session session, String userId) throws ConnectionException {
    boolean valid = false;
    Authorizable authorizable;
    try {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      authorizable = userManager.getAuthorizable(userId);
      if (authorizable.isGroup()) {
        // not a user
        valid = false;
      } else {
        valid = true;
      }
    } catch (RepositoryException e) {
      // general repo failure
      throw new ConnectionException(500, e.getMessage(), e);
    } catch (Exception e) {
      // other failures return false
      valid = false;
    }
    return valid;
  }

  /**
   * Get the current user id
   * 
   * @param session
   *          the JCR session
   * @return the current user OR null if there is no current user
   */
  private String getCurrentUserId(Session session) {
    /*
     * use request.getRemoteUser() or session.getUserId() to get a session user
     * request.getResourceResolver().adaptTo(Session.class);
     */
    return session.getUserID();
    // return sessionManagerService.getCurrentUserId();
  }

  /**
   * Get the connection state from a node
   * 
   * @param userContactNode
   *          the node to check (should be a user contact node)
   * @return the connection state (may be NONE)
   * @throws ConnectionException
   * @throws RepositoryException
   */
  private ConnectionStates getConnectionState(Node userContactNode)
      throws ConnectionException, RepositoryException {
    if (userContactNode == null) {
      throw new IllegalArgumentException(
          "Node cannot be null to check for connection state");
    }
    ConnectionStates state = ConnectionStates.NONE;
    if (userContactNode.hasProperty(ConnectionConstants.SAKAI_CONNECTION_STATE)) {
      state = ConnectionStates.valueOf(userContactNode.getProperty(
          ConnectionConstants.SAKAI_CONNECTION_STATE).getString());
    }
    return state;
  }

  // SERVICE INTERFACE METHODS

  public String request(Resource resource, String userId, String[] types,
      String requesterUserId) throws ConnectionException {
    // node is the contacts node: e.g. /_user/contacts
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
    String path;
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
            "Invalid requesterUserId specified for connection: "
                + requesterUserId);
      }
      if (!checkValidUserId(session, targetUserId)) {
        throw new ConnectionException(404,
            "Invalid targetUserId specified for connection: " + targetUserId);
      }
      Session adminSession = slingRepository.loginAdministrative(null);
      try {
        // get the contact userstore nodes
        Node requesterStoreNode = ConnectionUtils.getStorageNode(session,
            contactsPath, requesterUserId, false,
            ConnectionConstants.SAKAI_CONTACT_USERSTORE_RT);
        Node targetStoreNode = ConnectionUtils.getStorageNode(session,
            contactsPath, targetUserId, false,
            ConnectionConstants.SAKAI_CONTACT_USERSTORE_RT);
        // get the user contact nodes
        Node requesterNode = ConnectionUtils.getStorageNode(requesterStoreNode
            .getSession(), requesterStoreNode.getPath(), targetUserId, false,
            ConnectionConstants.SAKAI_CONTACT_USERCONTACT_RT);
        Node targetNode = ConnectionUtils.getStorageNode(targetStoreNode
            .getSession(), targetStoreNode.getPath(), requesterUserId, false,
            ConnectionConstants.SAKAI_CONTACT_USERCONTACT_RT);
        // SPECIAL check - if the other user already requested this connection
        // then make both accepted
        ConnectionStates targetState = getConnectionState(targetNode);
        if (ConnectionStates.REQUEST.equals(targetState)) {
          // these users are requesting each other so set states to accept
          requesterNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE,
              ConnectionStates.ACCEPT.toString());
          targetNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE,
              ConnectionStates.ACCEPT.toString());
          // store the types in case they actually matter
          requesterNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES,
              types);
          // no change to the other properties is needed
        } else {
          // now add the properties to indicate the initial states
          requesterNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE,
              ConnectionStates.REQUEST.toString());
          targetNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE,
              ConnectionStates.PENDING.toString());
          requesterNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES,
              types);
          targetNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES,
              types);
          requesterNode.setProperty(
              ConnectionConstants.SAKAI_CONNECTION_REQUESTER, requesterUserId);
          targetNode.setProperty(
              ConnectionConstants.SAKAI_CONNECTION_REQUESTER, requesterUserId);
        }
        path = targetNode.getPath();
        // save changes if any were actually made
        if (adminSession.hasPendingChanges()) {
          adminSession.save();
        }
      } finally {
        // destroy the admin session
        adminSession.logout();
      }
    } catch (RepositoryException e) {
      // general failure
      throw new ConnectionException(500, e.getMessage(), e);
    }
    return path;
  }

  public String connect(Resource resource, String userId,
      ConnectionOperations operation, String requesterUserId)
      throws ConnectionException {
    // TODO Auto-generated method stub
    return null;
  }


  /**
   * @param store
   *          the resource from the request
   * @param userId
   *          id of the user the connection request is directed to
   * @param operation
   *          the connection operation to perform
   * @param types
   *          the types of this connection (e.g. friend of, professor of,
   *          student of)
   * @throws ConnectionException
   */
  // private void handleConnectionOperation(Resource store, String userId,
  // ConnectionOperations operation, String[] types)
  // throws ConnectionException {
  // if (store == null) {
  // throw new IllegalArgumentException("store cannot be null");
  // }
  // if (userId == null || "".equals(userId)) {
  // throw new IllegalArgumentException("userId cannot be null");
  // }
  // if (operation == null) {
  // throw new IllegalArgumentException("operation cannot be null");
  // }
  // try {
  // Session session = store.getResourceResolver().adaptTo(Session.class);
  // String currentUserId = getCurrentUserId(session);
  // if (!checkValidUserId(session, userId)) {
  // throw new ConnectionException(404,
  // "Invalid userId specified for connection: " + userId);
  // }
  // // contacts node
  // Node contactsNode = getStoreNode(session, store);
  // // contacts node of the other user
  // Node otherUserNode = getLocalStoreNode(session, contactsNode.getPath(),
  // userId);
  // // get the state of the connection with the given user for the given node
  // ConnectionStates otherUserState = getConnectionState(otherUserNode,
  // userId);
  // // contacts node of the current user
  // Node curUserNode = getLocalStoreNode(session, contactsNode.getPath(),
  // currentUserId);
  // // state of the node for the current user
  // ConnectionStates curUserState = getConnectionState(curUserNode, userId);
  // if (ConnectionOperations.REQUEST.equals(operation)) {
  // if (ConnectionStates.REQUEST.equals(otherUserState)) {
  // // if we request each other then just accept both
  // updateContactNodes(userId, currentUserId, otherUserNode, curUserNode,
  // ConnectionStates.ACCEPT, ConnectionStates.ACCEPT);
  // } else {
  // if (ConnectionStates.IGNORE.equals(otherUserState)
  // || ConnectionStates.REJECT.equals(otherUserState)
  // || ConnectionStates.BLOCK.equals(otherUserState)) {
  // // do not do anything (I think this is right)
  // } else {
  // // set to pending
  // updateContactNodes(userId, currentUserId, otherUserNode,
  // curUserNode, ConnectionStates.PENDING, ConnectionStates.REQUEST);
  // }
  // }
  //
  // } else if (ConnectionOperations.ACCEPT.equals(operation)) {
  // if (ConnectionStates.REQUEST.equals(otherUserState)) {
  // if (ConnectionStates.PENDING.equals(curUserState)) {
  // updateContactNodes(userId, currentUserId, otherUserNode,
  // curUserNode, ConnectionStates.ACCEPT, ConnectionStates.ACCEPT);
  // }
  // }
  //
  // } else if (ConnectionOperations.IGNORE.equals(operation)) {
  // if (ConnectionStates.REQUEST.equals(otherUserState)) {
  // setConnectionState(curUserNode, ConnectionStates.IGNORE, userId);
  // }
  //
  // } else {
  // // TODO handle the other states
  // throw new IllegalArgumentException(
  // "Don't know how to handle this operation (yet): " + operation);
  // }
  // } catch (RepositoryException e) {
  // throw new ConnectionException(500, e.getMessage(), e);
  // }
  // }

}
