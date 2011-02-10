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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.RepositoryImpl;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class ConnectionManagerImplTest {

  private ConnectionManagerImpl connectionManager;
  private RepositoryImpl repository;

  @Before
  public void setUp() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    repository = baseMemoryRepository.getRepository();
    connectionManager = new ConnectionManagerImpl();
    connectionManager.repository = repository;
  }

  @Test
  public void testAddArbitraryProperties() throws ClientPoolException, StorageClientException, AccessDeniedException {
    Session session = repository.loginAdministrative();
    session.getContentManager().update(new Content("/path/to/connection/node", null));  
    Content node = session.getContentManager().get("/path/to/connection/node");
    
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("alfa", new String[] { "a" });
    properties.put("beta", new String[] { "a", "b" });
    properties.put("charlie", "c");

    connectionManager.addArbitraryProperties(node, properties);
    
    Assert.assertArrayEquals((String[])node.getProperty("alfa"), new String[]{"a"});
    Assert.assertArrayEquals((String[])node.getProperty("beta"), new String[]{"a","b"});
    assertEquals(node.getProperty("charlie"), "c");
  }

  @Test
  public void testHandleInvitation() throws ClientPoolException, StorageClientException, AccessDeniedException  {
    Session session = repository.loginAdministrative();
    ContentManager contentManager = session.getContentManager();
    contentManager.update(new Content("a:alice/contacts/bob", null));  
    Content fromNode = contentManager.get("a:alice/contacts/bob");
    contentManager.update(new Content("a:bob/contacts/alice", null));  
    Content toNode = contentManager.get("a:bob/contacts/alice");

    Map<String, String[]> props = new HashMap<String, String[]>();

    props.put(ConnectionConstants.PARAM_FROM_RELATIONSHIPS, new String[] { "Supervisor",
        "Lecturer" });
    props.put(ConnectionConstants.PARAM_TO_RELATIONSHIPS, new String[] { "Supervised",
        "Student" });
    props.put(ConnectionConstants.SAKAI_CONNECTION_TYPES, new String[] { "foo" });
    props.put("random", new String[] { "israndom" });

    connectionManager.handleInvitation(props, null, fromNode, toNode);

    String[] fromValues = (String[]) fromNode.getProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES);

    String[] toValues = (String[]) toNode.getProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES);

    assertEquals(3, fromValues.length);
    int j = 0;
    // order may not be what we expect it to be
    for ( int i = 0; i < 3; i++ ) {
      if ( "foo".equals(fromValues[i])) {
        j = j|1;
      }
      if ( "Lecturer".equals(fromValues[i])) {
        j = j|2;
      }
      if ( "Supervisor".equals(fromValues[i])) {
        j = j|4;
      }
    }

    Assert.assertTrue((j&1)==1);
    Assert.assertTrue((j&2)==2);
    Assert.assertTrue((j&4)==4);
    assertEquals(3, toValues.length);

    j = 0;
    for ( int i = 0; i < 3; i++ ) {
      if ( "foo".equals(toValues[i])) {
        j = j|1;
      }
      if ( "Student".equals(toValues[i])) {
        j = j|2;
      }
      if ( "Supervised".equals(toValues[i])) {
        j = j|4;
      }
    }
    Assert.assertTrue((j&1)==1);
    Assert.assertTrue((j&2)==2);
    Assert.assertTrue((j&4)==4);


    String fromRandomValues = (String) fromNode.getProperty("random");
    String toRandomValues =  (String) toNode.getProperty("random");

    assertEquals("israndom", fromRandomValues);
    assertEquals("israndom", toRandomValues);
  }

  @Test
  public void testCheckValidUserIdAnon() {
    Session session = mock(Session.class);
    try {
      connectionManager.checkValidUserId(session, UserConstants.ANON_USERID);
      fail("This should've thrown a ConnectionException.");
    } catch (ConnectionException e) {
      assertEquals(403, e.getCode());
    }
  }

  @Test
  public void testCheckValidUserIdNonExisting() throws AccessDeniedException, StorageClientException  {
    Session session = repository.loginAdministrative();
    session.getAuthorizableManager().createUser("bob", "bob", "test", null);
    session.logout();
    session = repository.loginAdministrative("bob");
    
    try {
      connectionManager.checkValidUserId(session, "alice");
      fail("This should've thrown a ConnectionException.");
    } catch (ConnectionException e) {
      assertEquals(404, e.getCode());
    }
  }

  @Test
  public void testCheckValidUserId() throws ConnectionException, ClientPoolException, StorageClientException, AccessDeniedException {
    Session session = repository.loginAdministrative();
    session.getAuthorizableManager().createUser("bob", "bob", "test", null);
    session.getAuthorizableManager().createUser("alice", "alice", "test", null);
    session.logout();
    session = repository.loginAdministrative("bob");
    Authorizable actual = connectionManager.checkValidUserId(session, "alice");
    assertEquals("alice", actual.getId());
  }

  @Test
  public void testGetConnectionState() throws ConnectionException, ClientPoolException, StorageClientException, AccessDeniedException {
    // Passing in null
    try {
      connectionManager.getConnectionState(null);
      fail("Passing in null should result in exception.");
    } catch (IllegalArgumentException e) {
      // Swallow it and continue with the test.
    }

    Session session = repository.loginAdministrative();
    session.getContentManager().update(new Content("/path/to/connection/node", null));  
    Content node = session.getContentManager().get("/path/to/connection/node");

    ConnectionState state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.NONE, state);

    // Passing in node with state property.
    node.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE, "ACCEPTED");
    state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.ACCEPTED, state);

    // Passing in node with wrong state property.
    node.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE, "fubar");
    state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.NONE, state);
  }

  @Test
  public void testDeepGetCreateNodeExisting() throws ClientPoolException, StorageClientException, AccessDeniedException {
    Session session = repository.loginAdministrative();
    session.getAuthorizableManager().createUser("bob", "bob", "test", null);
    session.getAuthorizableManager().createUser("alice", "alice", "test", null);
    session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, "a:alice",new AclModification[]{
        new AclModification(AclModification.grantKey("alice"), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
    });
    session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, "a:bob",new AclModification[]{
        new AclModification(AclModification.grantKey("bob"), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
    });
    session.logout();
    session = repository.loginAdministrative("bob");
    Authorizable from = session.getAuthorizableManager().findAuthorizable("bob");
    Authorizable to = session.getAuthorizableManager().findAuthorizable("alice");
    
    Content result = connectionManager.getOrCreateConnectionNode(session, from, to);
    assertEquals("a:bob/contacts/alice", result.getPath());
  }

  @Test
  public void testDeepGetCreateNodeExistingBase() throws AccessDeniedException, StorageClientException  {
    Session session = repository.loginAdministrative();
    session.getAuthorizableManager().createUser("bob", "bob", "test", null);
    session.getAuthorizableManager().createUser("alice", "alice", "test", null);
    session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, "a:alice",new AclModification[]{
        new AclModification(AclModification.grantKey("alice"), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
    });
    session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, "a:bob",new AclModification[]{
        new AclModification(AclModification.grantKey("bob"), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
    });
    session.logout();
    session = repository.loginAdministrative("bob");
    Authorizable from = session.getAuthorizableManager().findAuthorizable("bob");
    Authorizable to = session.getAuthorizableManager().findAuthorizable("alice");


    Content result = connectionManager.getOrCreateConnectionNode(session, from, to);
    assertEquals("a:bob/contacts/alice", result.getPath());
    assertEquals(ConnectionConstants.SAKAI_CONTACT_RT, result.getProperty("sling:resourceType"));
    assertEquals("a:alice/public/authprofile", result.getProperty("reference"));
  }
}
