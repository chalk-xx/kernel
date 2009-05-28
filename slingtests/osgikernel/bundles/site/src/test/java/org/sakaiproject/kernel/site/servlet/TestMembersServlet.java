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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

public class TestMembersServlet extends AbstractSiteNodeTest {

  private final String TEST_GROUP = "TestGroup";
  private final String TEST_USER = "TestUser";
  
  @Test
  public void testRenderSiteWithNoMembers() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(false).anyTimes();

    JSONArray result = makeGetRequestReturningJSON();
    assertEquals("Expected nothing back", 0, result.length());
  }

  @Test
  public void testRenderSiteWithBadParameters() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(dummyRequestParameter("fish"));
    expect(request.getRequestParameter(eq("items"))).andReturn(dummyRequestParameter("cat"));
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(false).anyTimes();

    JSONArray result = makeGetRequestReturningJSON();
    assertEquals("Expected nothing back", 0, result.length());
  }

  @Test
  public void testRenderSiteWithSingleMember() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    
    setSiteGroups(new String[] { TEST_GROUP });
    User testUser = createDummyUser(TEST_USER);
    createDummyGroupWithMember(TEST_GROUP, testUser);
    
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    expect(resourceResolver.resolve("/system/userManager/user/" + TEST_USER)).andReturn(dummyUserResource(TEST_USER));

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 1 member", 1, json.length());
    JSONObject user = (JSONObject) json.get(0);
    String username = user.getString("username");
    assertEquals("Expected username back", TEST_USER, username);
  }

  @Test
  public void testRenderSiteWithManyMembers() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    
    setSiteGroups(new String[] { TEST_GROUP });
    
    createUsersInGroup(50, TEST_GROUP);    

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 25 members", 25, json.length());
    for (int i=0; i<25; i++)
    {
      JSONObject user = (JSONObject) json.get(i);
      String username = user.getString("username");
      assertEquals("Expected username back", TEST_USER + i, username);
    }
  }

  @Test
  public void testRenderSiteWithNestedMembers() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    
    String SUB_GROUP = "sub";
    setSiteGroups(new String[] { TEST_GROUP, SUB_GROUP });
    
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    List<Authorizable> subUsers = createUsers(0, 10, resourceResolver);
    Group subGroup = createDummyGroupWithMembers(SUB_GROUP, subUsers);
    List<Authorizable> superUsers = createUsers(10, 15, resourceResolver);
    superUsers.add(subGroup);
    createDummyGroupWithMembers(TEST_GROUP, superUsers);

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 25 members", 25, json.length());
    Set<String> usernames = new HashSet<String>();
    for (int i=0; i<json.length(); i++)
    {
      JSONObject user = (JSONObject) json.get(i);
      String username = user.getString("username");
      usernames.add(username);
    }
    for (int i=0; i<25; i++)
    {
      assertTrue("Expected username back", usernames.contains(TEST_USER + i));
    }
  }

  @Test
  public void testRenderSiteWithManyMembersAtOffset() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(dummyRequestParameter("5"));
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    
    setSiteGroups(new String[] { TEST_GROUP });
    
    createUsersInGroup(50, TEST_GROUP);    

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 25 members", 25, json.length());
    for (int i=0; i<25; i++)
    {
      JSONObject user = (JSONObject) json.get(i);
      String username = user.getString("username");
      assertEquals("Expected username back", TEST_USER + (i + 5), username);
    }
  }

  @Test
  public void testRenderSiteWithManyMembersAtOffsetWithLimit() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(dummyRequestParameter("10"));
    expect(request.getRequestParameter(eq("items"))).andReturn(dummyRequestParameter("10"));
    expect(request.getRequestParameters(eq("sort"))).andReturn(null);
    
    setSiteGroups(new String[] { TEST_GROUP });
    
    createUsersInGroup(50, TEST_GROUP);    

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 10 members", 10, json.length());
    for (int i=0; i<10; i++)
    {
      JSONObject user = (JSONObject) json.get(i);
      String username = user.getString("username");
      assertEquals("Expected username back", TEST_USER + (i + 10), username);
    }
  }

  @Test
  public void testRenderSiteWithBadSort() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(new RequestParameter[] { dummyRequestParameter("fish,cat") });
    
    setSiteGroups(new String[] { TEST_GROUP });
    
    createUsersInGroup(50, TEST_GROUP);    

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 25 members", 25, json.length());
    for (int i=0; i<25; i++)
    {
      JSONObject user = (JSONObject) json.get(i);
      String username = user.getString("username");
      assertEquals("Expected username back", TEST_USER + i, username);
    }
  }
  
  @Test
  public void testRenderSiteWithUsernameSort() throws RepositoryException, IOException, ServletException, JSONException
  {
    goodSiteNodeSetup();
    
    expect(request.getRequestParameter(eq("start"))).andReturn(null);
    expect(request.getRequestParameter(eq("items"))).andReturn(null);
    expect(request.getRequestParameters(eq("sort"))).andReturn(new RequestParameter[] { dummyRequestParameter("firstName,desc") });
    
    setSiteGroups(new String[] { TEST_GROUP });
    
    createUsersInGroup(10, TEST_GROUP);    

    JSONArray json = makeGetRequestReturningJSON();
    assertEquals("Expected 10 members", 10, json.length());
    for (int i=0; i<10; i++)
    {
      JSONObject user = (JSONObject) json.get(9 - i);
      String username = user.getString("username");
      assertEquals("Expected username back", TEST_USER + i, username);
    }
  }

  private RequestParameter dummyRequestParameter(String string) {
    RequestParameter result = createMock(RequestParameter.class);
    expect(result.getString()).andReturn(string).anyTimes();
    return result;
  }

  private List<Authorizable> createUsers(int start, int count, ResourceResolver resourceResolver) throws RepositoryException
  {
    List<Authorizable> users = new ArrayList<Authorizable>();
    for (int i=start; i<start + count; i++)
    {
      String testUserName = TEST_USER + i;
      User testUser = createDummyUser(testUserName);
      users.add(testUser);
      expect(resourceResolver.resolve("/system/userManager/user/" + testUserName)).andReturn(dummyUserResource(testUserName)).anyTimes();
    }
    return users;
  }
  
  private void createUsersInGroup(int count, String groupName) throws RepositoryException {
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    List<Authorizable> users = createUsers(0, count, resourceResolver);
    createDummyGroupWithMembers(groupName, users);
  }

  private Resource dummyUserResource(String username) {
    Resource result = createMock(Resource.class);
    Map<String,Object> userValues = new HashMap<String,Object>();
    userValues.put("username", username);
    expect(result.adaptTo(ValueMap.class)).andReturn(new ValueMapDecorator(userValues)).anyTimes();
    return result;
  }

  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteMembersServlet servlet = new SiteMembersServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
