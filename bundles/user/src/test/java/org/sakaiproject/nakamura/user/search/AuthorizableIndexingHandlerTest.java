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
package org.sakaiproject.nakamura.user.search;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthorizableIndexingHandlerTest {

  @Mock
  private RepositorySession repoSession;

  @Mock
  private Session session;

  @Mock
  private AuthorizableManager authzMgr;

  private Authorizable user1;
  private AuthorizableIndexingHandler handler;

  @Before
  public void setUp() throws Exception {
    handler = new AuthorizableIndexingHandler();
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(Authorizable.ID_FIELD, "user1");
    user1 = new Authorizable(props);

    when(repoSession.adaptTo(Session.class)).thenReturn(session);
    when(session.getAuthorizableManager()).thenReturn(authzMgr);
    when(authzMgr.findAuthorizable(anyString())).thenReturn(user1);
  }

  @Test
  public void delete() {
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put("userid", "user1");
    Event event = new Event(StoreListener.DELETE_TOPIC, props);

    Collection<String> queries = handler.getDeleteQueries(repoSession, event);

    assertNotNull(queries);
    assertEquals(1, queries.size());
    assertEquals("id:" + ClientUtils.escapeQueryChars("user1"), queries.iterator().next());
  }

  @Test
  public void deleteExcluded() {
    user1.setProperty(UserConstants.SAKAI_EXCLUDE, "true");

    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put("path", "user1");
    Event event = new Event(StoreListener.UPDATED_TOPIC, props);

    Collection<String> queries = handler.getDeleteQueries(repoSession, event);

    assertNotNull(queries);
    assertEquals(1, queries.size());
  }

  @Test
  public void doNotIndexExcluded() {
    user1.setProperty(UserConstants.SAKAI_EXCLUDE, "true");

    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put("path", "user1");
    Event event = new Event(StoreListener.UPDATED_TOPIC, props);

    Collection<SolrInputDocument> queries = handler.getDocuments(repoSession, event);

    assertNotNull(queries);
    assertEquals(0, queries.size());
  }

  @Test
  public void doNotdeleteNonExcluded() {
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put("path", "user1");
    Event event = new Event(StoreListener.UPDATED_TOPIC, props);

    Collection<String> queries = handler.getDeleteQueries(repoSession, event);

    assertNotNull(queries);
    assertEquals(0, queries.size());
  }
}
