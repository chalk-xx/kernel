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
package org.sakaiproject.nakamura.user.lite.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class LiteAbstractSakaiGroupPostServletTest {

  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;
  
  private LiteAbstractSakaiGroupPostServlet servlet;

  private Repository repository;
  
  private Session session;


  public LiteAbstractSakaiGroupPostServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
    repository = RepositoryHelper.getRepository(new String[]{ "ieb","jeff","joe"}, new String[]{"g-course101"} );
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException {
    
    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    session = repository.loginAdministrative("ieb");
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(session);
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);
    when(request.getRemoteUser()).thenReturn("ieb");
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    servlet = new LiteAbstractSakaiGroupPostServlet() {
      
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      protected void handleOperation(SlingHttpServletRequest request,
          HtmlResponse htmlResponse, List<Modification> changes)
          throws StorageClientException, AccessDeniedException, AuthorizableExistsException {
        
      }
    };

  }


  @Test
  public void testAddManager() throws Exception {
    when(request.getParameterValues(":manager")).thenReturn(
        new String[] { "jack", "john", "jeff" });
    when(request.getParameterValues(":manager@Delete")).thenReturn(null);
    
    Map<String, Object> props = Maps.newHashMap();
    props.put(Group.ID_FIELD,"g-foo");
    Group group = new Group(props);

    Map<String, Object> toSave = Maps.newLinkedHashMap();

    servlet.updateOwnership(request, group, new String[] { "joe" }, null, toSave);

    Set<String> values = ImmutableSet.of((String[])group.getProperty(UserConstants.PROP_GROUP_MANAGERS));
    assertTrue(values.contains("jeff"));
    assertTrue(values.contains("jack"));
    assertTrue(values.contains("john"));
    assertTrue(values.contains("joe"));
    assertEquals(4, values.size());
  }

  @Test
  public void testDeleteManager() throws Exception {
    // Remove jeff, add jack
    when(request.getParameterValues(":manager")).thenReturn(new String[] { "jack" });
    when(request.getParameterValues(":manager@Delete")).thenReturn(
        new String[] { "jeff" });

    Map<String, Object> props = Maps.newHashMap();
    props.put(Group.ID_FIELD,"g-foo");
    Group group = new Group(props);

    Map<String, Object> toSave = Maps.newLinkedHashMap();
    servlet.updateOwnership(request, group, new String[0], null,toSave);

    Set<String> values = ImmutableSet.of((String[])group.getProperty(UserConstants.PROP_GROUP_MANAGERS));
    assertTrue(values.contains("jack"));
    assertEquals(1, values.size());
  }
  
  @Test
  public void testNonJoinableGroup() throws Exception {
    Session adminSession = repository.loginAdministrative();
    adminSession.getAuthorizableManager().createGroup("g-fooNoJoin", "FooNoJoin", ImmutableMap.of(UserConstants.PROP_JOINABLE_GROUP,(Object)"no"));
    adminSession.logout();
    
    Group group = (Group) session.getAuthorizableManager().findAuthorizable("g-fooNoJoin");
    
    
    
    when(request.getParameterValues(":member")).thenReturn(
        new String[] { "ieb" });
    
    
    Map<String, Object> toSave = Maps.newLinkedHashMap();
    ArrayList<Modification> changes = new ArrayList<Modification>();
    try {
       servlet.updateGroupMembership(request, session, group, changes, toSave);
    
       servlet.saveAll(session, toSave);
       Assert.fail("Should have thrown an exception");
    } catch ( AccessDeniedException e) {
      
    }
  }
  
  @Test
  public void testJoinableGroup() throws Exception {
    Map<String, Object> props = Maps.newHashMap();
    props.put(UserConstants.PROP_JOINABLE_GROUP,"yes");
    Session adminSession = repository.loginAdministrative();
    adminSession.getAuthorizableManager().createGroup("g-foo2", "g-foo2", props);
    adminSession.logout();
    
    Group group = (Group) session.getAuthorizableManager().findAuthorizable("g-foo2");
    
    when(request.getRemoteUser()).thenReturn("ieb");
    when(request.getParameterValues(":member")).thenReturn(
        new String[] { "ieb" });
    
    servlet.repository = repository;  
    
    
    ArrayList<Modification> changes = new ArrayList<Modification>();
    
    Map<String, Object> toSave = Maps.newLinkedHashMap();
    servlet.updateGroupMembership(request, session, group, changes, toSave);
    
    assertTrue(changes.size() > 0);
  }

}
