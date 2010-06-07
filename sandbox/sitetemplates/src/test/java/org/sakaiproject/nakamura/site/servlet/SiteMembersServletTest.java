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
package org.sakaiproject.nakamura.site.servlet;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.site.ACMEGroupStructure;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
public class SiteMembersServletTest extends AbstractSiteServletTest {

  private SiteMembersServlet servlet;
  private Session adminSession;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    adminSession = loginAdministrative();
  }

  public void testGetMembers() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);
    
    // The managers will be maintainers of the site node.
    // The others will be viewers.
    List<Authorizable> siteManagers = new ArrayList<Authorizable>();
    siteManagers.add(acme.acmeManagers);
    setManagers(siteNode, siteManagers);
    addAuthorizable(siteNode, acme.acmeLabs, false);

    // Retrieve the site node trough a Developer.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());
    
    Resource siteResource = mock(Resource.class);
    when(siteResource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(siteResource);

    // Mock the selectors
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getSelectors()).thenReturn(new String[] { "members", "-1" });
    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    JSONArray arr = makeGetRequestReturningJSON();
    // Should contain 7 users
    assertEquals(7, arr.length());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.servlet.AbstractSiteServletTest#makeRequest()
   */
  @Override
  public void makeRequest() throws Exception {
    servlet = new SiteMembersServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
  }

}
