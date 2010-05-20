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
package org.sakaiproject.nakamura.doc.servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

/**
 * Track servlets and record them in a map, keyed by class name, its beleived that there
 * will in general be only one servlet per service as we tend to generated the
 * serviceComponents.xml for OSGi from the class and not manually add services to that xml
 * with the same classname.
 */
public class ServletDocumentationTracker extends ServiceTracker {

  /**
   * A map of servlet Documetnation objects.
   */
  private Map<String, ServletDocumentation> servletDocumentation = new ConcurrentHashMap<String, ServletDocumentation>();

  /**
   * @param context
   * @param customizer
   */
  public ServletDocumentationTracker(BundleContext context) {
    super(context, Servlet.class.getName(), null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    Object service = super.addingService(reference);
    if (service instanceof Servlet) {
      addServlet(reference, (Servlet) service);
    }
    return service;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
   *      java.lang.Object)
   */
  @Override
  public void removedService(ServiceReference reference, Object service) {
    if (service instanceof Servlet) {
      removeServlet(reference, (Servlet) service);
    }
    super.removedService(reference, service);
  }

  /**
   * @param reference
   * @param service
   */
  public void removeServlet(ServiceReference reference, Servlet servlet) {
    ServletDocumentation doc = new ServletDocumentation(reference, servlet);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.remove(key);
    }
  }

  /**
   * @param reference
   * @param service
   */
  public void addServlet(ServiceReference reference, Servlet servlet) {
    ServletDocumentation doc = new ServletDocumentation(reference, servlet);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.put(key, doc);
    }
  }

  /**
   * @return the map of servlet documents, this map is the internal map and should not be
   *         modified.
   */
  public Map<String, ServletDocumentation> getServletDocumentation() {
    return servletDocumentation;
  }

}
