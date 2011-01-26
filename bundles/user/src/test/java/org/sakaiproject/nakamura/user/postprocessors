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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import org.apache.sling.servlets.post.Modification;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;

public class LiteMessageAuthorizablePostProcessorTest {

  @Test
  public void testNotAUser() throws Exception {
    Session session = mock(Session.class);
    ContentManager contentManager = mock(ContentManager.class);
    when(session.getContentManager()).thenReturn(contentManager);
    AccessControlManager accessControlManager = mock(AccessControlManager.class);
    when(session.getAccessControlManager()).thenReturn(accessControlManager);
    Group group = new Group(ImmutableMap.of(Authorizable.ID_FIELD, (Object)"joe"));
    LiteMessagingService messagingService = mock(LiteMessagingService.class);
    when(messagingService.getFullPathToStore("joe", session)).thenReturn("~joe/messages");

    LiteMessageAuthorizablePostProcessor proc = new LiteMessageAuthorizablePostProcessor();
    proc.messagingService = messagingService;

    proc.process(group, session, Modification.onCreated("~joe"), null);
    verify(contentManager).update(any(Content.class));
    verify(accessControlManager).setAcl(anyString(), anyString(), any(AclModification[].class));
  }
}
