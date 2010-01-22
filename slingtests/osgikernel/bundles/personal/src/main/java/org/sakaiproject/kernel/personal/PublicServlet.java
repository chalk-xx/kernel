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
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.PathUtils;

/**
 */
@ServiceDocumentation(name="Personal Public Servlet",
    shortDescription="Provides access to a content tree bound to a named Person",
    description={"Provides access to a content tree where a URL is mapped to an internal store that is bound" +
        "to a user. The first element of the URL after the target resource type specifies the user. Once the URL is processed into a resource type " +
        "it is passed back to the Sling Engine for further processing. The root node of a public personal store is marked with the resource Type sakai/personalPublic" +
        "and has ACL's applied that restrict access so that only the user can write to the resource. By default everyone can read the contents " +
        "of a personal public store. ",
        "At the moment the mapping between URL" +
        "and the final resource takes the form URL: /_user/public/userid/pathinfo where /_user/public is a node of type sakai/personalPublic, userid is the id of the user " +
        "and pathinfo is the remaining path of the url eg term3/myreadinglist. This URL is translated into a Resource as /_group/public/shard{userid}/pathinfo " +
        "where shard{userid} is a function of userid that produces a deterministic path. At the moment we have sharding based on a SHA1 hash of the " +
        "userid which results in resource paths of the form /_user/public/3e/ff/4e/de/ieb/term3/myreadinglist. Having converted the URL into a concrete resource path " +
        "that path is re-presented to the Sling Engine which takes over processing of the request on the real resource."},
        bindings=@ServiceBinding(type=BindingType.TYPE, bindings="sakai/personalPublic"),
        methods={
          @ServiceMethod(name="GET",
              description="Operation is dependent on the servlet that the request is redirected to, for instance if a GET was made to " +
                  "/_user/public/ieb/term3/myreadinglist.json it might be processed by the SlingDefaultGetServlet serializing the node properties " +
                  "as a json tree"),
         @ServiceMethod(name="POST",
             description="Operation is dependent on the servlet that the request is redirected to, for instance if a POST was made to " +
             "/_user/public/ieb/term3/myreadinglist it might be processed by the SlingDefaultPostServlet setting properties of the node to parameters in the post"),
         @ServiceMethod(name="PUT",
             description="Operation is routed through the SlingEngine to the servlet that handles PUT methods for the final resource."),
         @ServiceMethod(name="DELETE",
             description="Operation is routed through the SlingEngine to the servlet that handles DELETE methods for the final resource.")
            }
)
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
  protected transient VirtualResourceProvider virtualResourceProvider;

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
