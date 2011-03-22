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
package org.sakaiproject.nakamura.message.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.message.LiteMessagingServiceImpl;
import org.sakaiproject.nakamura.message.search.MessageSearchPropertyProvider;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MessageSearchPropertyProviderTest extends AbstractEasyMockTest {

  @Test
  public void testProperties() throws Exception {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    Repository repository = baseMemoryRepository.getRepository();
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);

    ResourceResolver resolver = mock(ResourceResolver.class);
    JackrabbitSession jackrabbitSession = mock(JackrabbitSession.class);
    when(resolver.adaptTo(javax.jcr.Session.class)).thenReturn(jackrabbitSession);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRemoteUser()).thenReturn("admin");

    Authorizable au = createAuthorizable("admin", false, true);
    UserManager um = createUserManager(null, true, au);
    when(jackrabbitSession.getUserManager()).thenReturn(um);

    // Special requests
    RequestParameter fromParam = mock(RequestParameter.class);
    when(fromParam.getString()).thenReturn("usera,userb");
    when(request.getRequestParameter("_from")).thenReturn(fromParam);

    Map<String, String> pMap = new HashMap<String, String>();

    MessageSearchPropertyProvider provider = new MessageSearchPropertyProvider();
    LiteMessagingService messagingService = new LiteMessagingServiceImpl();
    provider.messagingService = messagingService;
    provider.loadUserProperties(request, pMap);
    provider.messagingService = null;

    assertEquals(
        ClientUtils.escapeQueryChars(LitePersonalUtils.PATH_AUTHORIZABLE
            + "admin/message/")
            + "*", pMap.get(MessageConstants.SEARCH_PROP_MESSAGESTORE));

    assertEquals("from:(\"usera\" OR \"userb\")", pMap.get("_from"));
  }

}
