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
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseMapUserManager;
import org.sakaiproject.nakamura.message.internal.LiteInternalMessageHandler;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageSearchResultProcessorTest {

  private MessageSearchResultProcessor proc;

  @Mock
  private LiteMessagingService messagingService;

  @Mock
  private ResourceResolver resolver;

  @Mock
  private Session session;
  
  @Mock
  private ContentManager contentManager;
  
  @Mock
  private JackrabbitSession jcrSession;
  
  @Mock
  private SparseMapUserManager userManager;

  @Mock
  private SlingHttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    proc = new MessageSearchResultProcessor();
    proc.messagingService = messagingService;

    when(request.getResourceResolver()).thenReturn(resolver);
    when(jcrSession.getUserManager()).thenReturn(userManager);
    when(userManager.getSession()).thenReturn(session);
    when(resolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);

    LiteInternalMessageHandler handler = new LiteInternalMessageHandler();
    proc.bindWriters(handler);
  }

  @After
  public void tearDown() {
    proc.messagingService = null;
  }

  @Test
  public void testProc() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    ExtendedJSONWriter write = new ExtendedJSONWriter(w);

    // We handle a previous msg.
    String previousId = "prevId";
    String userID = "john";
    String pathToStore = "/path/to/store/";
    String pathToPrevMsg = "/path/to/store/outbox/" + previousId;
    Content previousMsg = createDummyMessage(previousId);
    when(request.getRemoteUser()).thenReturn(userID);
    when(session.getUserId()).thenReturn(userID);
    when(session.getContentManager()).thenReturn(contentManager);
    when(contentManager.exists(pathToPrevMsg)).thenReturn(Boolean.TRUE);
    when(contentManager.get(pathToPrevMsg)).thenReturn(previousMsg);
    when(messagingService.getFullPathToStore(userID, session)).thenReturn(
        pathToStore);

    Content resultNode = createDummyMessage("msgid");
    resultNode.setProperty(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE, previousId);
    proc.writeContent(request, write, resultNode);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    assertEquals(o.getString("id"), "msgid");
    assertEquals(o.getString(MessageConstants.PROP_SAKAI_MESSAGEBOX),
        MessageConstants.BOX_INBOX);
    assertEquals(2, o.getJSONArray("foo").length());

    assertEquals(previousId, o.getString(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE));

    JSONObject prev = o.getJSONObject("previousMessage");
    assertEquals(prev.getString("id"), previousId);
  }

  private Content createDummyMessage(String msgID) {
    Content c = new Content("/path/to/store/" + msgID, null);
    c.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_INBOX);
    c.setProperty("foo", new String[] { "a", "b" });
    return c;
  }

}
