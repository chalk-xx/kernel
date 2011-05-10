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
package org.sakaiproject.nakamura.image;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseMapUserManager;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CropItServletTest extends AbstractEasyMockTest {

  private CropItServlet servlet;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new CropItServlet();
  }

  @Test
  public void testProperPost() throws ServletException, IOException, RepositoryException,
      JSONException, AccessDeniedException, StorageClientException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    Session mockSession = mock(Session.class);
    ContentManager contentManager = mock(ContentManager.class);
    when(mockSession.getContentManager()).thenReturn(contentManager);
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(mockSession);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);
    expect(request.getResourceResolver()).andReturn(resourceResolver);

    // Provide parameters
    String[] dimensions = new String[] { "16x16", "32x32" };
    addStringRequestParameter(request, "img", "/~johndoe/people.png");
    addStringRequestParameter(request, "save", "/~johndoe/breadcrumbs");
    addStringRequestParameter(request, "x", "10");
    addStringRequestParameter(request, "y", "10");
    addStringRequestParameter(request, "width", "70");
    addStringRequestParameter(request, "height", "70");
    addStringRequestParameter(request, "dimensions", StringUtils.join(dimensions, 0, ';'));
    expect(request.getRemoteUser()).andReturn("johndoe");


    String imagePath = "a:johndoe/people.png";
    when(contentManager.getInputStream(imagePath)).thenReturn(getClass().getClassLoader().getResourceAsStream("people.png"));
    when(contentManager.get(anyString())).thenReturn(new Content("foo", null));

    SparseContentResource someResource = mock(SparseContentResource.class);
    when(someResource.adaptTo(Content.class)).thenReturn(new Content(imagePath, ImmutableMap.of("mimeType", (Object)"image/png", "_bodyLocation", "2011/lt/zz/x8")));
    JackrabbitSession jrSession = mock(JackrabbitSession.class);
    SparseMapUserManager userManager = mock(SparseMapUserManager.class);
    when(userManager.getSession()).thenReturn(mockSession);
    when(jrSession.getUserManager()).thenReturn(userManager);
    when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jrSession);
    when(resourceResolver.getResource(anyString())).thenReturn(someResource);

    // Capture output.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter write = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(write);

    replay();
    servlet.doPost(request, response);
    write.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    JSONArray files = o.getJSONArray("files");
    assertEquals(2, files.length());
    for (int i = 0; i < files.length(); i++) {
      String url = files.getString(i);
      assertEquals("/~johndoe/breadcrumbs/" + dimensions[i] + "_people.png", url);
    }
  }

  @Test
  public void testAnon() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRemoteUser()).andReturn("anonymous");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Anonymous user cannot crop images.");
    replay();
    servlet.doPost(request, response);
  }

  @Test
  public void testMissingParameters() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRequestParameter("img")).andReturn(null);
    expect(request.getRemoteUser()).andReturn("johndoe");
    addStringRequestParameter(request, "save", null);
    addStringRequestParameter(request, "x", null);
    addStringRequestParameter(request, "y", null);
    addStringRequestParameter(request, "width", null);
    addStringRequestParameter(request, "height", null);
    addStringRequestParameter(request, "dimensions", "");

    response
        .sendError(HttpServletResponse.SC_BAD_REQUEST,
            "The following parameters are required: img, save, x, y, width, height, dimensions");

    replay();

    servlet.doPost(request, response);
  }

  @Test
  public void testImageException() {
    ImageException e = new ImageException(500, "foo");
    assertEquals(500, e.getCode());
    assertEquals("foo", e.getMessage());

    e = new ImageException();
    e.setCode(500);
    assertEquals(500, e.getCode());

  }

  @Test
  public void testCheck() {
    int val = servlet.checkIntBiggerThanZero(5, 1);
    assertEquals(5, val);

    val = servlet.checkIntBiggerThanZero(0, 1);
    assertEquals(0, val);

    val = servlet.checkIntBiggerThanZero(-5, 1);
    assertEquals(1, val);
  }

}
