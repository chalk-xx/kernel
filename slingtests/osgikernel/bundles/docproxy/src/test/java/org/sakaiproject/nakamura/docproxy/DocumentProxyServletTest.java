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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class DocumentProxyServletTest extends AbstractDocProxyServlet {

  private ExternalDocumentProxyServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new ExternalDocumentProxyServlet();
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

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream stream = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    expect(response.getOutputStream()).andReturn(stream);
    replay();

    servlet.doGet(request, response);

    String result = baos.toString("UTF-8");
    assertEquals("K2 docProxy test resource", result);

  }

  @Test
  public void testNoProcessor() throws PathNotFoundException, RepositoryException,
      ServletException, IOException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    Node node = createMock(Node.class);
    // We use the default disk processor.
    Property processorProperty = createMock(Property.class);
    expect(processorProperty.getString()).andReturn("foo");

    // Our resource type
    Property resourceTypeProp = createMock(Property.class);
    expect(resourceTypeProp.getString()).andReturn(
        DocProxyConstants.RT_EXTERNAL_REPOSITORY);

    expect(node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true);
    expect(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(resourceTypeProp);
    expect(node.getProperty(REPOSITORY_PROCESSOR)).andReturn(processorProperty);
    expect(node.getPath()).andReturn("/docproxy/disk");
    expect(node.isNode()).andReturn(true);

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(node);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown repository.");

    replay();

    servlet.doGet(request, response);

  }
}
