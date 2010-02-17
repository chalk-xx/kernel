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

import static org.junit.Assert.assertNull;

import static org.junit.Assert.assertNotNull;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
/**
 *
 */
public class VirtualResourceProviderImplTest extends AbstractEasyMockTest {

  private List<Object> all = new ArrayList<Object>();
  private VirtualResourceProviderImpl v;
  private Session session;

  @Before
  public void before() {
     v = new VirtualResourceProviderImpl();
     session = createMock(Session.class);
  }
 
  @After
  public void after() {
    verify();

  }


  public boolean checkIgnore(String path, boolean check, boolean exists) throws RepositoryException {
    if ( check ) {
      expect(session.itemExists(path)).andReturn(exists).atLeastOnce();
    }
    replay();
    return v.ignoreThisPath(session,path);
  }
  
  @Test 
  public void testLastPath() {
    replay();
    v.pushLastPath("PathA");
    v.pushLastPath("PathB");
    assertEquals("PathB", v.getLastPath());
    assertEquals("PathB",v.popLastPath());
    assertEquals("PathA", v.getLastPath());
    assertEquals("PathA",v.popLastPath());
    assertEquals(null,v.popLastPath());
  }

  @Test
  public void testGetParentReference() {
    replay();
    assertEquals("/testing/one", v.getParentReference("/testing/one/two"));
  }
  
  
  @Test 
  public void testGetResource() throws PathNotFoundException, RepositoryException {
    Node node = createMock(Node.class);
    Property property = createNiceMock(Property.class);
    Session session = createNiceMock(Session.class);
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    VirtualResourceType virtualResourceType = createMock(VirtualResourceType.class);
    Resource resource = createNiceMock(Resource.class);
    expect(virtualResourceType.getResourceType()).andReturn("sakai/virtualtype").anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(session.getItem("/apath/bla/bla")).andThrow(new PathNotFoundException());
    expect(session.getItem("/apath/bla")).andThrow(new PathNotFoundException());
    expect(session.getItem("/apath")).andReturn(node);
    expect(node.isNode()).andReturn(true).anyTimes();
    expect(node.getPath()).andReturn("/apath").anyTimes();
    expect(node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true).anyTimes();
    expect(node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)).andReturn(property).anyTimes();
    expect(property.getString()).andReturn("sakai/virtualtype").anyTimes();
    expect(virtualResourceType.getResource(resourceResolver, null, node, node, "/apath/bla/bla")).andReturn(resource);
    
    
    replay();
    v.bindVirtualResourceType(virtualResourceType);
    assertNotNull(v.getResource(resourceResolver, "/apath/bla/bla"));
    v.unbindVirtualResourceType(virtualResourceType);
    assertNull(v.getResource(resourceResolver, "/apath/bla/bla"));
  }
  
  @Test
  public void testIgnoreThisPath() throws RepositoryException {
    assertTrue(checkIgnore("/trertre/erter.sdf", false, false));
  }

  @Test
  public void testIgnoreThisPath1() throws RepositoryException {
    assertTrue(checkIgnore("/trertre/erter", true, true));
  }
 
  @Test
  public void testIgnoreThisPath2() throws RepositoryException {
    assertFalse(checkIgnore("/trertre/erter", true, false));
  }
 
  @Test
  public void testIgnoreThisPath3() throws RepositoryException {
    assertFalse(checkIgnore("/tre.rtre/erter", true, false));
  }

  @Test
  public void testIgnoreThisPath5() throws RepositoryException {
    assertTrue(checkIgnore("/tre.rtre", false, true));
  }

}
