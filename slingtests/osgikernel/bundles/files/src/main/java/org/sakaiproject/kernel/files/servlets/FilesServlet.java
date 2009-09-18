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
package org.sakaiproject.kernel.files.servlets;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.util.PathUtils;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/files"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="PUT"
 *               values.2="DELETE" values.3="GET"
 */
public class FilesServlet extends AbstractVirtualPathServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -1960932906632564021L;

  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    if (virtualPath.indexOf("/") != -1) {
      String[] virtualParts = StringUtils.split(virtualPath, "/");
      return PathUtils.toInternalHashedPath(realPath, virtualParts[0], "") + "/" + virtualParts[1];
      
    } else {
      return PathUtils.toInternalHashedPath(realPath, virtualPath, "");
    }
  }
  
  
  @Override
  public boolean removeSelectors(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    return false;
  }
}
