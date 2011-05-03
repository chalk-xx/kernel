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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

/**
 *
 */
public class RepositoryBaseTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBaseTest.class);
  private static BundleContext bundleContext;
  private static RepositoryBase repositoryBase;

  public static RepositoryBase getRepositoryBase() throws IOException,
      RepositoryException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      ClassNotFoundException {
    if (repositoryBase == null) {
      bundleContext = Mockito.mock(BundleContext.class);
      repositoryBase = new RepositoryBase(bundleContext);
      repositoryBase.start();
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

        public void run() {
          if (repositoryBase != null) {
            repositoryBase.stop();
            repositoryBase = null;
          }
        }
      }));
    }
    return repositoryBase;
  }

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAnonLoginStartup() throws LoginException, RepositoryException,
      IOException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      ClassNotFoundException {
    Repository repo = getRepositoryBase().getRepository();
    Session session = null;
    try {
      LOGGER.info("Opening Anon Session ");
      session = repo.login();
      LOGGER.info("Done Opening Anon Session ");
      Node rootNode = session.getRootNode();
      Assert.assertNotNull(rootNode);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @Test
  public void testAdminLoginStartup() throws LoginException, RepositoryException,
      IOException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      ClassNotFoundException {
    Repository repo = getRepositoryBase().getRepository();
    Session session = null;
    try {
      LOGGER.info("Opening Admin Session ");
      session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
      LOGGER.info("Done Opening Admin Session ");
      Assert.assertEquals("admin", session.getUserID());
      Node rootNode = session.getRootNode();
      Assert.assertNotNull(rootNode);
      Node testNode = rootNode.addNode("testNode");
      Assert.assertNotNull(testNode);
      session.save();
      session.logout();
      LOGGER.info("Opening admin session ");
      session = repo.login();
      LOGGER.info("Done Opening admin session ");
      rootNode = session.getRootNode();
      Assert.assertNotNull(rootNode);
      Assert.assertTrue(session.itemExists("/testNode"));
      Item item = session.getItem("/testNode");
      Assert.assertNotNull(item);
      Assert.assertTrue(item.isNode());
      Assert.assertEquals("/testNode", item.getPath());
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @Test
  public void testStandardPrincipal() throws LoginException, RepositoryException,
      IOException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      ClassNotFoundException {
    Repository repo = getRepositoryBase().getRepository();
    JackrabbitSession session = null;
    try {
      LOGGER.info("Opening Admin Session ");
      session = (JackrabbitSession) repo.login(new SimpleCredentials("admin", "admin"
          .toCharArray()));
      LOGGER.info("Done Opening Admin Session ");
      PrincipalManager principalManager = session.getPrincipalManager();

      Principal principal = principalManager.getPrincipal("admin");
      Assert.assertNotNull(principal);
      Assert.assertTrue(principal instanceof AdminPrincipal);
      Assert.assertEquals("admin", principal.getName());

      principal = principalManager.getPrincipal("everyone");
      Assert.assertNotNull(principal);
      Assert.assertEquals("everyone", principal.getName());

      principal = principalManager.getPrincipal("anonymous");
      Assert.assertNotNull(principal);
      Assert.assertTrue(principal instanceof AnonymousPrincipal);
      Assert.assertEquals("anonymous", principal.getName());

      /*
       * principal = principalManager.getPrincipal("Administrators");
       * Assert.assertNotNull(principal); Assert.assertEquals("Administrators",
       * principal.getName());
       */

    } finally {
      session.logout();
    }
  }




  @Test
  public void testUserAccessControl() throws LoginException, RepositoryException,
      IOException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      ClassNotFoundException {
    Repository repo = getRepositoryBase().getRepository();
    JackrabbitSession session = null;
    try {
      LOGGER.info("Opening Admin Session ");
      session = (JackrabbitSession) repo.login(new SimpleCredentials("admin", "admin"
          .toCharArray()));
      LOGGER.info("Done Opening Admin Session ");
      UserManager userManager = session.getUserManager();
      String testUser = "testUser" + System.currentTimeMillis();
      String testViewerUser = "testViewerUser" + System.currentTimeMillis();
      String testManagerUser = "testManagerUser" + System.currentTimeMillis();
      userManager.createUser(testUser, "testpassword");
      userManager.createUser(testViewerUser, "testpassword");
      userManager.createUser(testManagerUser, "testpassword");
      Principal groupPrincipal = new Principal() {
        private String groupName = "group" + System.currentTimeMillis();

        public String getName() {
          return groupName;
        }
      };
      Principal group2Principal = new Principal() {
        private String groupName = "group2-" + System.currentTimeMillis();

        public String getName() {
          return groupName;
        }
      };

      Group g1 = userManager.createGroup(groupPrincipal);
      Group g2 = userManager.createGroup(group2Principal);
      g1.setProperty("rep:group-managers", new Value[] { session.getValueFactory()
          .createValue("dummy") });
      g2.setProperty("rep:group-managers", new Value[] { session.getValueFactory()
          .createValue(testManagerUser) });
      g2.setProperty("rep:group-viewers", new Value[] { session.getValueFactory()
          .createValue(testViewerUser) });
      // we need to be able to add properties to the group somehow.

      if (session.hasPendingChanges()) {
        session.save();
      }
      session.logout();

      session = (JackrabbitSession) repo.login(new SimpleCredentials(testUser,
          "testpassword".toCharArray()));
      userManager = session.getUserManager();
      User testUserU = (User) userManager.getAuthorizable(testUser);
      testUserU.setProperty("mytestvale", session.getValueFactory()
          .createValue("testing"));
      if (session.hasPendingChanges()) {
        session.save();
      }
      testUserU = (User) userManager.getAuthorizable(testViewerUser);
      try {
        testUserU.setProperty("mytestvale",
            session.getValueFactory().createValue("cant-do-this-wrong-user"));
        Assert.fail();
      } catch (AccessDeniedException e) {
        // Ok
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
      Group group1 = (Group) userManager.getAuthorizable(groupPrincipal);
      Value[] v = group1.getProperty("rep:group-managers");
      Assert.assertNotNull(v);
      Assert.assertEquals(1, v.length);
      Assert.assertEquals("dummy", v[0].getString());
      try {
        group1.setProperty("mytestvale",
            session.getValueFactory().createValue("cant-do-this-wrong-user"));
        Assert.fail();
      } catch (AccessDeniedException e) {
        // Ok
      }
      if (session.hasPendingChanges()) {
        session.save();
      }

      session.logout();

      session = (JackrabbitSession) repo.login(new SimpleCredentials(testUser,
          "testpassword".toCharArray()));
      userManager = session.getUserManager();

      System.err.println(testUser + " Trying to get Authorizable for "
          + group2Principal.getName());
      Group group2 = (Group) userManager.getAuthorizable(group2Principal);
      Assert.assertNull(group2);// should not have been able to read group2.

      session.logout();

      session = (JackrabbitSession) repo.login(new SimpleCredentials(testViewerUser,
          "testpassword".toCharArray()));
      userManager = session.getUserManager();
      System.err.println(testViewerUser + " Trying to get Authorizable for "
          + group2Principal.getName());
      group2 = (Group) userManager.getAuthorizable(group2Principal);
      Assert.assertNotNull(group2);
      v = group2.getProperty("rep:group-managers");
      Assert.assertNotNull(v);
      Assert.assertEquals(1, v.length);
      Assert.assertEquals(testManagerUser, v[0].getString());

      try {
        group2.setProperty("mytestvale",
            session.getValueFactory().createValue("cant-do-this-wrong-user"));
        Assert.fail();
      } catch (AccessDeniedException e) {
        // Ok
      }

      session.logout();

      session = (JackrabbitSession) repo.login(new SimpleCredentials(testManagerUser,
          "testpassword".toCharArray()));
      userManager = session.getUserManager();
      group2 = (Group) userManager.getAuthorizable(group2Principal);
      Assert.assertNotNull(group2); // should be able to see the group
      v = group2.getProperty("rep:group-managers");
      Assert.assertNotNull(v);
      Assert.assertEquals(1, v.length);
      Assert.assertEquals(testManagerUser, v[0].getString());
      group2.setProperty("mytestvale",
          session.getValueFactory().createValue("cant-do-this-wrong-user"));

    } finally {
      session.logout();
    }

  }

}
