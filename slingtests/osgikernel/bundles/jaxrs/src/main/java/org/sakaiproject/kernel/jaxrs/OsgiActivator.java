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
package org.sakaiproject.kernel.jaxrs;

import org.jboss.resteasy.spi.Registry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.sakaiproject.kernel.api.jaxrs.JaxRestService;

import javax.servlet.ServletException;

/**
 * 
 */
public class OsgiActivator implements BundleActivator {
  /**
   *
   */
  protected Registry registry;

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    // Register the rest servlet when the HTTP service becomes available
    ServiceTracker httpTracker = new ServiceTracker(context,
        HttpService.class.getName(), null) {
      ResteasyServlet restServlet = new ResteasyServlet();

      /**
       * {@inheritDoc}
       * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
       */
      @Override
      public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService)context.getService(reference);
        try {
          httpService.registerServlet(ResteasyServlet.SERVLET_URL_MAPPING, restServlet, null, null);
        } catch (ServletException e) {
          e.printStackTrace();
        } catch (NamespaceException e) {
          e.printStackTrace();
        }
        // The registry is available only after servlet registration
        registry = restServlet.getRegistry();
        // Add the existing JAX-RS resources
        ServiceReference[] jaxRsRefs = null;
        try {
          jaxRsRefs = context.getAllServiceReferences(JaxRestService.class.getName(), null);
        } catch (InvalidSyntaxException e) {
          e.printStackTrace();
        }
        if (jaxRsRefs != null) {
          for (ServiceReference jaxRsRef : jaxRsRefs) {
            registry.addSingletonResource(context.getService(jaxRsRef));
          }
        }
        return super.addingService(reference);
      }

      /**
       * {@inheritDoc}
       * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
       */
      @Override
      public void removedService(ServiceReference reference, Object service) {
        HttpService httpService = (HttpService)service;
        httpService.unregister(ResteasyServlet.SERVLET_URL_MAPPING);
        super.removedService(reference, service);
      }
    };
    httpTracker.open();

    // Track JAX-RS Resources that are added and removed
    ServiceTracker jaxRsResourceTracker = new ServiceTracker(context,
        JaxRestService.class.getName(), null) {
      /**
       * {@inheritDoc}
       * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
       */
      @Override
      public Object addingService(ServiceReference reference) {
        JaxRestService jaxRsResource = (JaxRestService)context.getService(reference);
        registry.addSingletonResource(jaxRsResource);
        return super.addingService(reference);
      }

      /**
       * {@inheritDoc}
       * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
       */
      @Override
      public void removedService(ServiceReference reference, Object service) {
        registry.removeRegistrations(context.getService(reference).getClass());
        super.removedService(reference, service);
      }
    };
    jaxRsResourceTracker.open();
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext arg0) throws Exception {
  }

}
