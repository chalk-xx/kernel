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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import org.sakaiproject.nakamura.api.resource.lite.SparsePostOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

/**
 * track servlets with service documentation in a single place.
 */
@Component(immediate=true)
@Service(value=ServletDocumentationRegistry.class)
@References(
    value = { 
        @Reference(name = "servlet", referenceInterface=Servlet.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "operation", referenceInterface=SlingPostOperation.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "sparseoperation", referenceInterface=SparsePostOperation.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    }
  )
public class ServletDocumentationRegistryImpl implements ServletDocumentationRegistry {

  private ComponentContext context;
  private List<ServiceReference> pendingReferences = new ArrayList<ServiceReference>();
  /**
   * A map of servlet Documentation objects.
   */
  private Map<String, ServletDocumentation> servletDocumentation = new ConcurrentHashMap<String, ServletDocumentation>();

  protected void activate(ComponentContext context) { 
    synchronized (pendingReferences) {
      this.context = context;
      for (ServiceReference ref : pendingReferences) {
        addDocumentation(ref);
      }
    }
  }

  protected void deactivate(ComponentContext context) {
    synchronized (pendingReferences) {
      pendingReferences.clear();
      servletDocumentation.clear();
      this.context = null;
    }
  }

  protected void bindServlet(ServiceReference reference) {
    synchronized (pendingReferences) {
      if (context == null) {
        pendingReferences.add(reference);
      } else {
        addDocumentation(reference);
      }
    }
  }

  protected void unbindServlet(ServiceReference reference) {
    synchronized (pendingReferences) {
      pendingReferences.remove(reference);
      if (context != null) {
        removeDocumentation(reference);
      }
    }
  }

  /**
   * @param reference the ServiceReference whose documentation will be removed
   */
  public void removeDocumentation(ServiceReference reference) {
    Servlet servlet = (Servlet) context.getBundleContext().getService(reference);
    ServletDocumentation doc = new ServletDocumentation(reference, servlet);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.remove(key);
    }
  }

  /**
   * @param reference the service reference representing the service object to add to the documentation
   */
  public void addDocumentation(ServiceReference reference) {
    Object service = context.getBundleContext().getService(reference);
    if (notDeprecated(service) && service.getClass().getCanonicalName().startsWith("org.sakaiproject")) {
      ServletDocumentation doc = new ServletDocumentation(reference, service);
      String key = doc.getKey();
      if (key != null) {
        servletDocumentation.put(key, doc);
      }
    }
  }

  private boolean notDeprecated(Object service) {
    return !service.getClass().isAnnotationPresent(Deprecated.class);
  }

  /*
  * Operations
  */
  
  protected void bindOperation(ServiceReference reference) {
    synchronized (pendingReferences) {
      if (context == null) {
        pendingReferences.add(reference);
      } else {
        addDocumentation(reference);
      }
    }
  }

  protected void unbindOperation(ServiceReference reference) {
    synchronized (pendingReferences) {
      pendingReferences.remove(reference);
      if (context != null) {
        removeDocumentation(reference);
      }
    }
  }


    /*
   *  Sparse Operations
   */

  protected void bindSparseoperation(ServiceReference reference) {
    synchronized (pendingReferences) {
      if (context == null) {
        pendingReferences.add(reference);
      } else {
        addDocumentation(reference);
      }
    }
  }

  protected void unbindSparseoperation(ServiceReference reference) {
    synchronized (pendingReferences) {
      pendingReferences.remove(reference);
      if (context != null) {
        removeDocumentation(reference);
      }
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
