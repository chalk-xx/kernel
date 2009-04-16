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

package org.sakaiproject.kernel.session;

import com.google.inject.Scopes;

import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.session.SessionManagerService;
import org.sakaiproject.kernel.guice.AbstractOsgiModule;

/**
 * Configuration module for persistence bindings.
 */
public class ActivatorModule extends AbstractOsgiModule {

  private BundleContext bundleContext;

  public ActivatorModule(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    super.configure();

    // bind in the current context
    bind(BundleContext.class).toInstance(bundleContext);

    // bind the EntityManager to a ScopedEntityManger
    bind(export(SessionManagerService.class)).to(SessionManagerServiceImpl.class).in(
        Scopes.SINGLETON);

    bind(CacheManagerService.class).toProvider(importService(CacheManagerService.class)).in(
        Scopes.SINGLETON);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.guice.AbstractOsgiModule#getBundleContext()
   */
  @Override
  protected BundleContext getBundleContext() {
    return bundleContext;
  }
}
