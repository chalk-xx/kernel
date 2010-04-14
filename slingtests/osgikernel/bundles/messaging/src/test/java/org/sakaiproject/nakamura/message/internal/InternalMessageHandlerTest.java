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
package org.sakaiproject.nakamura.message.internal;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.message.listener.MessageRoutesImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 *
 */
public class InternalMessageHandlerTest {

  private InternalMessageHandler handler;
  private MessagingService messagingService;
  private SlingRepository slingRepository;
  private Session session;

  @Before
  public void setUp() throws Exception {
    messagingService = mock(MessagingService.class);
    slingRepository = mock(SlingRepository.class);
    session = mock(Session.class);
    handler = new InternalMessageHandler();
    handler.messagingService = messagingService;
    handler.slingRepository = slingRepository;
  }

  @Test
  public void testHandle() throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException {
    String path = "/path/to/msg";
    String newPath = "/path/to/new/msg";

    // Original message created to send
    Node originalMessage = new MockNode(path);
    originalMessage.setProperty(MessageConstants.PROP_SAKAI_TO, "internal:admin");
    originalMessage.setProperty(MessageConstants.PROP_SAKAI_ID, "foo");

    Node newNode = new MockNode(newPath);
    Node newNodeParent = new MockNode(newPath.substring(0, newPath.lastIndexOf("/")));
    Workspace space = mock(Workspace.class);
    when(session.getWorkspace()).thenReturn(space);

    when(session.itemExists(newNodeParent.getPath())).thenReturn(true);
    when(session.getItem(newNodeParent.getPath())).thenReturn(newNodeParent);
    when(session.itemExists(newPath)).thenReturn(true);
    when(session.getItem(newPath)).thenReturn(newNode);

    when(slingRepository.loginAdministrative(null)).thenReturn(session);

    when(messagingService.getFullPathToMessage("admin", "foo", session)).thenReturn(
        newPath);

    MessageRoutes routes = new MessageRoutesImpl(originalMessage);

    handler.send(routes, null, originalMessage);

    assertEquals(false, newNode.getProperty(MessageConstants.PROP_SAKAI_READ)
        .getBoolean());
    assertEquals(MessageConstants.BOX_INBOX, newNode.getProperty(
        MessageConstants.PROP_SAKAI_MESSAGEBOX).getString());
    assertEquals(MessageConstants.STATE_NOTIFIED, newNode.getProperty(
        MessageConstants.PROP_SAKAI_SENDSTATE).getString());

  }
}
