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
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.LiteDiscussionManager;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessageConstants;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LiteDiscussionRouterTest {

  @Mock
  Repository repository;
  
  @Mock
  LiteDiscussionManager discussionManager;

  @Mock
  Session session;

  @Test
  public void testDelivery() throws Exception {

    when(repository.loginAdministrative()).thenReturn(session);

    Content settingsNode = new Content("/sites/bla/_widgets/12345/settings", null);
    settingsNode.setProperty(DiscussionConstants.PROP_NOTIFICATION, true);
    settingsNode.setProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS, "admin");

    // Mock the discussion manager
    when(discussionManager.findSettings("12345", session, "discussion")).thenReturn(
        settingsNode);

    MockMessageRoutes routing = new MockMessageRoutes();

    Content c = new Content("/sites/bla/store/foomessage", null);
    c.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    c.setProperty(MessageConstants.PROP_SAKAI_TO, "discussion:s-foo");
    c.setProperty(DiscussionConstants.PROP_MARKER, "12345");

    LiteDiscussionRouter router = new LiteDiscussionRouter(discussionManager, repository);
    router.route(c, routing);

    assertEquals(1, routing.size());
    assertEquals("internal:admin", routing.get(0).getTransport() + ":"
        + routing.get(0).getRcpt());
  }

}
