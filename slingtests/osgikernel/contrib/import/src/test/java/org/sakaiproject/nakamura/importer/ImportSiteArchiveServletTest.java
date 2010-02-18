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
package org.sakaiproject.nakamura.importer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.files.FilesConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportSiteArchiveServletTest {
  private ImportSiteArchiveServlet importSiteArchiveServlet;
  @Mock
  ServletConfig servletConfig;

  @Before
  public void setUp() throws Exception {
    importSiteArchiveServlet = new ImportSiteArchiveServlet();
    try {
      importSiteArchiveServlet.init(servletConfig);
    } catch (Exception e) {
      assertNull("init method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostNoSiteParam() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRequestParameter("site")).thenReturn(null);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostBadSiteParam() {
    RequestParameter siteParam = mock(RequestParameter.class);
    when(siteParam.getString()).thenReturn("site/foo");
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRequestParameter("site")).thenReturn(siteParam);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostNoFileData() {
    RequestParameter siteParam = mock(RequestParameter.class);
    when(siteParam.getString()).thenReturn("/site/foo");
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRequestParameter("site")).thenReturn(siteParam);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPost() throws Exception {
    // mock RequestParameter which returns a valid siteParam
    RequestParameter siteParam = mock(RequestParameter.class);
    when(siteParam.getString()).thenReturn("/site/foo");
    // mock Request that returns valid siteParam
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRequestParameter("site")).thenReturn(siteParam);
    // mock param which stubs a real InputStream from archive.zip test file
    RequestParameter fileParam = mock(RequestParameter.class);
    when(fileParam.getInputStream()).thenAnswer(new Answer<InputStream>() {
      public InputStream answer(InvocationOnMock invocation) {
        FileInputStream fis = null;
        try {
          File file = new File("archive.zip");
          System.out.println("\nPATH:" + file.getAbsolutePath());
          fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
          throw new Error(e);
        }
        return fis;
      }
    });
    // Request returns a valid FileData RequestParameter[]
    RequestParameter[] files = { fileParam };
    when(request.getRequestParameters("Filedata")).thenReturn(files);
    // mock Session that always claims itemExists == true
    Session userSession = mock(Session.class, "userSession");
    when(userSession.itemExists(anyString())).thenReturn(true);
    // mock Property that returns a valid SAKAI_FILENAME
    Property property = mock(Property.class, "fileNodeProperty");
    when(property.getString()).thenReturn("foo");
    // mock child Node to parent fileNode
    Node content = mock(Node.class, "contentNode");
    // mock fileNode that returns the mock Property, Session, and getPath
    Node fileNode = mock(Node.class, "fileNode");
    when(fileNode.getProperty(FilesConstants.SAKAI_FILENAME)).thenReturn(
        property);
    when(fileNode.getSession()).thenReturn(userSession);
    when(fileNode.getPath()).thenReturn("/foo");
    when(fileNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(content);
    when(userSession.getItem(anyString())).thenReturn(fileNode);
    // mock ResourceResolver which Request will return
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(userSession);
    when(request.getResourceResolver()).thenReturn(resolver);
    // inject ClusterTrackingService
    ClusterTrackingService clusterTrackingService = mock(ClusterTrackingService.class);
    when(clusterTrackingService.getClusterUniqueId()).thenReturn(
        UUID.randomUUID().toString());
    importSiteArchiveServlet.clusterTrackingService = clusterTrackingService;
    // inject SlingRepository
    SlingRepository slingRepository = mock(SlingRepository.class);
    Session adminSession = mock(Session.class, "adminSession");
    when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
    importSiteArchiveServlet.slingRepository = slingRepository;
    // mock Response mainly for verify
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_OK), anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }
}
