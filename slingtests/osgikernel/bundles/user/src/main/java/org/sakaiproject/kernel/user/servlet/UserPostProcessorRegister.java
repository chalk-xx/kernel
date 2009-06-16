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
package org.sakaiproject.kernel.user.servlet;

import static org.sakaiproject.kernel.api.user.UserConstants.USER_POST_PROCESSOR;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.user.UserPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 */
public class UserPostProcessorRegister {
  private Map<Long, UserPostProcessor> processors = new ConcurrentHashMap<Long, UserPostProcessor>();
  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();

  protected void bindUserPostProcessor(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.add(serviceReference);
      } else {
        addProcessor(serviceReference);
      }
    }

  }

  protected void unbindUserPostProcessor(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.remove(serviceReference);
      } else {
        removeProcessor(serviceReference);
      }
    }

  }

  /**
   * @param serviceReference
   */
  private void removeProcessor(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    processors.remove(serviceId);
  }

  /**
   * @param serviceReference
   */
  private void addProcessor(ServiceReference serviceReference) {
    UserPostProcessor processor = (UserPostProcessor) osgiComponentContext.locateService(
        USER_POST_PROCESSOR, serviceReference);
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    processors.put(serviceId, processor);
  }

  /**
   * @param componentContext
   */
  public void setComponentContext(ComponentContext componentContext) {
    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        addProcessor(ref);
      }
      delayedReferences.clear();
    }
  }

  /**
   * @return
   */
  public Iterable<UserPostProcessor> getProcessors() {
    return processors.values();
  }

}
