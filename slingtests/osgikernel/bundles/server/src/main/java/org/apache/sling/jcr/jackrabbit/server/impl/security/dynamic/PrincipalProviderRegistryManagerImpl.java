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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrincipalProviderRegistryManagerImpl extends ServiceTracker implements
    PrincipalProviderRegistryManager {

  private List<PrincipalProvider> testServices = new ArrayList<PrincipalProvider>();

  /**
   * @param bundleContext
   */
  public PrincipalProviderRegistryManagerImpl(BundleContext bundleContext) {
    super(bundleContext, PrincipalProvider.class.getName(),null);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.PrincipalProviderRegistryManager#getPrincipalProvider(org.apache.jackrabbit.core.security.principal.PrincipalProvider)
   */
  public PrincipalProviderRegistry getPrincipalProvider(PrincipalProvider defaultPrincipalProvider) {
    return new DynamicProviderRegistryImpl(defaultPrincipalProvider, (PrincipalProvider[]) getServices(), testServices);
  }

  /**
   * @param rulesPrincipalProvider
   */
  protected void addProvider(RulesPrincipalProvider rulesPrincipalProvider) {
    testServices.add(rulesPrincipalProvider);
  }




}
