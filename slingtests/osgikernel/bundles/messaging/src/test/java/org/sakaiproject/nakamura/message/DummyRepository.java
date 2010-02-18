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
package org.sakaiproject.nakamura.message;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractRepositoryTest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 *
 */
public class DummyRepository extends AbstractRepositoryTest {

  @Test
  public void testACLstuff() throws RepositoryException {
    startRepository();
    Session adminSession = loginAdministrative();

    Node rootNode = adminSession.getRootNode();
    Node node = rootNode.addNode("adminOnly");
    node.setProperty("prop", "bla");
    rootNode.save();

    UserManager userManager = AccessControlUtil.getUserManager(adminSession);
    userManager.createUser("foo", "foo");

    Session fooSession = getRepository().login(
        new SimpleCredentials("foo", "foo".toCharArray()));

    Node test = fooSession.getRootNode().getNode("adminOnly");

  }
}
