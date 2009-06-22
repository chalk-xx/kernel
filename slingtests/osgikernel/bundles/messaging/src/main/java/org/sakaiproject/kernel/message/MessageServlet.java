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
package org.sakaiproject.kernel.message;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.util.PathUtils;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="GET"
 */
public class MessageServlet extends AbstractMessageServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.resource.AbstractVirtualPathServlet#getTargetPath(org.apache.sling.api.resource.Resource,
   *      org.apache.sling.api.SlingHttpServletRequest, java.lang.String,
   *      java.lang.String)
   */
  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      String realPath, String virtualPath) {
    String[] parts = PathUtils.getNodePathParts(virtualPath);
    return PathUtils.toInternalHashedPath(realPath, parts[0], parts[1]);
  }
}
