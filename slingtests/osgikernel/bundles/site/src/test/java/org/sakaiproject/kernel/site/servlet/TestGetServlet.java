package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

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
    expect(node.hasProperty(eq(SiteService.SAKAI_SITE_TEMPLATE))).andReturn(false);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    Resource resource = createMock(Resource.class);
    expect(resourceResolver.getResource(eq(SiteServiceImpl.DEFAULT_SITE))).andReturn(resource);
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
  
  @Test
  public void testSiteException() throws RepositoryException, IOException, ServletException, SiteException
  {
    goodSiteNodeSetup();
    SiteService siteService = createMock(SiteService.class);
    expect(siteService.isSite(isA(Item.class))).andReturn(true);
    expect(siteService.getSiteTemplate(isA(Node.class))).andThrow(new SiteException(1, "Doom"));
    response.sendError(eq(1), isA(String.class));

    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

  
  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
