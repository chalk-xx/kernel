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
package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.accept;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.block;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.cancel;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.ignore;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.invite;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.reject;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.remove;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.ACCEPTED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.BLOCKED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.IGNORED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.INVITED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.NONE;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.PENDING;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.REJECTED;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionOperation;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;




/**
 * Service for doing operations with connections.
 */
@Component(immediate = true, description = "Service for doing operations with connections.", label = "ConnectionSearchResultProcessor")
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
@Service(value = ConnectionManager.class)
public class ConnectionManagerImpl implements ConnectionManager {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionManagerImpl.class);


  @Reference
  protected transient Repository repository;


  private static Map<TransitionKey, StatePair> stateMap = new HashMap<TransitionKey, StatePair>();

  static {
    stateMap.put(tk(NONE, NONE, invite), sp(PENDING, INVITED)); // t1
    stateMap.put(tk(REJECTED, REJECTED, invite), sp(PENDING, INVITED)); // t2
    stateMap.put(tk(PENDING, IGNORED, invite), sp(PENDING, INVITED)); // t3
    stateMap.put(tk(PENDING, INVITED, cancel), sp(NONE, NONE)); // t4
    stateMap.put(tk(PENDING, IGNORED, cancel), sp(NONE, NONE)); // t5
    stateMap.put(tk(PENDING, BLOCKED, cancel), sp(NONE, BLOCKED)); // t6
    stateMap.put(tk(INVITED, PENDING, accept), sp(ACCEPTED, ACCEPTED)); // t7
    stateMap.put(tk(INVITED, PENDING, reject), sp(REJECTED, REJECTED)); // t8
    stateMap.put(tk(INVITED, PENDING, ignore), sp(IGNORED, PENDING)); // t9
    stateMap.put(tk(INVITED, PENDING, block), sp(BLOCKED, PENDING)); // t10
    stateMap.put(tk(ACCEPTED, ACCEPTED, remove), sp(NONE, NONE)); // t11
    stateMap.put(tk(REJECTED, REJECTED, remove), sp(NONE, NONE)); // t12
    stateMap.put(tk(IGNORED, PENDING, remove), sp(NONE, NONE)); // t13
    stateMap.put(tk(PENDING, IGNORED, remove), sp(NONE, NONE)); // t14
    stateMap.put(tk(BLOCKED, PENDING, remove), sp(NONE, NONE)); // t15
    stateMap.put(tk(PENDING, BLOCKED, remove), sp(NONE, BLOCKED)); // t16
    stateMap.put(tk(NONE, BLOCKED, invite), sp(PENDING, BLOCKED)); // t17
    stateMap.put(tk(IGNORED, PENDING, invite), sp(PENDING, INVITED)); // t19
    stateMap.put(tk(INVITED, PENDING, invite), sp(ACCEPTED, ACCEPTED)); // t20
    stateMap.put(tk(NONE, NONE, remove), sp(NONE, NONE)); // t21
    stateMap.put(tk(NONE, BLOCKED, remove), sp(NONE, BLOCKED)); // t22
    stateMap.put(tk(BLOCKED, NONE, remove), sp(NONE, NONE)); // t23
  }

  /**
   * @param pending
   * @param invited
   * @return
   */
  private static StatePair sp(ConnectionState thisState, ConnectionState otherState) {
    return new StatePairFinal(thisState, otherState);
  }

  /**
   * @return
   */
  private static TransitionKey tk(ConnectionState thisState, ConnectionState otherState,
      ConnectionOperation operation) {
    return new TransitionKey(sp(thisState, otherState), operation);
  }

  /**
   * Check to see if a userId is actually a valid one
   *
   * @param session
   *          the JCR session
   * @param userId
   *          the userId to check
   * @return
   */
  protected Authorizable checkValidUserId(Session session, String userId)
      throws ConnectionException {
    Authorizable authorizable;
    if (User.ANON_USER.equals(session.getUserId()) || User.ANON_USER.equals(userId)) {
      throw new ConnectionException(403, "Cant make a connection with anonymous.");
    }
    try {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      authorizable = authorizableManager.findAuthorizable(userId);
      if (authorizable != null && authorizable.getId().equals(userId)) {
        return authorizable;
      }
    } catch (StorageClientException e) {
      // general repo failure
      throw new ConnectionException(500, e.getMessage(), e);
    } catch (Exception e) {
      // other failures return false
      LOGGER.info("Failure checking for valid user (" + userId + "): " + e);
      throw new ConnectionException(404, "User " + userId + " does not exist.");
    }
    throw new ConnectionException(404, "User " + userId + " does not exist.");
  }

  /**
   * Get the connection state from a node
   *
   * @param userContactNode
   *          the node to check (should be a user contact node)
   * @return the connection state (may be NONE)
   * @throws ConnectionException
   */
  protected ConnectionState getConnectionState(Content userContactNode)
      throws ConnectionException {
    if (userContactNode == null) {
      return ConnectionState.NONE;
    }
    try {
      if (userContactNode.hasProperty(ConnectionConstants.SAKAI_CONNECTION_STATE)) {

        return ConnectionState.valueOf((String) userContactNode.getProperty(
            ConnectionConstants.SAKAI_CONNECTION_STATE));
      }
    } catch (Exception e) {
      LOGGER.error(e.getLocalizedMessage(), e);
    }
    return ConnectionState.NONE;
  }

  // SERVICE INTERFACE METHODS

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#connect(org.apache.sling.api.resource.Resource,
   *      java.lang.String,
   *      org.sakaiproject.nakamura.api.connections.ConnectionConstants.ConnectionOperation,
   *      java.lang.String)
   */
  public boolean connect(Map<String, String[]> requestParameters, Resource resource,
      String thisUserId, String otherUserId, ConnectionOperation operation)
      throws ConnectionException {

    Session session = StorageClientUtils.adaptToSession(resource.getResourceResolver().adaptTo(javax.jcr.Session.class));

    if (thisUserId.equals(otherUserId)) {
      throw new ConnectionException(
          400,
          "A user cannot operate on their own connection, this user and the other user are the same");
    }

    // fail if the supplied users are invalid
    Authorizable thisAu = checkValidUserId(session, thisUserId);
    Authorizable otherAu = checkValidUserId(session, otherUserId);

    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();

      // get the contact userstore nodes
      Content thisNode = getOrCreateConnectionNode(adminSession, thisAu, otherAu);
      Content otherNode = getOrCreateConnectionNode(adminSession, otherAu, thisAu);
      if ( thisNode == null ) {
        throw new ConnectionException(400,"Failed to connect users, no connection for "+thisUserId );
      }
      if ( otherNode == null ) {
        throw new ConnectionException(400,"Failed to connect users, no connection for "+otherUserId );
      }

      // check the current states
      ConnectionState thisState = getConnectionState(thisNode);
      ConnectionState otherState = getConnectionState(otherNode);
      StatePair sp = stateMap.get(tk(thisState, otherState, operation));
      if (sp == null) {
        throw new ConnectionException(400, "Cannot perform operation "
            + operation.toString() + " on " + thisState.toString() + ":"
            + otherState.toString());
      }

      // A legitimate invitation can set properties on the invited
      // user's view of the connection, including relationship types
      // that differ from those viewed by the inviting user.
      if (operation == ConnectionOperation.invite) {
        handleInvitation(requestParameters, adminSession, thisNode, otherNode);
      }

      // KERN-763 : Connections need to be "stored" in groups.
      StatePairFinal spAccepted = new StatePairFinal(ACCEPTED, ACCEPTED);
      if (sp.equals(spAccepted)) {
        addUserToGroup(thisAu, otherAu, adminSession);
        addUserToGroup(otherAu, thisAu, adminSession);
        // KERN-1696 make it so that everyone can read someone's list of contacts
        // TODO Should the rule be that reading a contact list is limited to people who are in that contact list?
        AccessControlManager accessControlManager = adminSession.getAccessControlManager();
        AclModification[] aclMods = new AclModification[] { new AclModification(AclModification.grantKey(Group.EVERYONE),
            Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE),
            new AclModification(AclModification.grantKey(User.ANON_USER),
                Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE) };
        accessControlManager.setAcl(Security.ZONE_CONTENT, ConnectionUtils.getConnectionPathBase(thisAu.getId()), aclMods);
        accessControlManager.setAcl(Security.ZONE_CONTENT, ConnectionUtils.getConnectionPathBase(otherAu.getId()), aclMods);
      } else {
        // This might be an existing connection that needs to be removed
        removeUserFromGroup(thisAu, otherAu, adminSession);
        removeUserFromGroup(otherAu, thisAu, adminSession);
      }

      sp.transition(thisNode, otherNode);

      ContentManager contentManager = adminSession.getContentManager();
      contentManager.update(thisNode);
      contentManager.update(otherNode);

      if (operation == ConnectionOperation.invite) {
        throw new ConnectionException(200, "Invitation made between "
            + thisNode.getPath() + " and " + otherNode.getPath());
      }
    } catch (StorageClientException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new ConnectionException(403, e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        // destroy the admin session
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.error(e.getMessage(),e);
        }
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#getConnectionDetails(org.apache.sling.api.SlingHttpServletRequest,
   *      java.lang.String, java.lang.String)
   */
  public Content getConnectionDetails(Session session, String thisUser, String otherUser)
      throws StorageClientException, AccessDeniedException {
    String connPath = ConnectionUtils.getConnectionPath(thisUser, otherUser);
    ContentManager cm = session.getContentManager();

    Content connection = cm.get(connPath);
    return connection;
  }

  /**
   * Removes a member from a group
   *
   * @param thisAu
   *          The {@link Authorizable authorizable} who owns the group.
   * @param otherAu
   *          The {@link Authorizable authorizable} who needs to be removed from the
   *          contact group.
   * @param adminSession
   *          A session that can be used to modify a group.
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   */
  protected void removeUserFromGroup(Authorizable thisAu, Authorizable otherAu,
      Session session) throws StorageClientException, AccessDeniedException {
    if ( otherAu != null && thisAu != null) {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Group g = (Group) authorizableManager.findAuthorizable("g-contacts-" + thisAu.getId());
      g.removeMember(otherAu.getId());
      authorizableManager.updateAuthorizable(g);
    }
  }

  /**
   * Adds one user to another user his connection group.
   *
   * @param thisAu
   *          The base user who is adding a contact.
   * @param otherAu
   *          The user that needs to be added to the group.
   * @param session
   *          The session that can be used to locate and manipulate the group
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   */
  protected void addUserToGroup(Authorizable thisAu, Authorizable otherAu, Session session) throws StorageClientException, AccessDeniedException {
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    Group g = (Group) authorizableManager.findAuthorizable("g-contacts-" + thisAu.getId());
    g.addMember(otherAu.getId());
    authorizableManager.updateAuthorizable(g);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#getConnectedUsers(org.apache.sling.api.SlingHttpServletRequest, java.lang.String, org.sakaiproject.nakamura.api.connections.ConnectionState)
   */
  public List<String> getConnectedUsers(SlingHttpServletRequest request, String user, ConnectionState state) {
    return getConnectedUsers(StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class)), user, state);
  }
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#getConnectedUsers(java.lang.String,
   *      org.sakaiproject.nakamura.api.connections.ConnectionState)
   */
  public List<String> getConnectedUsers(Session session, String user, ConnectionState state) {
    List<String> connections = Lists.newArrayList();
    try {
      ContentManager contentManager = session.getContentManager();
      String path = ConnectionUtils.getConnectionPathBase(user);
      Content content = contentManager.get(path);
      if (content != null) {
        for ( Content connection : content.listChildren() ) {
          String resourceType = (String) connection.getProperty("sling:resourceType");
          String connectionStateValue = (String) connection.getProperty(ConnectionConstants.SAKAI_CONNECTION_STATE);
          ConnectionState connectionState = ConnectionState.NONE;
          if (connectionStateValue != null ) {
            connectionState = ConnectionState.valueOf(connectionStateValue);
          }
          if ( ConnectionConstants.SAKAI_CONTACT_RT.equals(resourceType) && state.equals(connectionState)) {
            connections.add(StorageClientUtils.getObjectName(connection.getPath()));
          }
        }
      }
    } catch (StorageClientException e) {
      throw new IllegalStateException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return connections;
  }

  protected Content getOrCreateConnectionNode(Session session, Authorizable fromUser,
      Authorizable toUser) throws StorageClientException, AccessDeniedException {
    String nodePath = ConnectionUtils.getConnectionPath(fromUser, toUser);
    ContentManager contentManager = session.getContentManager();
    if (!contentManager.exists(nodePath)) {
      // Add auth name for sorting (KERN-1924)
      String firstName = "";
      String lastName = "";
      if (toUser.getProperty("firstName") != null) {
        firstName = (String) toUser.getProperty("firstName");
      }
      if (toUser.getProperty("lastName") != null) {
        lastName = (String) toUser.getProperty("lastName");
      }

      contentManager.update(new Content(nodePath, ImmutableMap.of("sling:resourceType",
          (Object)ConnectionConstants.SAKAI_CONTACT_RT,
            "reference", LitePersonalUtils.getProfilePath(toUser.getId()),
 "sakai:contactstorepath",
          ConnectionUtils.getConnectionPathBase(fromUser), "firstName",
 firstName,
          "lastName", lastName)));
    }
    return contentManager.get(nodePath);
  }

  protected void handleInvitation(Map<String, String[]> requestProperties,
      Session session, Content fromNode, Content toNode)  {
    Set<String> toRelationships = new HashSet<String>();
    Set<String> fromRelationships = new HashSet<String>();
    Map<String, Object> sharedProperties = new HashMap<String, Object>();
    for (Entry<String, String[]> rp : requestProperties.entrySet()) {
      String key = rp.getKey();
      String[] values = rp.getValue();
      if (ConnectionConstants.PARAM_FROM_RELATIONSHIPS.equals(key)) {
        fromRelationships.addAll(Arrays.asList(values));
      } else if (ConnectionConstants.PARAM_TO_RELATIONSHIPS.equals(key)) {
        toRelationships.addAll(Arrays.asList(values));
      } else if (ConnectionConstants.SAKAI_CONNECTION_TYPES.equals(key)) {
        fromRelationships.addAll(Arrays.asList(values));
        toRelationships.addAll(Arrays.asList(values));
      } else {
        if ( values.length == 1 ) {
          sharedProperties.put(key, values[0]);
        } else if ( values.length > 1 ) {
          sharedProperties.put(key, values);
        }
      }
    }
    addArbitraryProperties(fromNode, sharedProperties);
    fromNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES, fromRelationships
        .toArray(new String[fromRelationships.size()]));
    addArbitraryProperties(toNode, sharedProperties);
    toNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES, toRelationships
        .toArray(new String[toRelationships.size()]));
  }

  /**
   * Add property values as individual strings or as string arrays.
   *
   * @param node
   * @param properties
   */
  protected void addArbitraryProperties(Content node, Map<String, Object> properties) {
    for (Entry<String, Object> param : properties.entrySet()) {
        if ( param != null && param.getKey() != null && param.getValue() != null ) {
          node.setProperty(param.getKey(), param.getValue());
        }
    }
  }

}
