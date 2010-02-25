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

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingService;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

/**
 *
 */
public class MessageSearchPropertyProviderTest {

  @Test
  public void testProperties() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);

    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRemoteUser()).thenReturn("admin");

    // Special requests
    RequestParameter fromParam = mock(RequestParameter.class);
    when(fromParam.getString()).thenReturn("usera,userb");
    when(request.getRequestParameter("_from")).thenReturn(fromParam);

    Map<String, String> pMap = new HashMap<String, String>();

    MessageSearchPropertyProvider provider = new MessageSearchPropertyProvider();
    MessagingService messagingService = new MessagingServiceImpl();
    provider.bindMessagingService(messagingService);
    provider.loadUserProperties(request, pMap);
    provider.unbindMessagingService(messagingService);

    assertEquals("/_user/message/d0/_x0033_3/e2/_x0032_a/admin", pMap
        .get(MessageConstants.SEARCH_PROP_MESSAGESTORE));

    assertEquals(" and (@sakai:from=\"usera\" or @sakai:from=\"userb\")", pMap
        .get("_from"));
  }

}
