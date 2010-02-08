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
package org.sakaiproject.nakamura.docproxy;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class MetadataServletTest extends AbstractDocProxyServlet {

  private ExternalDocumentMetadataServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new ExternalDocumentMetadataServlet();
    servlet.activate(componentContext);
    servlet.tracker = tracker;
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testGet() throws ServletException, IOException, PathNotFoundException,
      RepositoryException, JSONException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    expect(response.getWriter()).andReturn(printWriter);

    replay();

    servlet.doGet(request, response);
    printWriter.flush();

    String jsonString = baos.toString("UTF-8");
    JSONObject o = new JSONObject(jsonString);

    JSONObject properties = o.getJSONObject("properties");
    assertEquals(123, properties.get("num"));
    assertEquals("bar", properties.get("foo"));

  }
}
