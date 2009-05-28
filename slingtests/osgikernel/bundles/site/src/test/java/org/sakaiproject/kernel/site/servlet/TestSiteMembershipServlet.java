package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;

public class TestSiteMembershipServlet extends AbstractSiteServiceServletTest {

  private final String TEST_USER = "testuser";
  private final String TEST_SITE_GROUP = "some_site_group";
  private final String TEST_SITE_PATH = "/some/test/site";
  
  private SlingHttpServletRequest request;
  
  @Test
  public void testUserNoMemberships() throws ServletException, IOException, RepositoryException, JSONException
  {
    request = createMock(SlingHttpServletRequest.class);
    createDummyUser(TEST_USER);
    expect(request.getRemoteUser()).andReturn(TEST_USER);
    JSONArray sites = makeGetRequestReturningJSON();
    assertEquals("Expected no sites back", 0, sites.length());
  }
  
  @Test
  public void testUserWithMemberships() throws ServletException, IOException, RepositoryException, JSONException
  {
    request = createMock(SlingHttpServletRequest.class);
    Group siteGroup = createDummySiteGroup(TEST_SITE_GROUP, TEST_SITE_PATH);
    List<Group> siteGroups = new ArrayList<Group>();
    siteGroups.add(siteGroup);
    createDummyUserWithGroups(TEST_USER, siteGroups);
    expect(request.getRemoteUser()).andReturn(TEST_USER);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resolver);
    expect(resolver.resolve(eq(TEST_SITE_PATH))).andReturn(dummySiteResource(TEST_SITE_PATH));
    JSONArray sites = makeGetRequestReturningJSON();
    assertEquals("Expected 1 site back", 1, sites.length());
    JSONObject site = (JSONObject) sites.get(0);
    assertEquals("Expected siteref to match path", TEST_SITE_PATH, site.get("siteref"));
  }

  private Resource dummySiteResource(String path) {
    Resource result = createMock(Resource.class);
    Map<String,Object> userValues = new HashMap<String,Object>();
    userValues.put("path", path);
    expect(result.adaptTo(ValueMap.class)).andReturn(new ValueMapDecorator(userValues)).anyTimes();
    return result;
  }

  private Group createDummySiteGroup(String groupName, String sitePath) throws RepositoryException {
    Group result = createDummyGroup(groupName);
    expect(result.hasProperty(SiteService.SITES)).andReturn(true);
    expect(result.getProperty(SiteService.SITES)).andReturn(new Value[] { new MockValue(sitePath) });
    return result;
  }

  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteMembershipServlet servlet = new SiteMembershipServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
