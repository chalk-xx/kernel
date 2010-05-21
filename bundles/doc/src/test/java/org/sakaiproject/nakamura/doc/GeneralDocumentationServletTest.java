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
package org.sakaiproject.nakamura.doc;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.doc.servlet.ServletDocumentationTracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 *
 */
public class GeneralDocumentationServletTest {

  private GeneralDocumentationServlet servlet = null;
  private BundleContext bundleContext;
  private ComponentContext componentContext;

  @Before
  public void setUp() throws InvalidSyntaxException {
    servlet = new GeneralDocumentationServlet();

    bundleContext = mock(BundleContext.class);
    expectServiceTrackerCalls(bundleContext, Servlet.class.getName());

    componentContext = mock(ComponentContext.class);
    when(componentContext.getBundleContext()).thenReturn(bundleContext);
    servlet.activate(componentContext);
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testGet() throws IOException, ServletException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(writer);

    when(request.getRequestParameter("p")).thenReturn(null);

    Servlet documentedServlet = new DocumentedService();
    ServletDocumentationTracker tracker = new ServletDocumentationTracker(bundleContext);
    ServiceReference reference = createServiceReference();
    tracker.addServlet(reference, documentedServlet);
    servlet.servletTracker = tracker;

    servlet.doGet(request, response);
    writer.flush();
    String s = baos.toString("UTF-8");
    assertEquals(true, s.contains(DocumentedService.class.getAnnotation(
        ServiceDocumentation.class).url()));
  }

  private ServiceReference createServiceReference() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> dict = new Hashtable<String, String>();
    dict.put(Constants.BUNDLE_NAME, "doc");
    when(bundle.getHeaders()).thenReturn(dict);
    when(bundle.getRegisteredServices()).thenReturn(new ServiceReference[] {});

    ServiceReference reference = mock(ServiceReference.class);
    when(reference.getBundle()).thenReturn(bundle);
    when(reference.getPropertyKeys()).thenReturn(new String[] { "foo" });
    when(reference.getProperty("foo")).thenReturn("bar");

    return reference;
  }

  private void expectServiceTrackerCalls(BundleContext bundleContext, String className)
      throws InvalidSyntaxException {
    Filter filter = mock(Filter.class);

    when(bundleContext.createFilter("(objectClass=" + className + ")"))
        .thenReturn(filter);
    when(bundleContext.getServiceReferences(className, null)).thenReturn(
        new ServiceReference[0]);
  }
}
