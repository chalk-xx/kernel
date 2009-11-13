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
package org.sakaiproject.kernel.personal;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.PathUtils;

/**
 */
@SlingServlet(resourceTypes="sakai/personalPublic",methods={"GET","POST","PUT","DELETE"})
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for public user stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class PublicServlet extends AbstractVirtualPathServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  @Reference
  protected VirtualResourceProvider virtualResourceProvider;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.personal.AbstractPersonalServlet#getTargetPath(org.apache.sling.api.resource.Resource,
   *      org.apache.sling.api.SlingHttpServletRequest)
   */
  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request, SlingHttpServletResponse response, String realPath, String virtualPath) {
    String[] pathParts = PathUtils.getNodePathParts(virtualPath);
    return PathUtils.toInternalHashedPath(realPath, pathParts[0], pathParts[1]);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.resource.AbstractVirtualPathServlet#getVirtualResourceProvider()
   */
  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return virtualResourceProvider;
  }


}
