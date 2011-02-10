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
package org.sakaiproject.nakamura.connections.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionFinderSearchResultProcessorTest {

  @Mock
  ProfileService profileService;

  @Mock
  SolrSearchServiceFactory searchServiceFactory;

  @Mock
  Session session;

  @Mock
  ContentManager cm;

  @Test
  public void test() throws Exception {
    ConnectionFinderSearchResultProcessor processor = new ConnectionFinderSearchResultProcessor();
    processor.searchServiceFactory = searchServiceFactory;
    processor.profileService = profileService;

    Object hybridSession = mock(javax.jcr.Session.class, withSettings()
        .extraInterfaces(SessionAdaptable.class));

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    AuthorizableManager am = mock(AuthorizableManager.class);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(javax.jcr.Session.class)).thenReturn((javax.jcr.Session) hybridSession);
    when(((SessionAdaptable) hybridSession).getSession()).thenReturn(session);
    when(session.getAuthorizableManager()).thenReturn(am);
    when(session.getContentManager()).thenReturn(cm);

    when(request.getRemoteUser()).thenReturn("alice");
    Authorizable auAlice = mock(Authorizable.class);
    when(auAlice.getId()).thenReturn("alice");

    Authorizable auBob = mock(Authorizable.class);
    when(auBob.getId()).thenReturn("bob");
    HashMap<String, Object> auProps = new HashMap<String, Object>();
    auProps.put("lastName", "The Builder");
    when(auBob.getSafeProperties()).thenReturn(auProps);
    when(profileService.getCompactProfileMap(auBob, (javax.jcr.Session) hybridSession)).thenReturn(new ValueMapDecorator(auProps));

    when(am.findAuthorizable("alice")).thenReturn(auAlice);
    when(am.findAuthorizable("bob")).thenReturn(auBob);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);

    Result result = mock(Result.class);
    when(result.getPath()).thenReturn("a:alice/contacts/bob");
    when(result.getFirstValue(User.NAME_FIELD)).thenReturn("bob");

    Content contactNode = new Content("a:alice/contacts/bob", null);
    contactNode.setProperty("sling:resourceType", "sakai/contact");
    contactNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE,
        ConnectionState.ACCEPTED.toString());
    when(cm.get("a:alice/contacts/bob")).thenReturn(contactNode);

    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    processor.writeResult(request, write, result);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    assertEquals("bob", o.getString("target"));
    assertEquals("The Builder", o.getJSONObject("profile").getString("lastName"));
    assertEquals("sakai/contact", o.getJSONObject("details").getString(
        "sling:resourceType"));
  }
}
