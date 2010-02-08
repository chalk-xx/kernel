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
package org.sakaiproject.nakamura.personal;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet;
import org.sakaiproject.nakamura.resource.VirtualResourceProvider;
import org.sakaiproject.nakamura.util.PathUtils;

/**
 */
@ServiceDocumentation(name="Personal Servlet",
    shortDescription="Provides access to a content tree bound to the current user",
    description="Provides access to a content tree where a generic URL is mapped to an internal store that is bound" +
    		"to the current user. The incomming URL request is converted into a URL bound to the user and the URL passed back to" +
    		"the Sling Engine for further processing. The root node of a personal store is marked with the resource Type sakai/personalPrivate" +
    		"and has ACL's applied that restrict access so that only the current user can access their own store. At the moment the mapping between URL" +
    		"and the final resource takes the form URL: /_user/private/pathinfo where /_user/private is a node of type sakai/personalPrivate and where pathinfo " +
    		"is the remaining path of the url eg widgets/portal/preferences. This URL is translated into a Resource as /_user/private/shard{currentUser}/pathinfo " +
    		"where shard{currentUser} is a function of current user that produces a deterministic path. At the moment we hare sharding based on a SHA1 hash of the " +
    		"current user which results in resource paths of the form /_user/private/3e/ff/4e/de/widgets/portal/preferences. Having converted the URL into a concrete resource path " +
    		"that path is re-presented to the Sling Engine which takes over processing of the request on the real resource.",
    		bindings=@ServiceBinding(type=BindingType.TYPE, bindings="sakai/personalPrivate"),
    		methods={
          @ServiceMethod(name="GET",
              description="Operation is dependent on the servlet that the request is redirected to, for instance if a GET was made to " +
              		"/_user/personal/setup.json it might be processed by the SlingDefaultGetServlet serializing the node properties " +
              		"as a json tree"),
         @ServiceMethod(name="POST",
             description="Operation is dependent on the servlet that the request is redirected to, for instance if a POST was made to " +
             "/_user/personal/setup it might be processed by the SlingDefaultPostServlet setting properties of the node to parameters in the post"),
         @ServiceMethod(name="PUT",
             description="Operation is routed through the SlingEngine to the servlet that handles PUT methods for the final resource."),
         @ServiceMethod(name="DELETE",
             description="Operation is routed through the SlingEngine to the servlet that handles DELETE methods for the final resource.")
            }
)
@SlingServlet(resourceTypes="sakai/personalPrivate",methods={"GET","POST","PUT","DELETE"})
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for private/personal user stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class PersonalServlet extends AbstractVirtualPathServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  @Reference
  protected transient VirtualResourceProvider virtualResourceProvider;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet#getTargetPath(org.apache.sling.api.resource.Resource, org.apache.sling.api.SlingHttpServletRequest, SlingHttpServletResponse, java.lang.String, java.lang.String)
   */
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    String userId = request.getRemoteUser();
    return PathUtils.toInternalHashedPath(realPath, userId, virtualPath);

  }


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet#getVirtualResourceProvider()
   */
  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return virtualResourceProvider;
  }


}
