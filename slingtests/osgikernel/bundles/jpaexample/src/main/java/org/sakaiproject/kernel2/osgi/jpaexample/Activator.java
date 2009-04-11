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
package org.sakaiproject.kernel2.osgi.jpaexample;


import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel.api.registry.ComponentLifecycle;
import org.sakaiproject.kernel.api.registry.RegistryService;
import org.sakaiproject.kernel.api.registry.utils.RegistryServiceUtil;
import org.sakaiproject.kernel.guice.AbstractOsgiModule;
import org.sakaiproject.kernel.guice.GuiceActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends GuiceActivator implements ComponentLifecycle {

  protected static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
  
  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.guice.GuiceActivator#getModule()
   */
  @Override
  protected AbstractOsgiModule getModule(BundleContext bundleContext) {
    return new ActivatorModule(bundleContext);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.guice.GuiceActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    RegistryServiceUtil.addComponentLifecycle(injector.getInstance(RegistryService.class),this);    
  }
  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.guice.GuiceActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    RegistryServiceUtil.removeComponentLifecycle(injector.getInstance(RegistryService.class),this);    
    super.stop(bundleContext);
  }
  
  
  

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.registry.Provider#getKey()
   */
  public String getKey() {
    return this.getClass().getName();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.registry.Provider#getPriority()
   */
  public int getPriority() {
    // we dont care when this is started, last will do.
    return 0;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.registry.ComponentLifecycle#init()
   */
  public void init() {
    LOGGER.info("Starting JPA Example");
    JpaExample example = injector.getInstance(JpaExample.class);
    example.exercise();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.registry.ComponentLifecycle#destroy()
   */
  public void destroy() {
    
  }


}
