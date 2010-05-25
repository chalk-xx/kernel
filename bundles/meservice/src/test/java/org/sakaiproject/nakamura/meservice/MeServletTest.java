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
package org.sakaiproject.nakamura.meservice;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class MeServletTest extends AbstractEasyMockTest {

  @Test
  public void testGeneralInfoAdmin() throws JSONException, UnsupportedEncodingException, RepositoryException {

    MeServlet servlet = new MeServlet();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    ExtendedJSONWriter write = new ExtendedJSONWriter(w);

    Authorizable user = createAuthorizable("admin", false, true);
    
    Set<String> subjects = new HashSet<String>();
    subjects.add("administrators");
    Map<String, Object> properties = new HashMap<String, Object>();

    write.object();
    servlet.writeGeneralInfo(write, user, subjects, properties);
    write.endObject();

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertEquals("admin", j.getString("userid"));
    assertEquals(true, j.getBoolean("superUser"));
    assertEquals("a/ad/admin/", j.getString("userStoragePrefix"));
    assertEquals(1, j.getJSONArray("subjects").length());
  }

  @Test
  public void testWriteLocale() throws JSONException, UnsupportedEncodingException {
    MeServlet servlet = new MeServlet();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    ExtendedJSONWriter write = new ExtendedJSONWriter(w);

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("locale", "en_US");
    properties.put("timezone", "America/Los_Angeles");

    write.object();
    servlet.writeLocale(write, properties);
    write.endObject();
    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);
    JSONObject locale = j.getJSONObject("locale");
    assertEquals("US", locale.getString("country"));
    assertEquals("USA", locale.getString("ISO3Country"));
    assertEquals("en", locale.getString("language"));
    assertEquals("USA", locale.getString("ISO3Country"));
    assertEquals("USA", locale.getString("ISO3Country"));
    JSONObject timezone = locale.getJSONObject("timezone");
    assertEquals("America/Los_Angeles", timezone.getString("name"));
  }

  @Test
  public void testAnon() throws RepositoryException, JSONException, ServletException,
      IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    Node profileNode = createMock(Node.class);
    
    Authorizable au = createAuthorizable(UserConstants.ANON_USERID, false, true);
    UserManager um = createUserManager(null, true, au);
    
    String profilePath = PersonalUtils.getProfilePath(au);
    PropertyIterator propIterator = createMock(PropertyIterator.class);
    NodeIterator nodeIterator = createMock(NodeIterator.class);
    expect(propIterator.hasNext()).andReturn(false);
    expect(nodeIterator.hasNext()).andReturn(false);
    expect(profileNode.getNodes()).andReturn(nodeIterator);
    expect(profileNode.getProperties()).andReturn(propIterator);
    expect(profileNode.getName()).andReturn("authprofile").anyTimes();
    expect(profileNode.getPath()).andReturn("/path/to/authprofile").anyTimes();

    JackrabbitSession session = createMock(JackrabbitSession.class);
    expect(session.getItem(profilePath)).andReturn(profileNode).anyTimes();
    expect(session.getUserID()).andReturn(UserConstants.ANON_USERID).anyTimes();
    expect(session.getUserManager()).andReturn(um).anyTimes();

    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver).anyTimes();
    expect(response.getWriter()).andReturn(w);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    replay();

    MeServlet servlet = new MeServlet();

    servlet.doGet(request, response);
    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s).getJSONObject("user");

    assertEquals(true, j.getBoolean("anon"));
    assertEquals(false, j.getBoolean("superUser"));
    assertEquals(0, j.getJSONArray("subjects").length());
  }

  @Test
  public void testExceptions() throws IOException, ServletException,
      PathNotFoundException, RepositoryException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver);
    Authorizable au = createAuthorizable(UserConstants.ANON_USERID, false, true);
    String profilePath = PersonalUtils.getProfilePath(au);
    expect(session.getUserID()).andReturn(UserConstants.ANON_USERID).anyTimes();
    expect(session.getItem(profilePath)).andThrow(new RepositoryException());

    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    expect(response.getWriter()).andReturn(w);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "Failed to get the profile node.");
    replay();

    MeServlet servlet = new MeServlet();

    servlet.doGet(request, response);
  }
}
