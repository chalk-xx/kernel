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
package org.sakaiproject.nakamura.userinitializer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.util.osgi.BindingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Component to make selected pre-defined Jackrabbit Authorizable objects into Sakai 3
 * User or Group entities by setting required properties and applying required
 * post-processors.
 */
@Component(metatype = true, immediate = true)
public class UserInitializerService implements BindingListener {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(UserInitializerService.class);

  @Reference
  protected AuthorizablePostProcessService authorizablePostProcessService;
  @Reference
  protected SlingRepository repository;

  @Property(value = {      
      "org.sakaiproject.nakamura.personal.PersonalAuthorizablePostProcessor",
      "org.sakaiproject.nakamura.calendar.CalendarAuthorizablePostProcessor",
      "org.sakaiproject.nakamura.connections.ConnectionsUserPostProcessor",
      "org.sakaiproject.nakamura.message.MessageAuthorizablePostProcessor",
      "org.sakaiproject.nakamura.pages.PagesAuthorizablePostProcessor" 
      }, 
      description = "LIst of service IDs that are required for user initialization ", 
      cardinality = 2147483647)
  static final String REQUIRED_SERVICE_ID = "required.service";

  private URL[] jsonFiles = new URL[0];

  private boolean initialized = false;

  private String[] requiredServiceIds;

  // ----------- OSGi integration ----------------------------

  @Activate
  protected void activate(ComponentContext componentContext) throws MalformedURLException {
    requiredServiceIds = OsgiUtil.toStringArray(componentContext.getProperties().get(REQUIRED_SERVICE_ID),
        new String[0]);
    Enumeration<?> entriesEnum = componentContext.getBundleContext().getBundle()
        .findEntries("users", "*.json", true);
    List<URL> urls = new ArrayList<URL>();
    while (entriesEnum.hasMoreElements()) {
      Object entry = entriesEnum.nextElement();
      urls.add(new URL(entry.toString()));
    }
    jsonFiles = urls.toArray(new URL[urls.size()]);
    notifyBinding();
    // only bind as a listener if there are services to wait on and json files.
    if (!initialized && requiredServiceIds.length > 0 && jsonFiles.length > 0) {
      authorizablePostProcessService.addListener(this);
    }
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    authorizablePostProcessService.removeListener(this);
  }

  public void notifyBinding() {
    if (!initialized && requiredServiceIds.length > 0 && jsonFiles.length > 0) {
      String[] services = authorizablePostProcessService.getRegisteredServiceIDs();
      Set<String> serviceIdSet = new HashSet<String>();
      for (String requiredServiceId : requiredServiceIds) {
        serviceIdSet.add(requiredServiceId);
      }
      for (String serviceId : services) {
        LOGGER.info("Got Service {} ", serviceId);
        serviceIdSet.remove(serviceId);
      }
      if (serviceIdSet.size() == 0) {
        LOGGER.info("All services bound, initializing default users ");
        DefaultAuthorizablesLoader defaultAuthorizablesLoader = new DefaultAuthorizablesLoader();
        initialized = defaultAuthorizablesLoader.initDefaultUsers(
            authorizablePostProcessService, jsonFiles, repository);
      } else {
        LOGGER.info("Waiting for remaining services to bind: [{}] ",
            Arrays.toString(serviceIdSet.toArray()));
      }
    }
  }
}
