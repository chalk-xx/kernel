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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private RequestParameter requestParameter;
  @Mock
  private ResourceResolver resolver;
  @Mock
  private Session session;

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
    when(response.isCommitted()).thenReturn(false);
    when(request.getRequestParameter("site")).thenReturn(null);
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
    when(requestParameter.getString()).thenReturn("site/foo");
    when(request.getRequestParameter("site")).thenReturn(requestParameter);
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
    when(requestParameter.getString()).thenReturn("/site/foo");
    when(request.getRequestParameter("site")).thenReturn(requestParameter);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }


}
