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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;

import java.util.Map;

/**
 * Provides configuration support.
 */
public abstract class AbstractOsgiModule extends AbstractModule {
  
  /**
   * {@inheritDoc}
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    OsgiServiceProvider<ConfigurationService> configurationServiceProvider = new OsgiServiceProvider<ConfigurationService>(ConfigurationService.class,getBundleContext());
    ConfigurationService configurationService = configurationServiceProvider.get();
    Map<String, String> config = configurationService.getProperties();
    if (config != null) {
      Names.bindProperties(this.binder(), config);
    }
  }

  /**
   * @return the current bundle context
   */
  protected abstract BundleContext getBundleContext();

}
