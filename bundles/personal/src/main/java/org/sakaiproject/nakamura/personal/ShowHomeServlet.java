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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionValidator;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>ShowHomeServlet</code>
 */
@ServiceDocumentation(name = "Show Home",
  description = " Shows the content of /dev/show.html when requested ",
  shortDescription = "Shows html page for users",
  bindings = @ServiceBinding(type = BindingType.TYPE,
    bindings = { "sakai/user-home", "sakai/group-home" }
  ),
  methods = @ServiceMethod(name = "GET",
    description = { "Shows  a HTML page when the user or groups home is accessed" },
    response = {
      @ServiceResponse(code = 200, description = "A HTML view of the User or Group entity's home space.")
    })
)
@Component(immediate = true, label = "%home.showServlet.label", description = "%home.showServlet.desc")
@Services({
  @Service(Servlet.class),
  @Service(ServerProtectionValidator.class)
})
@SlingServlet(resourceTypes = { "sakai/user-home", "sakai/group-home" }, methods = { "GET" }, generateComponent = false, generateService = false)
public class ShowHomeServlet extends SlingSafeMethodsServlet implements OptingServlet, ServerProtectionValidator {

  private static final long serialVersionUID = 613629169503411716L;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Renders user and groups")
  static final String SERVICE_DESCRIPTION = "service.description";

  public static final String DEFAULT_GROUP_HOME_RES = "/dev/group.html";
  @Property(value = DEFAULT_GROUP_HOME_RES)
  static final String GROUP_HOME_RES = "sakai.group.home";
  private String groupHome;

  public static final String DEFAULT_USER_HOME_RES = "/dev/user.html";
  @Property(value = DEFAULT_USER_HOME_RES)
  static final String USER_HOME_RES = "sakai.user.home";
  private String userHome;

  @Activate @Modified
  protected void activate(Map<?, ?> props) {
    groupHome = OsgiUtil.toString(props.get(GROUP_HOME_RES), DEFAULT_GROUP_HOME_RES);
    userHome = OsgiUtil.toString(props.get(USER_HOME_RES), DEFAULT_USER_HOME_RES);
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String resourceType = request.getResource().getResourceType();

    Resource resource = null;
    if (UserConstants.GROUP_HOME_RESOURCE_TYPE.equals(resourceType)) {
      resource = request.getResourceResolver().getResource(groupHome);
    } else if (UserConstants.USER_HOME_RESOURCE_TYPE.equals(resourceType)) {
      resource = request.getResourceResolver().getResource(userHome);
    }

    if (resource == null) {
      response.sendError(500, "Somehow didn't get a user or group home resource [" + resourceType + "]");
    } else {
      response.setContentType("text/html");
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpServletResponse.SC_OK);
      IOUtils.stream(resource.adaptTo(InputStream.class), response.getOutputStream());
    }
  }

  /**
   * Let other servlets take care of requests for JSON.
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    String extension = request.getRequestPathInfo().getExtension();
    if ( extension == null || extension.length() == 0 ) {
      return true;
    } else {
      return false;
    }
  }

  public boolean safeToStream(SlingHttpServletRequest request, Resource resource) {
    if ( "GET".equals(request.getMethod()) && accepts(request) ) {
      String resourceType = resource.getResourceType();
      if ( "sakai/user-home".equals(resourceType) || "sakai/group-home".equals(resourceType) ) {
        return true;
      }
    }
    return false;
  }

}
