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
package org.sakaiproject.kernel.doc;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

/**
 *
 */
public class ServletDocumentationTracker extends ServiceTracker {

  private Map<String, ServletDocumentation> servletDocumentation = new ConcurrentHashMap<String, ServletDocumentation>();

  /**
   * @param context
   * @param customizer
   */
  public ServletDocumentationTracker(BundleContext context) {
    super(context, Servlet.class.getName(), null);
  }

  @Override
  public Object addingService(ServiceReference reference) {
    Object service = super.addingService(reference);
    if (service instanceof Servlet) {
      ServletDocumentation doc = new ServletDocumentation(reference,
          (Servlet) service);
      servletDocumentation.put(doc.getKey(),doc);
    }
    return service;
  }

  @Override
  public void removedService(ServiceReference reference, Object service) {
    if (service instanceof Servlet) {
      ServletDocumentation doc = new ServletDocumentation(reference,
          (Servlet) service);
      servletDocumentation.remove(doc.getKey());
    }
    super.removedService(reference, service);
  }

  public Map<String, ServletDocumentation> getServletDocumentation() {
    return servletDocumentation;
  }

}
