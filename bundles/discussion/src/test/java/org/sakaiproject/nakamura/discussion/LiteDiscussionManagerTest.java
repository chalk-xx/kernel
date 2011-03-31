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
package org.sakaiproject.nakamura.discussion;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.discussion.LiteDiscussionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.io.IOException;
import java.util.Map;

import javax.jcr.RepositoryException;

@RunWith(MockitoJUnitRunner.class)
public class LiteDiscussionManagerTest {

  @Mock
  private Session session;

  @Mock
  private ContentManager cm;

  @Before
  public void setUp() throws Exception {
    when(session.getContentManager()).thenReturn(cm);
  }

  /**
   * Login as administrator
   *
   * @return Returns the administrator session.
   * @throws LoginException
   * @throws RepositoryException
   * @throws IOException
   */
//  private Session loginAsAdmin() throws LoginException, RepositoryException, IOException {
//    return getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
//  }

  @Test
  public void testFindSettings() throws Exception {
    // Add a couple of nodes
    Content settingsNode = new Content("/settingsNode", null);
    settingsNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/settings");
    settingsNode.setProperty("sakai:marker", "foo");
    settingsNode.setProperty("sakai:type", "discussion");
    when(cm.find(isA(Map.class))).thenReturn(Lists.immutableList(settingsNode));

    LiteDiscussionManager manager = new LiteDiscussionManagerImpl();
    Content result = manager.findSettings("foo", session, "discussion");

    assertNotNull(result);
    assertEquals("/settingsNode", result.getPath());
  }

  @Test
  public void testFindMessage() throws Exception {
    // Add a couple of nodes
    Content messagesNode = new Content("/messages", null);
    Content msgNode = new Content("/messages/msgNodeCorrect", null);
    msgNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode.setProperty("sakai:marker", "foo");
    msgNode.setProperty("sakai:type", "discussion");
    msgNode.setProperty("sakai:id", "10");

    Content msgNode2 = new Content("/messages/msgNodeCorrect2", null);
    msgNode2.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode2.setProperty("sakai:marker", "foo");
    msgNode2.setProperty("sakai:type", "discussion");
    msgNode2.setProperty("sakai:id", "20");

    Content randomNode = new Content("/messages/foo", null);
    randomNode.setProperty("foo", "bar");

    when(cm.find(isA(Map.class))).thenReturn(Lists.immutableList(msgNode));
    LiteDiscussionManager manager = new LiteDiscussionManagerImpl();
    Content result = manager.findMessage("10", "foo", session, "/messages");

    assertNotNull(result);
    assertEquals("/messages/msgNodeCorrect", result.getPath());
  }
}
