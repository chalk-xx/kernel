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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LiteMessagePostProcessorTest {
  private LiteMessagePostProcessor processor;
  private MockEventAdmin eventAdmin;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private Session session;
  @Mock
  private ContentManager contentManager;
  @Mock
  private ResourceResolver resourceResolver;

  public LiteMessagePostProcessorTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setUp() throws StorageClientException {
    eventAdmin = new MockEventAdmin();

    processor = new LiteMessagePostProcessor();
    processor.eventAdmin = eventAdmin;

    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Resource contentResource = new SparseContentResource(new Content("dummy",null), session, resourceResolver);
    when(request.getResource()).thenReturn(contentResource);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(session.getContentManager()).thenReturn(contentManager);
  }

  @After
  public void tearDown() {
    processor.eventAdmin = null;
  }

  @Test
  public void testNoModification() throws Exception {
    Modification mod = new Modification(ModificationType.MOVE, "/from", "/to");
    List<Modification> changes = new ArrayList<Modification>();
    changes.add(mod);

    processor.process(request, changes);

    List<Event> events = eventAdmin.getEvents();
    assertEquals(0, events.size());
  }

  @Test
  public void testMessageModification() throws Exception {
    String path = "/path/to/message";
    String modificationPath = path + "/" + MessageConstants.PROP_SAKAI_MESSAGEBOX;
    Modification mod = new Modification(ModificationType.MODIFY, modificationPath,
        modificationPath);
    List<Modification> changes = new ArrayList<Modification>();
    changes.add(mod);

    Content content = new Content(path, null);
    content.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    content.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_OUTBOX);
    content.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
        MessageConstants.STATE_PENDING);
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Content.class)).thenReturn(content);
    when(resourceResolver.getResource(path)).thenReturn(resource);

    when(contentManager.exists(path)).thenReturn(true);
    when(contentManager.get(path)).thenReturn(content);
    when(request.getRemoteUser()).thenReturn("johndoe");

    processor.process(request, changes);

    List<Event> events = eventAdmin.getEvents();
    assertEquals(1, events.size());
    Event event = events.get(0);
    assertEquals(MessageConstants.PENDINGMESSAGE_EVENT, event.getTopic());
    String location = (String) event.getProperty(MessageConstants.EVENT_LOCATION);
    assertEquals(path, location);

  }

}
