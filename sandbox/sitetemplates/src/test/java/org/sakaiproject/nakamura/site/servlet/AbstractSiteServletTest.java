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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.site.AbstractSiteTest;
import org.sakaiproject.nakamura.site.SiteServiceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public abstract class AbstractSiteServletTest extends AbstractSiteTest {

  protected SlingHttpServletRequest request;
  protected SlingHttpServletResponse response;
  protected SiteService siteService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    siteService = new SiteServiceImpl();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);
  }

  public abstract void makeRequest() throws Exception;

  /**
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  public Node createGoodSite(Session adminSession) throws Exception {
    long time = System.currentTimeMillis();
    String sitePath = "/" + time + "/sites/goodsite";
    Node siteNode = createSite(adminSession, sitePath);

    if (adminSession.hasPendingChanges()) {
      adminSession.save();
    }
    return siteNode;
  }

  public byte[] makeGetRequestReturningBytes() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(writer);
    makeRequest();
    writer.flush();
    return baos.toByteArray();
  }

  public String makeGetRequestReturningString() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(writer);
    makeRequest();
    writer.flush();
    return baos.toString("utf-8");
  }

  public JSONArray makeGetRequestReturningJSON() throws Exception, JSONException {
    String jsonString = new String(makeGetRequestReturningBytes());
    System.err.println(jsonString);
    return new JSONArray(jsonString);
  }
}
