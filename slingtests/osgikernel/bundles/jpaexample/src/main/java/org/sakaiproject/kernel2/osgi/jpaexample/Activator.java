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


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel2.osgi.simple.ActivatorHelper;
import org.sakaiproject.kernel2.osgi.simple.DelayedActivation;
import org.sakaiproject.kernel2.osgi.simple.ServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class Activator implements BundleActivator, DelayedActivation {

  protected static final Logger LOG = LoggerFactory.getLogger(Activator.class);
  
  public void go(BundleContext bc, ServiceResolver bundleResolver, ServiceResolver startupResolver) throws Exception {
    LOG.info("Going");
    EntityManager em = startupResolver.getService(EntityManager.class);
    JpaExample example = new JpaExample(em);
    example.exercise();
  }

  public void start(BundleContext bc) throws Exception {
    LOG.info("Starting");
    ActivatorHelper.startServices(new Class<?>[]{EntityManager.class},bc,this);
  }

  public void stop(BundleContext bc) throws Exception {
    LOG.info("Stopping");
    ActivatorHelper.stopServices(bc);
  }

}
