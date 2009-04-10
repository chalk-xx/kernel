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
package org.sakaiproject.kernel.guice;

import com.google.inject.Provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Create a generic Guice provider that binds to a service in OSGi
 */
public class OsgiServiceProvider<T> implements Provider<T>, ServiceListener,
    InvocationHandler {

  private BundleContext bundleContext;
  private T service;
  private String serviceName;
  private T osgiService;
  private ServiceEvent lastEvent;

  /**
   * @param the
   *          interface representing the service
   * @param bundleContext
   *          the bundle context of this bundle
   * @throws InvalidSyntaxException
   */
  @SuppressWarnings("unchecked")
  public OsgiServiceProvider(Class<T> serviceClass, BundleContext bundleContext) {
    this.bundleContext = bundleContext;

    // convert the service class into a reference, since the provision of the service
    // class
    // may change.
    try {
      bundleContext.addServiceListener(this, "((PID = "
          + serviceClass.getName() + ")");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("Listener syntax was wrong " + e.getMessage(), e);
    }
    service = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), serviceClass
        .getInterfaces(), this);
    serviceName = serviceClass.getName();
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.Provider#get()
   */
  public T get() {
    return service;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
   */
  @SuppressWarnings("unchecked")
  public void serviceChanged(ServiceEvent event) {
    lastEvent = event;
    switch (event.getType()) {
    case ServiceEvent.MODIFIED:
    case ServiceEvent.REGISTERED:
      ServiceReference serviceReference = bundleContext.getServiceReference(serviceName);
      osgiService = (T) bundleContext.getService(serviceReference);
      break;
    case ServiceEvent.UNREGISTERING:
      osgiService = null;
      break;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
   *      java.lang.reflect.Method, java.lang.Object[])
   */
  public Object invoke(Object object, Method method, Object[] args) throws Throwable {
    if (osgiService == null) {
      throw new IllegalStateException("Service is not yet available, last Event was  :"
          + lastEvent);
    }
    return method.invoke(osgiService, args);
  }
}
