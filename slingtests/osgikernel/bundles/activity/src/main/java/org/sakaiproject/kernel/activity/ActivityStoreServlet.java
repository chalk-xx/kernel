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
package org.sakaiproject.kernel.activity;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/activityStore"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST"
 *               values.2="PUT" values.3="DELETE"
 */
public class ActivityStoreServlet extends AbstractVirtualPathServlet {
  private static final long serialVersionUID = -8014319281629970139L;
  private static final Logger LOG = LoggerFactory.getLogger(ActivityStoreServlet.class);

  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    return getHashedPath(realPath, virtualPath);
  }

  public static String getHashedPath(String realPath, String virtualPath) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getHashedPath(String {}, String {})", new Object[] { realPath,
          virtualPath });
    }
    String[] pathParts = PathUtils.getNodePathParts(virtualPath);
    LOG.debug(pathParts.toString());
    return PathUtils.toInternalHashedPath(realPath, pathParts[0], pathParts[1]);
  }

}
