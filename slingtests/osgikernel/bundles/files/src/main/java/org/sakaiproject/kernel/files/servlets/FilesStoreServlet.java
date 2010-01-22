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
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(resourceTypes = { "sakai/files" }, methods = { "POST", "PUT", "DELETE",
    "GET" })
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for file stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(
    name = "FilesStoreServlet", shortDescription = "BigStore servlet for files", 
    description = "This servlet transforms requests from /_user/files/grades.doc to a hashed path like /_user/files/aa/bb/cc/dd/grades.doc",
    bindings = {@ServiceBinding(
        type = BindingType.PATH, 
        bindings="sakai/files"
    )},
    methods = {
      @ServiceMethod(name = "GET", description = "This servlet transforms GET requests to the hashed address."), 
      @ServiceMethod(name = "POST", description = "This servlet transforms POST requests to the hashed address."),
      @ServiceMethod(name = "PUT", description = "This servlet transforms PUT requests to the hashed address."), 
      @ServiceMethod(name = "DELETE", description = "This servlet transforms DELETE requests to the hashed address.")
    }
)
public class FilesStoreServlet extends AbstractVirtualPathServlet {

  public static final Logger LOGGER = LoggerFactory.getLogger(FilesStoreServlet.class);
  private static final long serialVersionUID = -1960932906632564021L;

  @Reference
  protected transient VirtualResourceProvider virtualResourceProvider;

  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {

    String[] parts = StringUtils.split(virtualPath, '.');

    LOGGER.info("Rewriting the url in filesstore: ", new Object[] { realPath,
        virtualPath, PathUtils.toInternalHashedPath(realPath, parts[0], "") });

    String sel = "";
    for (int i = 1; i < parts.length; i++) {
      sel += "." + parts[i];
    }

    return PathUtils.toInternalHashedPath(realPath, parts[0], sel);
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
