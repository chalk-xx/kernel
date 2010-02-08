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
package org.sakaiproject.nakamura.personal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * 
 */
public class PublicServletTest {


  @Test
  public void testGetTargetPath() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    Resource resource = createMock(Resource.class);   
    
    replay(request,resource);
     
    PublicServlet ps = new PublicServlet();
    
    Assert.assertEquals("/test/da/39/a3/ee",ps.getTargetPath(resource, request, response, "/test", ""));
    Assert.assertEquals("/test/da/39/a3/ee",ps.getTargetPath(resource, request, response, "/test", "/"));
    Assert.assertEquals("/test/9a/67/47/fc/sdf",ps.getTargetPath(resource, request, response, "/test", "/sdf"));
    Assert.assertEquals("/test/9a/67/47/fc/sdf",ps.getTargetPath(resource, request, response, "/test", "sdf"));
    Assert.assertEquals("/test/0b/1f/ec/29/sadsafds/ssd",ps.getTargetPath(resource, request, response, "/test", "/sadsafds/ssd"));
    Assert.assertEquals("/0b/1f/ec/29/sadsafds/ssd",ps.getTargetPath(resource, request, response, "/", "/sadsafds/ssd"));
    
    verify(request,resource);
  }
  

}
