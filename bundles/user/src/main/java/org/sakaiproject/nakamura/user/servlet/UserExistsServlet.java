/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.servlet;


import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling GET Servlet for checking for the existence of a user.
 * </p>
 * <h2>REST Service Description</h2>
 * <p>
 * Checks for the existence of a user with the given id. This servlet responds at
 * <code>/system/userManager/user.exists.html?userid=</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>GET</li>
 * </ul>
 * <h4>GET Parameters</h4>
 * <dl>
 * <dt>userid</dt>
 * <dd>The name of the user to check for</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, user exists.</dd>
 * <dt>404</dt>
 * <dd>User does not exist.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl http://localhost:8080/system/userManager/user.exists.html?userid=foo
 * </code>
 *
 * <h4>Notes</h4>
 *
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/users"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="exists"
 *
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 *
 *
 */
@ServiceDocumentation(name="User Exists Servlet",
    description="Tests for existence of user. This servlet responds at /system/userManager/user.exists.html",
    shortDescription="Tests for existence of user",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager/user.exists.html",
        selectors=@ServiceSelector(name="exists", description="Tests for existence of user."),
        extensions=@ServiceExtension(name="html", description="GETs produce HTML with request status.")),
    methods=@ServiceMethod(name="GET",
        description={"Checks for existence of user with id supplied in the userid parameter."},
        parameters={
        @ServiceParameter(name="userid", description="The id of the user to check for (required)")},
        response={
        @ServiceResponse(code=200,description="Success, a redirect is sent to the groups resource locator with HTML describing status."),
        @ServiceResponse(code=500,description="Failure, including group already exists. HTML explains failure."),
        @ServiceResponse(code=400,description="Bad request: the required userid parameter was missing.")
        }))

public class UserExistsServlet extends SlingSafeMethodsServlet {



  /**
   * 
   */
  private static final long serialVersionUID = 7051557537133012560L;
  
  private static final Logger LOGGER = LoggerFactory
      .getLogger(UserExistsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      RequestParameter idParam = request.getRequestParameter("userid");
      if (idParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This request must have a 'userid' parameter.");
        return;
      }
      String id = idParam.getString();
      LOGGER.debug("Checking for existence of {}", id);
      if (session != null) {
          UserManager userManager = AccessControlUtil.getUserManager(session);
          if (userManager != null) {
              Authorizable authorizable = userManager.getAuthorizable(id);
              if (authorizable != null) {
                  response.setStatus(HttpServletResponse.SC_OK);
              } else response.sendError(HttpServletResponse.SC_NOT_FOUND);
          }
      }
    } catch (RepositoryException e) {
      
    } finally {
      LOGGER.debug("checking for existence took {} ms", System.currentTimeMillis() - start);
    }
  }

}
