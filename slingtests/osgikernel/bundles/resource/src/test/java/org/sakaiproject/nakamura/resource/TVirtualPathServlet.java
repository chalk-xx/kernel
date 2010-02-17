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
package org.sakaiproject.nakamura.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

/**
 *
 */
public class TVirtualPathServlet extends AbstractVirtualPathServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -5414317442082458564L;
  private VirtualResourceProvider vp;

  public TVirtualPathServlet(VirtualResourceProvider vp) {
    this.vp  = vp;
  }
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet#getTargetPath(org.apache.sling.api.resource.Resource, org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse, java.lang.String, java.lang.String)
   */
  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    return "targetpath";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet#getVirtualResourceProvider()
   */
  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return vp;
  }

}
