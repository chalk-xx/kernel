package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteService;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractSiteNodeTest extends AbstractSiteServiceServletTest {

  protected String SITE_PATH = "/test/site/path";

  protected Resource resource;
  protected Node node;

  protected void setSiteGroups(String[] groups) throws RepositoryException {
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(true).anyTimes();
    MockProperty authProperty = new MockProperty(SiteService.AUTHORIZABLE);
    authProperty.setValue(groups);
    expect(node.getProperty(eq(SiteService.AUTHORIZABLE))).andReturn(authProperty).anyTimes();
  }

  protected void goodResourceResolverSetup() {
    resource = createMock(Resource.class);
    expect(request.getResource()).andReturn(resource);
  }

  protected void goodSiteNodeSetup() throws RepositoryException {
    goodResourceResolverSetup();
    node = createMock(Node.class);
    expect(resource.adaptTo(eq(Node.class))).andReturn(node);
    expect(node.hasProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(true)
        .anyTimes();
    MockProperty resourceType = new MockProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
    resourceType.setValue(SiteService.SITE_RESOURCE_TYPE);
    expect(node.getProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(
        resourceType).anyTimes();
    expect(node.getPath()).andReturn(SITE_PATH).anyTimes();
  }
  
  @Test
  public void testNullSite() throws IOException, ServletException {
    goodResourceResolverSetup();
    expect(resource.adaptTo(eq(Node.class))).andReturn(null);
    response.sendError(eq(HttpServletResponse.SC_NO_CONTENT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testNotASite() throws IOException, ServletException, RepositoryException {
    goodResourceResolverSetup();
    Node siteNode = createMock(Node.class);
    expect(resource.adaptTo(eq(Node.class))).andReturn(siteNode);
    expect(siteNode.hasProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(
        false);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }


}
