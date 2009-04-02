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
import org.osgi.framework.ServiceReference;

/**
 * Create a generic Guice provider that binds to a service in OSGi
 */
public class OsgiServiceProvider<T> implements
    Provider<T> {

  private BundleContext bundleContext;
  private ServiceReference serviceReference;

  /**
   * @param the interface representing the service
   * @param bundleContext the bundle context of this bundle
   */
  public OsgiServiceProvider(Class<T> serviceClass,
      BundleContext bundleContext) {
    this.bundleContext = bundleContext;
    // convert the service class into a reference, since the provision of the service class 
    // may change.
    this.serviceReference = bundleContext.getServiceReference(serviceClass.getName());
  }

  /**
   * {@inheritDoc}
   * @see com.google.inject.Provider#get()
   */
  @SuppressWarnings("unchecked")
  public T get() {
    return (T) bundleContext.getService(serviceReference);
  }

}
