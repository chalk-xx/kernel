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
package org.sakaiproject.kernel.meservice;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.kernel.api.session.SessionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;



/**
 * The <code>MeServiceResourceProvider</code>
 * 
 * @scr.component immediate="true" label="MeServiceResourceProvider"
 *                description="MeService resource provider"
 * @scr.property name="service.description"
 *                value="Handles requests for Me Service resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="provider.roots" value="/system/me/"
 * @scr.service interface="org.apache.sling.api.resource.ResourceProvider"
 * @scr.reference name="sessionManagerService"
 *                interface="org.sakaiproject.kernel.api.session.SessionManagerService"
 *                bind="bindSessionManagerService"
 *                unbind="unbindSessionManagerService"
 */
public class MeServiceResourceProvider implements ResourceProvider  {

  private static Logger LOG = LoggerFactory.getLogger(MeServiceResourceProvider.class);
  
  protected SessionManagerService sessionManagerService;  

  public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request,
      String path) {
    return getResource(resourceResolver, path);
  }

  public Resource getResource(ResourceResolver resourceResolver, String path) {
    LOG.info("Looking for resource at " + path);
    return new MeResource(resourceResolver, path, "sakai:user");
  }

  public Iterator<Resource> listChildren(Resource parent) {
    return null;
  }

  protected void bindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = sessionManagerService;
  }

  protected void unbindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = null;
  }

}
