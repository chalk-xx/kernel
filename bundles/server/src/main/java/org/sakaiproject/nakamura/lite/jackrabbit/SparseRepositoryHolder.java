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
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.SakaiActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Component(immediate=true, metatype=true) manually configured in serviceComponents.xml
// @Service
public class SparseRepositoryHolder implements JackrabbitRepositoryStartupService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SparseRepositoryHolder.class);
  private static Repository sparseRepositoryInstance;
  private SakaiActivator sakaiActivator;

  public void activate(ComponentContext componentContext) {
    BundleContext bundleContext = componentContext.getBundleContext();
    sakaiActivator = new SakaiActivator();
    sakaiActivator.start(bundleContext);
  }

  public void deactivate(ComponentContext componentContext) {
    BundleContext bundleContext = componentContext.getBundleContext();
    sakaiActivator.stop(bundleContext);
    sakaiActivator = null;
  }

  public void bindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(repository);
  }

  public void unbindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(null);
  }

  public static void setSparseRespository(Repository repository) {
    sparseRepositoryInstance = repository;
  }

  public static Repository getSparseRepositoryInstance() {
    if (sparseRepositoryInstance == null) {
      LOGGER
          .warn("No Sparse Repository availalbe at this time, has the SparseComponentHolder been activated ?");
    }
    return sparseRepositoryInstance;
  }
}
