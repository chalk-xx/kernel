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
package org.sakaiproject.nakamura.resource;

import static org.easymock.EasyMock.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.easymock.Capture;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;


/**
 *
 */
public class AbstractVirtualPathServletTest extends AbstractEasyMockTest {

  private SlingHttpServletResponse response;
  private SlingHttpServletRequest request;
  private VirtualResourceProvider vp;
  private Resource baseResource;

  @Test
  public void testGet() throws ServletException, IOException, PathNotFoundException, RepositoryException {
    start("GET");
    replay();
    
    TVirtualPathServlet vps = new TVirtualPathServlet(vp);
    vps.doGet(request, response);
    verify();
  }
  @Test
  public void testDelete() throws ServletException, IOException, PathNotFoundException, RepositoryException {
    start("DELETE");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    expect(baseResource.getResourceMetadata()).andReturn(resourceMetadata);
    Capture<Resource> captureResouce = new Capture<Resource>();
    RequestDispatcher requestDispatcher = createNiceMock(RequestDispatcher.class);
    expect(request.getRequestDispatcher(capture(captureResouce))).andReturn(requestDispatcher);
    requestDispatcher.forward(request, response);
    expectLastCall();
    replay();
    
    TVirtualPathServlet vps = new TVirtualPathServlet(vp);
    vps.doDelete(request, response);
    verify();
  }

  /**
   * @throws RepositoryException 
   * @throws PathNotFoundException 
   * 
   */
  private void start(String method) throws PathNotFoundException, RepositoryException {
    vp = createNiceMock(VirtualResourceProvider.class);
    request = createNiceMock(SlingHttpServletRequest.class);
    response = createNiceMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Session session = createMock(Session.class); 
    baseResource = createMock(Resource.class);
    Node node = createNiceMock(Node.class);
    
    expect(request.getResource()).andReturn(baseResource).anyTimes();
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(baseResource.getPath()).andReturn("/a/b/c").anyTimes();
    expect(session.getItem("/a/b/c")).andThrow(new PathNotFoundException());
    expect(session.getItem("/a/b")).andReturn(node).anyTimes();
    expect(node.isNode()).andReturn(true).anyTimes();
    expect(node.getPath()).andReturn("/a/b").anyTimes();
    expect(request.getMethod()).andReturn(method).anyTimes(); 
  }
}
