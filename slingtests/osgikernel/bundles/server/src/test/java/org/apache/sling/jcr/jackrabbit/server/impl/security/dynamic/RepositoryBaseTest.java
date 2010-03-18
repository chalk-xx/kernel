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
import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipal;
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

import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 *
 */
public class RepositoryBaseTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBaseTest.class);
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

}
