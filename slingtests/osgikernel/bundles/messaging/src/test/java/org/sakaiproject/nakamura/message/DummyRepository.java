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

import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractRepositoryTest;
import org.sakaiproject.nakamura.util.ACLUtils;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Example of writing to a live repo.
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

    Session createSession = getRepoSession();
    AccessControlManager acm = AccessControlUtil.getAccessControlManager(createSession);
    UserManager userManager = AccessControlUtil.getUserManager(createSession);

    // Create a new user.
    User user = createUser("foo", "foo");
    Authorizable au = userManager.getAuthorizable(user.getPrincipal());

    ACLUtils.addEntry("/adminOnly", au, createSession, "d:jcr:read");
    createSession.save();

    // Login as the new user and try to grab the new node.
    Session fooSession = getRepository().login(
        new SimpleCredentials("foo", "foo".toCharArray()));

    try {
      fooSession.getRootNode().getNode("adminOnly");
      fail("This should fail!");
    } catch (PathNotFoundException e) {
      // Since this node is protected, it's expected to throw a PathNotFoundException!
    }
  }

  @Test
  public void testGroupsInGroups() throws RepositoryException {
    startRepository();

    Session adminSession = loginAdministrative();

    // Create a node
    Node rootNode = adminSession.getRootNode();
    rootNode.addNode("groupTest");
    rootNode.save();

    // Create some groups and a user.
    User userInB = createUser("userInB", "userInB");
    Group groupA = createGroup("groupA");
    Group groupB = createGroup("groupB");

    // Add the user to group B and groupB to groupA
    groupB.addMember(userInB);
    groupA.addMember(groupB);

    // Give read to groupA on a path.
    ACLUtils.addEntry("/groupTest", groupA, getRepoSession(), "g:jcr:read");
    getRepoSession().save();

    // Login as userB and check if we can access that node.
    // Login as the new user and try to grab the new node.
    Session fooSession = getRepository().login(
        new SimpleCredentials("userInB", "userInB".toCharArray()));

    try {
      fooSession.getRootNode().getNode("groupTest");
    } catch (PathNotFoundException e) {
      // Since this node is protected, it's expected to throw a PathNotFoundException!
      fail("This should not fail!");
    }
  }

  @Test
  public void testDeniedAccessOnGroups() throws RepositoryException {
    startRepository();

    Session adminSession = loginAdministrative();

    // Create a node
    Node rootNode = adminSession.getRootNode();
    rootNode.addNode("groupTestDeny");
    rootNode.save();

    // Create some groups and a user.
    User userInGroup = createUser("userInGroup", "userInGroup");
    Group groupDeny = createGroup("groupDeny");

    // Add the user to group B and groupB to groupA
    groupDeny.addMember(userInGroup);

    // Give read to groupA on a path.
    ACLUtils.addEntry("/groupTestDeny", groupDeny, getRepoSession(), "d:jcr:read");
    getRepoSession().save();

    // Login as user and check if we can access that node.
    Session fooSession = getRepository().login(
        new SimpleCredentials("userInGroup", "userInGroup".toCharArray()));

    try {
      fooSession.getRootNode().getNode("groupTestDeny");
      fail("This should not fail!");
    } catch (PathNotFoundException e) {
      // Since this node is protected, it's expected to throw a PathNotFoundException!
    }
  }

}
