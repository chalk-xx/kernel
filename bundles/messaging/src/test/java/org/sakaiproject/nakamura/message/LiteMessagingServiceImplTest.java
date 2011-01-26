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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 *
 */
public class LiteMessagingServiceImplTest {
  private LiteMessagingServiceImpl messagingServiceImpl;
  @Mock
  private Session session;
  @Mock
  private ContentManager contentManager;
  @Mock
  private LockManager lockManager;

  private String userName = "joe";
  private String groupName = "g-physics-101-viewers";

  public LiteMessagingServiceImplTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setUp() throws Exception {
    when(session.getContentManager()).thenReturn(contentManager);
    messagingServiceImpl = new LiteMessagingServiceImpl();
    messagingServiceImpl.lockManager = lockManager;
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testFullPathToStoreSite() {
    // Groups
    String path = messagingServiceImpl.getFullPathToStore(groupName, session);
    assertEquals("a:g-physics-101-viewers/message/", path);

    // Users
    path = messagingServiceImpl.getFullPathToStore(userName, session);
    assertEquals("a:joe/message/", path);
  }

  @Test
  public void testGetFullPathToMessage() {
    String messageId = "cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c";
    String path = messagingServiceImpl.getFullPathToMessage(userName, messageId, session);
    assertEquals(
        "a:joe/message/inbox/cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c",
        path);
  }

  @Test
  public void testCreate() throws LockTimeoutException, RepositoryException {
    when(session.getUserId()).thenReturn("joe");
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put("num", 10L);
    mapProperties.put("s", "foobar");
    String messageId = "foo";
    Content result = messagingServiceImpl.create(session, mapProperties, messageId);
    assertEquals("foo", result.getProperty(MessageConstants.PROP_SAKAI_ID));
    assertEquals(10L, StorageClientUtils.toLong(result.getProperty("num")));
    assertEquals("foobar", result.getProperty("s"));
  }

  @Test
  public void testCreateFail() throws Exception {
    when(session.getUserId()).thenReturn("joe");
    Mockito.doThrow(new StorageClientException("Big mess! (Unit Test Generated Exception, its Ok)")).when(contentManager).update(Mockito.any(Content.class));
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    String messageId = "foo";
    try {
      messagingServiceImpl.create(session, mapProperties, messageId);
      fail("This should have thrown a Messageexception");
    } catch (MessagingException e) {
      assertEquals("Unable to save message.", e.getMessage());
    }
  }
}
