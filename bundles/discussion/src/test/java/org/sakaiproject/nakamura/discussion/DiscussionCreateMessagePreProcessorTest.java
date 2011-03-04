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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.PROP_MARKER;
import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.PROP_REPLY_ON;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessagingException;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscussionCreateMessagePreProcessorTest {

  private DiscussionCreateMessagePreProcessor processor;
  @Mock
  private DiscussionManager discussionManager;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private Session session;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Before
  public void setUp() throws Exception {
    processor = new DiscussionCreateMessagePreProcessor();
    processor.bindDiscussionManager(discussionManager);
  }

  @Test
  public void testMarker() {
    setupBaseRequest();

    RequestParameter paramMarker = mock(RequestParameter.class);
    when(paramMarker.getString()).thenReturn("123");
    when(request.getRequestParameter(PROP_MARKER)).thenReturn(paramMarker);
    when(request.getRequestParameter(PROP_REPLY_ON)).thenReturn(null);

    processor.checkRequest(request);
  }

  @Test
  public void testNoMarker() {
    setupBaseRequest();

    when(request.getRequestParameter(PROP_MARKER)).thenReturn(null);

    try {
      processor.checkRequest(request);
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
    }
  }

  @Test
  public void testMarkerAndValidReplyOn() {
    setupBaseRequest();
    String marker = "id123";
    String messageId = "messageId132";

    RequestParameter paramMarker = mock(RequestParameter.class);
    when(paramMarker.getString()).thenReturn(marker);

    RequestParameter paramReplyOn = mock(RequestParameter.class);
    when(paramReplyOn.getString()).thenReturn(messageId);

    when(request.getRequestParameter(PROP_MARKER)).thenReturn(paramMarker);
    when(request.getRequestParameter(PROP_REPLY_ON)).thenReturn(paramReplyOn);

    Content messageNode = new Content("/_user/message/bla", null);

    // session is null because we don't mock the adaptTo call
    when(discussionManager.findMessage(messageId, marker, null, "/_user/message"))
        .thenReturn(messageNode);

    processor.checkRequest(request);
  }

  @Test
  public void testMarkerAndNonValidReplyOn() {
    setupBaseRequest();
    String marker = "id123";
    String messageId = "messageId132";

    when(request.getParameter(PROP_MARKER)).thenReturn(marker);
    when(request.getParameter(PROP_REPLY_ON)).thenReturn(messageId);
    when(discussionManager.findMessage(messageId, marker, session, "/_user/message"))
        .thenReturn(null);

    try {
      processor.checkRequest(request);
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
    }
  }

  /**
   * 
   */
  private void setupBaseRequest() {
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getResourcePath()).thenReturn("/_user/message");

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestPathInfo()).thenReturn(pathInfo);
  }

}
