/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.shindig;

import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.JsServlet;
import org.apache.shindig.gadgets.servlet.MakeRequestServlet;
import org.apache.shindig.gadgets.servlet.OAuthCallbackServlet;
import org.apache.shindig.gadgets.servlet.ProxyServlet;
import org.apache.shindig.gadgets.servlet.RpcServlet;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.opensocial.service.JsonRpcServlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Activator for making Apache Shindig into an OSGi bundle.
 */
public class Activator implements BundleActivator {
  private static final Class<? extends HttpServlet>[] servletsToRegister = new Class[] {
      DataServiceServlet.class, JsonRpcServlet.class, ConcatProxyServlet.class,
      GadgetRenderingServlet.class, JsServlet.class, MakeRequestServlet.class, ProxyServlet.class,
      RpcServlet.class, OAuthCallbackServlet.class };

  private ServiceTracker httpServiceTracker;

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    httpServiceTracker = new ServiceTracker(context, HttpService.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService) context.getService(reference);
        for (Class<? extends HttpServlet> servletClass : servletsToRegister) {
          try {
            HttpContext httpContext = httpService.createDefaultHttpContext();
            httpService.registerServlet(servletClass.getName(), servletClass.newInstance(), null,
                httpContext);
          } catch (ServletException e) {
            e.printStackTrace();
          } catch (NamespaceException e) {
            e.printStackTrace();
          } catch (InstantiationException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        HttpService httpService = (HttpService) service;
        for (Class<? extends HttpServlet> servletClass : servletsToRegister) {
          httpService.unregister(servletClass.getName());
        }
        super.removedService(reference, service);
      }
    };
    httpServiceTracker.open();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    httpServiceTracker.close();
  }
}
