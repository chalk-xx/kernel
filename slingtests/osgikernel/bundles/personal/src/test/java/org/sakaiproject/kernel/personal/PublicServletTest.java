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
package org.sakaiproject.kernel.personal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.sakaiproject.kernel.api.user.UserFactoryService;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

/**
 * 
 */
public class PublicServletTest {


  @Test
  public void testHashRequest() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    UserFactoryService userFactoryService = createMock(UserFactoryService.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Resource finalResource = createMock(Resource.class);
    RequestDispatcher dispatcher = createMock(RequestDispatcher.class);
    
    Resource resource = new NonExistingResource(resourceResolver, PublicServlet._USER_PUBLIC+"/ieb/testpath.tidy.json");
   
    
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.resolve("/_user/public/ea/89/fa/4f/ieb/testpath.tidy.json")).andReturn(finalResource);
    expect(request.getRequestDispatcher(finalResource)).andReturn(dispatcher);
    dispatcher.forward(request,response);
    expectLastCall();
    
    replay(request,response, userFactoryService,resourceResolver, finalResource, dispatcher);
     
    PublicServlet ps = new PublicServlet();
    ps.bindUserFactoryService(userFactoryService);
    
    ps.hashRequest(request, response);
    verify(request,response, userFactoryService, resourceResolver, finalResource, dispatcher);
  }
  
  
  @Test
  public void testNonHashRequest() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    UserFactoryService userFactoryService = createMock(UserFactoryService.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Resource finalResource = createMock(Resource.class);
    RequestDispatcher dispatcher = createMock(RequestDispatcher.class);
    
    Resource resource = new NonExistingResource(resourceResolver, PublicServlet._USER_PUBLIC+"/ieb/testpath2.tidy.json");
    Resource nonExistentResource = new NonExistingResource(resourceResolver,"/_user/private/ed/fe/33/ieb/testpath2.tidy.json");
    
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.resolve("/_user/public/ea/89/fa/4f/ieb/testpath2.tidy.json")).andReturn(nonExistentResource);
    response.sendError(404, "Resource does not exist (non existant)");
    expectLastCall();
    replay(request,response, userFactoryService,resourceResolver, finalResource, dispatcher);
     
    PublicServlet ps = new PublicServlet();
    ps.bindUserFactoryService(userFactoryService);
    
    ps.hashRequest(request, response);
    verify(request,response, userFactoryService, resourceResolver, finalResource, dispatcher);
  }
  
  @Test
  public void testNullHashRequest() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    UserFactoryService userFactoryService = createMock(UserFactoryService.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Resource finalResource = createMock(Resource.class);
    RequestDispatcher dispatcher = createMock(RequestDispatcher.class);
    
    Resource resource = new NonExistingResource(resourceResolver, PublicServlet._USER_PUBLIC+"/ieb/testpath3.tidy.json");
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.resolve("/_user/public/ea/89/fa/4f/ieb/testpath3.tidy.json")).andReturn(null);
    response.sendError(404, "Resource does not exist (null)");
    expectLastCall();
    replay(request,response, userFactoryService,resourceResolver, finalResource, dispatcher);
     
    PublicServlet ps = new PublicServlet();
    ps.bindUserFactoryService(userFactoryService);
    
    ps.hashRequest(request, response);
    verify(request,response, userFactoryService, resourceResolver, finalResource, dispatcher);
  }

}
