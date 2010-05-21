/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.osgi.framework.BundleContext;

/**
 * The <code>Activator</code> 
 */
public class SakaiActivator extends Activator {
  
  
  private static DynamicPrincipalManagerFactoryImpl dynamicPrincipalManagerFactory;
  private static RuleProcessorManagerImpl ruleProcessorManager;
  private static PrincipalProviderRegistryManagerImpl principalProviderRegistryManager;

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.Activator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) {
    super.start(bundleContext);
    
    if (dynamicPrincipalManagerFactory == null) {
      dynamicPrincipalManagerFactory = new DynamicPrincipalManagerFactoryImpl(
          bundleContext);
    }
    dynamicPrincipalManagerFactory.open();

    if (ruleProcessorManager == null) {
      ruleProcessorManager = new RuleProcessorManagerImpl(
          bundleContext);
    }
    ruleProcessorManager.open();

    if (principalProviderRegistryManager == null) {
      principalProviderRegistryManager = new PrincipalProviderRegistryManagerImpl(
          bundleContext);
    }
    principalProviderRegistryManager.open();

  }
  
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.Activator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext arg0) {
    if (dynamicPrincipalManagerFactory != null) {
      dynamicPrincipalManagerFactory.close();
      dynamicPrincipalManagerFactory = null;
    }
    if (ruleProcessorManager != null) {
      ruleProcessorManager.close();
      ruleProcessorManager = null;
    }
    if (principalProviderRegistryManager != null) {
      principalProviderRegistryManager.close();
      principalProviderRegistryManager = null;
    }
    super.stop(arg0);
  }
  /**
   * @return
   */
  public static DynamicPrincipalManagerFactory getDynamicPrincipalManagerFactory() {
    return dynamicPrincipalManagerFactory;
  }


  /**
   * @return
   */
  public static RuleProcessorManager getRuleProcessorManager() {
    return ruleProcessorManager;
  }
  /**
   * @return
   */
  public static PrincipalProviderRegistryManager getPrincipalProviderRegistryManager() {
    return principalProviderRegistryManager;
  }

  protected static void setDynamicPrincipalManagerFactory(DynamicPrincipalManagerFactoryImpl dynamicPrincipalManagerFactory) {
      SakaiActivator.dynamicPrincipalManagerFactory = dynamicPrincipalManagerFactory;
  }

  protected static void setRuleProcessorManager(RuleProcessorManagerImpl ruleProcessorManagerImpl) {
    SakaiActivator.ruleProcessorManager = ruleProcessorManagerImpl;
  }
  protected static void setPrincipalProviderManager(PrincipalProviderRegistryManagerImpl principalProviderRegistryManagerImpl) {
    SakaiActivator.principalProviderRegistryManager = principalProviderRegistryManagerImpl;
  }
}
