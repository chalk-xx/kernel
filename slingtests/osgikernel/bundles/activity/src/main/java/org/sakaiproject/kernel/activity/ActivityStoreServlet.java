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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@SlingServlet(resourceTypes="sakai/activityStore",methods={"GET","POST","PUT","DELETE"})
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for activity stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "sakai/activityStore BigStore", description = "BigStore URL hash mapping for sakai/activityStore", bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/activityStore") }, methods = {
		@ServiceMethod(name = "GET", description = ""),
		@ServiceMethod(name = "POST", description = ""),
		@ServiceMethod(name = "PUT", description = ""),
		@ServiceMethod(name = "DELETE", description = "") })
public class ActivityStoreServlet extends AbstractVirtualPathServlet {
  private static final long serialVersionUID = -8014319281629970139L;
  private static final Logger LOG = LoggerFactory.getLogger(ActivityStoreServlet.class);

  @Reference
  protected transient VirtualResourceProvider virtualResourceProvider;

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
