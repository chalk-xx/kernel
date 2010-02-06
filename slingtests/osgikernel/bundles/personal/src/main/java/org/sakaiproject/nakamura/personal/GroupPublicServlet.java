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
@ServiceDocumentation(name="Group Public Servlet",
    shortDescription="Provides access to a content tree bound to a named Group",
    description={"Provides access to a content tree where a URL is mapped to an internal store that is bound" +
        "to a group. The first element of the URL after the target resource type specified the group. Once the URL is processed into a resource type " +
        "it is passed back to the Sling Engine for further processing. The root node of a public group store is marked with the resource Type sakai/groupPublic" +
        "and has ACL's applied that restrict access so that only the members of the group with jcr:write can write to the resource. By default everyone can read the contents " +
        "of a group public store. ",
        "At the moment the mapping between URL" +
        "and the final resource takes the form URL: /_group/public/groupid/pathinfo where /_group/public is a node of type sakai/groupPublic, groupid is the id of the group " +
        "and pathinfo is the remaining path of the url eg term3/readinglist. This URL is translated into a Resource as /_group/public/shard{groupid}/pathinfo " +
        "where shard{groupid} is a function of groupid that produces a deterministic path. At the moment we hare sharding based on a SHA1 hash of the " +
        "groupid which results in resource paths of the form /_group/public/3e/ff/4e/de/groupid/term3/readinglist. Having converted the URL into a concrete resource path " +
        "that path is re-presented to the Sling Engine which takes over processing of the request on the real resource."},
        bindings=@ServiceBinding(type=BindingType.TYPE, bindings="sakai/groupPublic"),
        methods={
          @ServiceMethod(name="GET",
              description="Operation is dependent on the servlet that the request is redirected to, for instance if a GET was made to " +
                  "/_group/public/physics101/term3/readinglist.json it might be processed by the SlingDefaultGetServlet serializing the node properties " +
                  "as a json tree"),
         @ServiceMethod(name="POST",
             description="Operation is dependent on the servlet that the request is redirected to, for instance if a POST was made to " +
             "/_group/public/physics101/term3/readinglist it might be processed by the SlingDefaultPostServlet setting properties of the node to parameters in the post"),
         @ServiceMethod(name="PUT",
             description="Operation is routed through the SlingEngine to the servlet that handles PUT methods for the final resource."),
         @ServiceMethod(name="DELETE",
             description="Operation is routed through the SlingEngine to the servlet that handles DELETE methods for the final resource.")
            }
)
@SlingServlet(resourceTypes="sakai/groupPublic",methods={"GET","POST","PUT","DELETE"})
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for group public user stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class GroupPublicServlet extends AbstractVirtualPathServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  @Reference
  protected transient VirtualResourceProvider virtualResourceProvider;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.personal.AbstractPersonalServlet#getTargetPath(org.apache.sling.api.resource.Resource,
   *      org.apache.sling.api.SlingHttpServletRequest)
   */
  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request, SlingHttpServletResponse response, String realPath, String virtualPath) {
    String[] pathParts = PathUtils.getNodePathParts(virtualPath);
    return PathUtils.toInternalHashedPath(realPath, pathParts[0], pathParts[1]);
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
