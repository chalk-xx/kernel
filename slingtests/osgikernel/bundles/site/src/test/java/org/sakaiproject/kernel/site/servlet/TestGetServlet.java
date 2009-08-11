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
package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.site.SiteServiceImpl;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class TestGetServlet extends AbstractSiteNodeTest {

  @Test
  public void testRenderSite() throws RepositoryException, IOException, ServletException
  {
    goodSiteNodeSetup();
    expect(node.hasProperty(eq(SiteService.SAKAI_SKIN))).andReturn(false);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    Resource resource = createMock(Resource.class);
    expect(resourceResolver.getResource(eq(SiteServiceImpl.DEFAULT_SITE))).andReturn(resource);
    response.setContentType(eq("text/html"));
    expect(response.getOutputStream()).andReturn(null);
    expect(resource.adaptTo(eq(InputStream.class))).andReturn(new InputStream() {
      @Override
      public int read() throws IOException {
        return -1;
      }
    });
    response.setStatus(eq(HttpServletResponse.SC_OK));
    makeRequest();
  }
  
  private void setupEmptyExtension() {
    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(requestPathInfo).anyTimes();
    expect(requestPathInfo.getExtension()).andReturn(null).anyTimes();
  }

  @Test
  public void testSiteException() throws RepositoryException, IOException, ServletException, SiteException
  {
    goodSiteNodeSetup();
    SiteService siteService = createMock(SiteService.class);
    expect(siteService.isSite(isA(Item.class))).andReturn(true);
    expect(siteService.getSiteSkin(isA(Node.class))).andThrow(new SiteException(1, "Doom"));
    response.sendError(eq(1), isA(String.class));

    setupEmptyExtension();
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

  
  protected void makeRequest() throws ServletException, IOException {
    setupEmptyExtension();
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
