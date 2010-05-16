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
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipal;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleACLModifier;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

/**
 *
 */
public class RepositoryBaseTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBaseTest.class);
  private static final long ADAY = 3600000L * 24L;
  private static BundleContext bundleContext;
  private static RepositoryBase repositoryBase;

  private static RepositoryBase getRepositoryBase() throws IOException,
      RepositoryException {
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
      IOException {
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
      IOException {
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
      IOException {
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
      Assert.assertTrue(principal instanceof ItemBasedPrincipal);
      ItemBasedPrincipal ibp = (ItemBasedPrincipal) principal;
      Assert.assertEquals("admin", ibp.getName());

      principal = principalManager.getPrincipal("everyone");
      Assert.assertNotNull(principal);
      Assert.assertEquals("everyone", principal.getName());

      principal = principalManager.getPrincipal("anonymous");
      Assert.assertNotNull(principal);
      Assert.assertTrue(principal instanceof ItemBasedPrincipal);
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
  public void testAcePrincipal() throws LoginException, RepositoryException, IOException {
    Repository repo = getRepositoryBase().getRepository();
    JackrabbitSession session = null;
    try {
      LOGGER.info("Opening Admin Session ");
      session = (JackrabbitSession) repo.login(new SimpleCredentials("admin", "admin"
          .toCharArray()));
      LOGGER.info("Done Opening Admin Session ");
      PrincipalManager principalManager = session.getPrincipalManager();

      Principal principal = principalManager.getPrincipal(RulesBasedAce.createPrincipal(
          "ieb").getName());
      Assert.assertNotNull(principal);
      Assert.assertTrue(principal instanceof RulesPrincipal);
      RulesPrincipal rp = (RulesPrincipal) principal;
      Assert.assertEquals("ieb", rp.getPrincipalName());

    } finally {
      session.logout();
    }
  }

  @Test
  public void testAddRuleBasedPrincipal() throws RepositoryException, IOException {
    Repository repo = getRepositoryBase().getRepository();
    JackrabbitSession session = null;
    try {
      LOGGER.info("Opening Admin Session ");
      session = (JackrabbitSession) repo.login(new SimpleCredentials("admin", "admin"
          .toCharArray()));
      LOGGER.info("Done Opening Admin Session ");
      PrincipalManager principalManager = session.getPrincipalManager();

      Node node = session.getRootNode().addNode("testnodeace");
      AccessControlManager accessControlManager = session.getAccessControlManager();
      String resourcePath = node.getPath();

      AccessControlList acl = null;
      AccessControlPolicy[] policies = accessControlManager.getPolicies(resourcePath);
      for (AccessControlPolicy policy : policies) {
        if (policy instanceof AccessControlList) {
          acl = (AccessControlList) policy;
          break;
        }
      }
      if (acl == null) {
        AccessControlPolicyIterator applicablePolicies = accessControlManager
            .getApplicablePolicies(resourcePath);
        while (applicablePolicies.hasNext()) {
          AccessControlPolicy policy = applicablePolicies.nextAccessControlPolicy();
          if (policy instanceof AccessControlList) {
            acl = (AccessControlList) policy;
            break;
          }
        }
      }
      Assert.assertNotNull(acl);

      Principal principal = principalManager.getPrincipal(RulesBasedAce.createPrincipal(
          "ieb").getName());

      Assert.assertNotNull(principal);
      Assert.assertTrue(principal instanceof RulesPrincipal);
      RulesPrincipal rp = (RulesPrincipal) principal;
      Assert.assertEquals("ieb", rp.getPrincipalName());

      Privilege[] privileges = new Privilege[] { accessControlManager
          .privilegeFromName("jcr:write") };

      acl.addAccessControlEntry(principal, privileges);

      accessControlManager.setPolicy(resourcePath, acl);

      // make the ACL a rules based.
      RuleACLModifier ruleAclModifier = new RuleACLModifier();
      Map<String, Object> ruleProperties = new HashMap<String, Object>();

      ValueFactory vf = session.getValueFactory();

      long now = System.currentTimeMillis();
      String[] range = new String[4];
      for (int i = 0; i < 4; i++) {
        GregorianCalendar start = new GregorianCalendar();
        start.setTimeInMillis((ADAY * i) + now - 3600000L);
        GregorianCalendar end = new GregorianCalendar();
        end.setTimeInMillis((ADAY * i) + now + 3600000L);
        range[i] = start.toString() + "/" + end.toString();
      }

      Value[] ranges = new Value[] { vf.createValue(range[1]), vf.createValue(range[2]),
          vf.createValue(range[3]) };
      ruleProperties.put(RulesBasedAce.P_ACTIVE_RANGE, vf.createValue(range[0]));

      Property[] p = ruleAclModifier.setProperties(resourcePath, session, principal,
          ruleProperties);
      Assert.assertEquals(1, p.length);
      Assert.assertEquals(RulesBasedAce.P_ACTIVE_RANGE, p[0].getName());
      Assert.assertEquals(range[0], p[0].getString());

      ruleProperties.put(RulesBasedAce.P_ACTIVE_RANGE, ranges);
      p = ruleAclModifier.setProperties(resourcePath, session, principal, ruleProperties);
      Assert.assertEquals(3, p.length);
      for (int i = 0; i < 3; i++) {
        Assert.assertEquals(RulesBasedAce.P_ACTIVE_RANGE + i, p[i].getName());
        Assert.assertEquals(range[i + 1], p[i].getString());
      }
    } finally {
      session.logout();
    }

  }

  @Test
  public void testUserAccessControl() throws LoginException, RepositoryException,
      IOException {
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
        testUserU.setProperty("mytestvale", session.getValueFactory().createValue(
            "cant-do-this-wrong-user"));
        Assert.fail();
      } catch (AccessDeniedException e) {
        // Ok
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
      Group group1 = (Group) userManager.getAuthorizable(groupPrincipal);
      Value [] v = group1.getProperty("rep:group-managers");
      Assert.assertNotNull(v);
      Assert.assertEquals(1,v.length);
      Assert.assertEquals("dummy", v[0].getString());
      try {
        group1.setProperty("mytestvale", session.getValueFactory().createValue(
            "cant-do-this-wrong-user"));
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

      System.err.println("Trying to get Authorizable for "+group2Principal.getName());
      Group group2 = (Group) userManager.getAuthorizable(group2Principal);
      Assert.assertNull(group2);// should not have been able to read group2.
  
      session.logout();

      session = (JackrabbitSession) repo.login(new SimpleCredentials(testViewerUser,
          "testpassword".toCharArray()));
      userManager = session.getUserManager();
      group2 = (Group) userManager.getAuthorizable(group2Principal);
      v = group2.getProperty("rep:group-managers");
      Assert.assertNotNull(v);
      Assert.assertEquals(1,v.length);
      Assert.assertEquals(testManagerUser, v[0].getString());

      try {
        group2.setProperty("mytestvale", session.getValueFactory().createValue(
            "cant-do-this-wrong-user"));
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
      Assert.assertEquals(1,v.length);
      Assert.assertEquals(testManagerUser, v[0].getString());
      group2.setProperty("mytestvale", session.getValueFactory().createValue(
          "cant-do-this-wrong-user"));

    } finally {
      session.logout();
    }

  }

}
