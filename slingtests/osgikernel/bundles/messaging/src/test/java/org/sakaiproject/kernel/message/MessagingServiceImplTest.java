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
package org.sakaiproject.kernel.message;

import static org.junit.Assert.fail;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class MessagingServiceImplTest {

  private MessagingServiceImpl messagingServiceImpl;
  private SiteService siteService;
  private Session session;

  private String siteName = "physics-101";
  private String sitePath = "/sites/" + siteName;
  private String siteNameException = "fubar";

  @Before
  public void setUp() throws Exception {
    Node siteNode = createMock(Node.class);
    expect(siteNode.getPath()).andReturn(sitePath);
    replay(siteNode);

    session = createMock(Session.class);
    replay(session);

    siteService = createMock(SiteService.class);
    expect(siteService.findSiteByName(session, siteName)).andReturn(siteNode)
        .anyTimes();
    expect(siteService.findSiteByName(session, siteNameException)).andThrow(
        new SiteException(400, "fubar")).anyTimes();
    replay(siteService);

    messagingServiceImpl = new MessagingServiceImpl();
    messagingServiceImpl.bindSiteService(siteService);
  }

  @After
  public void tearDown() {
    messagingServiceImpl.unbindSiteService(siteService);
    verify(siteService);
  }

  @Test
  public void testFullPathToStoreSite() {
    // Sites
    String path = messagingServiceImpl.getFullPathToStore("s-physics-101",
        session);
    assertEquals(sitePath + "/store", path);

    // Groups
    path = messagingServiceImpl.getFullPathToStore("g-physics-101-viewers",
        session);
    assertEquals("/_group/message/10/41/8e/87/g_physics_101_viewers", path);

    // Users
    path = messagingServiceImpl.getFullPathToStore("admin", session);
    assertEquals("/_user/message/d0/33/e2/2a/admin", path);

    // Site Exception
    try {
      path = messagingServiceImpl.getFullPathToStore("s-" + siteNameException,
          session);
      fail("This should fail.");
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
      assertEquals("fubar", e.getMessage());
    }
  }

  @Test
  public void testIsMessageStore() throws ValueFormatException,
      RepositoryException {
    Node node = createMock(Node.class);
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn(MessageConstants.SAKAI_MESSAGESTORE_RT);
    expect(node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(prop);
    expect(node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(true);

    replay(prop, node);

    boolean isNode = messagingServiceImpl.isMessageStore(node);
    assertEquals(true, isNode);

    Node randNode = createMock(Node.class);
    expect(
        randNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(false);
    replay(randNode);
    isNode = messagingServiceImpl.isMessageStore(randNode);
    assertEquals(false, isNode);

    Node excepNode = createMock(Node.class);
    expect(
        excepNode
            .hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andThrow(new RepositoryException());
    isNode = messagingServiceImpl.isMessageStore(excepNode);
    assertEquals(false, isNode);
  }

  @Test
  public void testGetFullPathToMessage() {
    String rcpt = "admin";
    String messageId = "cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c";
    String path = messagingServiceImpl.getFullPathToMessage(rcpt, messageId,
        session);
    assertEquals(
        "/_user/message/d0/33/e2/2a/admin/1a/62/60/12/cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c",
        path);
  }

  @Test
  public void testGetURItoStore() {
    // Site
    String path = messagingServiceImpl.getUriToStore("s-" + siteName, session);
    assertEquals(sitePath + "/store", path);

    // Groups
    path = messagingServiceImpl.getUriToStore("g-foo", session);
    assertEquals("/_group/message/g-foo", path);

    // User
    path = messagingServiceImpl.getUriToStore("admin", session);
    assertEquals("/_user/message/admin", path);    
  }
}
