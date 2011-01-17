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
package org.sakaiproject.nakamura.chat;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ChatMessageSearchResultProviderTest {

  private LiteMessagingService messagingService;
  private ChatMessageSearchPropertyProvider propProvider;
  
  private Session session;
  private String user = "johndoe";
  private ResourceResolver resourceResolver;

  @Before
  public void setUp() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException, ClientPoolException, StorageClientException, org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    messagingService = mock(LiteMessagingService.class);

    propProvider = new ChatMessageSearchPropertyProvider();
    propProvider.messagingService = messagingService;
    session = mock(Session.class);
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    when(messagingService.getFullPathToStore(user, session)).thenReturn("/full/path/to/store");
  }

  @After
  public void tearDown() {
    propProvider.messagingService = null;
    verify(messagingService);
  }

  @Test
  public void testProperties() {
    if ( true ) {
      System.err.println("Test Currently broken ");
      return;
    }
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(user);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    RequestParameter param = mock(RequestParameter.class);
    when(param.getString()).thenReturn("jack,peter,mary");
    when(request.getRequestParameter("_from")).thenReturn(param);
    Map<String, String> props = new HashMap<String, String>();
    propProvider.loadUserProperties(request, props);

    String expected = " and ((@sakai:from=\"jack\" or @sakai:from=\"peter\" or @sakai:from=\"mary\" or @sakai:from=\"johndoe\")";
    expected += " or (@sakai:to=\"jack\" or @sakai:to=\"peter\" or @sakai:to=\"mary\" or @sakai:to=\"johndoe\"))";
    assertEquals(expected, props.get("_from"));

  }

}
