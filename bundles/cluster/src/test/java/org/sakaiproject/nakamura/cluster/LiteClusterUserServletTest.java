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
package org.sakaiproject.nakamura.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

@RunWith(MockitoJUnitRunner.class)
public class LiteClusterUserServletTest {
  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;
  @Mock
  private CacheManagerService cacheManagerService;
  @Mock
  private Cache<Object> userTrackingCache;
  @Mock
  private Cache<Object> serverTrackingCache;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  SlingHttpServletResponse response;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  Resource resource;
  private Repository repository;
  private Session session;
  private String serverId;
  private LiteClusterUserServlet clusterUserServlet;
  @Mock
  private ComponentContext componentContext;

  @Before
  public void before() throws StorageClientException, AccessDeniedException,
      ClassNotFoundException, IOException {
    when(serverTrackingCache.list()).thenReturn(new ArrayList<Object>());
    when(serverTrackingCache.put(anyString(), any())).thenReturn(new Object());
    when(cacheManagerService.getCache("user-tracking-cache", CacheScope.INSTANCE))
        .thenReturn(userTrackingCache);
    when(
        cacheManagerService.getCache("server-tracking-cache",
            CacheScope.CLUSTERREPLICATED)).thenReturn(serverTrackingCache);
    clusterTrackingServiceImpl = new ClusterTrackingServiceImpl(cacheManagerService);

    repository = (Repository) new BaseMemoryRepository().getRepository();
    final Session adminSession = repository.loginAdministrative();
    final AuthorizableManager aam = adminSession.getAuthorizableManager();
    Map<String, Object> userProps = new HashMap<String, Object>();
    userProps.put("prop1", new String[] { "tokenA", "tokenB", "tokenC" });
    userProps.put("prop2", "tokenA");
    userProps.put("prop3", new Object[] {});
    assertTrue(aam.createUser("ieb", "ieb", "password", userProps));
    assertTrue(aam.createGroup("group:A", "Group A", null));
    final Group groupA = (Group) aam.findAuthorizable("group:A");
    assertNotNull(groupA);
    groupA.addMember("ieb");
    aam.updateAuthorizable(groupA);
    assertTrue(aam.createGroup("group:B", "Group B", null));
    final Group groupB = (Group) aam.findAuthorizable("group:B");
    assertNotNull(groupB);
    groupB.addMember("ieb");
    aam.updateAuthorizable(groupB);
    adminSession.logout();

    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito
        .withSettings().extraInterfaces(SessionAdaptable.class));
    session = repository.loginAdministrative("ieb");
    Mockito.when(((SessionAdaptable) jcrSession).getSession()).thenReturn(session);
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class))
        .thenReturn(jcrSession);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(request.getRemoteUser()).thenReturn(session.getUserId());
    when(resource.getPath()).thenReturn("/var/cluster/user");
    when(request.getResource()).thenReturn(resource);

    clusterUserServlet = new LiteClusterUserServlet(clusterTrackingServiceImpl,
        session.getAuthorizableManager());

    Hashtable<String, Object> dict = new Hashtable<String, Object>();
    dict.put(ClusterTrackingServiceImpl.PROP_SECURE_HOST_URL, "http://localhost:8081");
    when(componentContext.getProperties()).thenReturn(dict);
  }

  @After
  public void after() {
  }

  @Test
  public void testActivateDeactivate() throws Exception {
    activate();

    deactivate();

    clusterTrackingServiceImpl.activate(componentContext);

    clusterTrackingServiceImpl.deactivate(componentContext);

    checkActivation();
  }

  @Test
  public void testNormalGet() throws Exception {
    activate();

    ClusterUserImpl clusterUser = new ClusterUserImpl("ieb", "otherServerId");

    // request validated, processing request.
    when(request.getParameter("c")).thenReturn("some-tracking-cookie");
    when(userTrackingCache.get("some-tracking-cookie")).thenReturn(clusterUser);

    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));

    deactivate();

    clusterTrackingServiceImpl.activate(componentContext);

    clusterUserServlet.doGet(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);

    JSONObject jsonObject = new JSONObject(writer.toString());
    assertEquals(serverId, jsonObject.get("server"));
    JSONObject userObject = jsonObject.getJSONObject("user");
    assertEquals("otherServerId", userObject.get("homeServer"));
    assertEquals("ieb", userObject.get("id"));
    JSONObject properties = userObject.getJSONObject("properties");
    assertEquals(3, properties.getJSONArray("prop1").length());
    assertEquals("tokenA", properties.get("prop2"));
    assertEquals(0, properties.getJSONArray("prop3").length());

    JSONArray declaredMembership = userObject.getJSONArray("declaredMembership");
    assertEquals(2, declaredMembership.length());
    assertEquals("group:A", declaredMembership.get(0));
    assertEquals("group:B", declaredMembership.get(1));

    // TODO BL120 indirect group membership not currently supported in sparse
    // JSONArray membership = userObject.getJSONArray("membership");
    // assertEquals(2, membership.length());
    // assertEquals("indirectgroup:A", membership.get(0));
    // assertEquals("indirectgroup:B", membership.get(1));

    checkActivation();
  }

  @Test
  public void testNotFoundGet() throws Exception {
    activate();
    Resource resource = mock(Resource.class);

    when(request.getResource()).thenReturn(resource);

    // request validated, processing request.
    when(request.getParameter("c")).thenReturn(serverId + "-sometrackingcookie");
    when(userTrackingCache.get(serverId + "-sometrackingcookie")).thenReturn(null);

    clusterUserServlet.testing = true;

    deactivate();

    clusterTrackingServiceImpl.activate(componentContext);

    clusterUserServlet.doGet(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);

    checkActivation();

    verify(response).sendError(eq(404), anyString());
  }

  /**
   *
   */
  private void checkActivation() {
    ArgumentCaptor<ClusterServerImpl> csiCapture = ArgumentCaptor
        .forClass(ClusterServerImpl.class);
    verify(serverTrackingCache).put(eq(serverId), csiCapture.capture());
    ClusterServerImpl clusterServerImpl = csiCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
  }

  /**
   * @throws ReflectionException
   * @throws MBeanException
   * @throws NullPointerException
   * @throws InstanceNotFoundException
   * @throws AttributeNotFoundException
   * @throws MalformedObjectNameException
   * 
   */
  private void activate() throws Exception {
    serverId = getServerId();
  }

  /**
   *
   */
  private void deactivate() {
    serverTrackingCache.remove(serverId);
  }

  /**
   * @return
   * @throws NullPointerException
   * @throws MalformedObjectNameException
   * @throws ReflectionException
   * @throws MBeanException
   * @throws InstanceNotFoundException
   * @throws AttributeNotFoundException
   */
  private String getServerId() throws Exception {
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("java.lang:type=Runtime");
    return ((String) mbeanServer.getAttribute(name, "Name")).replace('@', '-');
  }

}
