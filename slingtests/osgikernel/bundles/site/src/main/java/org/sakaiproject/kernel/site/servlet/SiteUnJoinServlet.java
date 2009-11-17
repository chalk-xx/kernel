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
package org.sakaiproject.kernel.site.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceExtension;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceJoinServlet</code> supports Join, UnJoin for joinable sites and
 * initiates authorized Joining workflow.
 * 
 * @scr.component immediate="true" label="SiteJoinServlet"
 *                description="Join servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Supports Join functionality on the site."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="unjoin"
 */
@ServiceDocumentation(name="Site UnJoin Servlet",
    description=" The <code>SiteServiceJoinServlet</code> supports UnJoin for sites.",
    shortDescription="Supports site UnJoin operations.",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sakai/site"},
        selectors=@ServiceSelector(name="unjoin", description="Initiates UnJoining workflow"),
        extensions=@ServiceExtension(name="html", description="A standard HTML response for creating a node.")),
    methods=@ServiceMethod(name="POST",
        description={"UnJoins the user to the site in the specified group. The group must exist and be associated with the site. " +
            "The site must exist.",
            "Example<br>" +
            "<pre>curl -FtargetGroup=g-qastaff http://user:pass@localhost:8080/sites/physics101/year3.unjoin.html</pre>"
        },
        parameters={
          @ServiceParameter(name="targetGroup", description="The group to remove the user from (required)")
        
        },
        response={
          @ServiceResponse(code=200,description="The body will be empty on sucess."),
          @ServiceResponse(code=204,description="When A site is not found"),
          @ServiceResponse(code=400,description={
              "If the location does not represent a site.",
              "The target group is not a group.",
              "The group is not associated with the site.",
              "The user is not a member of the site"
          }),
          @ServiceResponse(code=403,description="Current user is not allowed to unjoin."),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=409,description="User is not a member"),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    )) 
public class SiteUnJoinServlet extends AbstractSiteServlet {

  /**
   *
   */
  private static final long serialVersionUID = 7673360724593565303L;
  /**
   *
   */
  private static final Logger LOG = LoggerFactory.getLogger(SiteUnJoinServlet.class);


  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOG.info("Got get to SiteServiceGetServlet");
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
      return;
    }
    if (!getSiteService().isSite(site)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Location does not represent site ");
      return;
    }
    RequestParameter requestedGroup = request
        .getRequestParameter(SiteService.PARAM_GROUP);
    if (requestedGroup == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Target Group must be specified in the request parameter "
              + SiteService.PARAM_GROUP);
      return;
    }

    try {
      getSiteService().unjoinSite(site, requestedGroup.getString());
      response.sendError(HttpServletResponse.SC_OK);
    } catch (SiteException ex) {
      response.sendError(ex.getStatusCode(), ex.getMessage());
    }
    return;

  }

}
