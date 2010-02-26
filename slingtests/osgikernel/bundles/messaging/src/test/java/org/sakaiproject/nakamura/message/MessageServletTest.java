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

import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessagingService;

import javax.jcr.Session;

/**
 *
 */
public class MessageServletTest {

  private MessageServlet servlet;
  private MessagingService messagingService;

  @Before
  public void setUp() {
    servlet = new MessageServlet();
    messagingService = mock(MessagingService.class);
    servlet.messagingService = messagingService;
  }

  @Test
  public void testTargetPath() {

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestPathInfo info = mock(RequestPathInfo.class);
    when(info.getResourcePath()).thenReturn("/_user/message/foo");
    when(request.getRequestPathInfo()).thenReturn(info);
    when(request.getRemoteUser()).thenReturn("admin");

    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);

    when(messagingService.getFullPathToMessage("admin", "foo", session)).thenReturn(
        "/_user/message/d0/33/a2/2e/admin/f/o/o/f/foo");

    String result = servlet.getTargetPath(null, request, null, null, null);
    assertEquals("/_user/message/d0/33/a2/2e/admin/f/o/o/f/foo", result);
  }
}
