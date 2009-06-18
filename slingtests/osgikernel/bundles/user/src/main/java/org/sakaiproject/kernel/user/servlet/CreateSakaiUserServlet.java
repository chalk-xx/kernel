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
package org.sakaiproject.kernel.user.servlet;

import static org.sakaiproject.kernel.api.user.UserConstants.DEFAULT_HASH_LEVELS;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.helper.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.impl.post.AbstractUserPostServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Servlet implementation for creating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new user. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user</code>. This servlet responds at
 * <code>/system/userManager/user.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new user (required)</dd>
 * <dt>:pwd</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt>:pwdConfirm</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt></dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including user already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -F:name=ieb -Fpwd=password -FpwdConfirm=password -Fproperty1=value1 http://localhost:8080/system/userManager/user.create.html
 * </code>
 * 
 * 
 * 
 * @scr.component immediate="true" label="%createUser.post.operation.name"
 *                description="%createUser.post.operation.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/users"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 * 
 * @scr.property name="password.digest.algorithm" value="sha1"
 * 
 * 
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 * 
 * 
 * @scr.property name="self.registration.enabled" label="%self.registration.enabled.name"
 *               description="%self.registration.enabled.description"
 *               valueRef="DEFAULT_SELF_REGISTRATION_ENABLED"
 *  
 * @scr.reference name="UserPostProcessor" bind="bindUserPostProcessor"
 *                unbind="unbindUserPostProcessor"
 *                interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 *                cardinality="0..n" policy="dynamic"
 */

public class CreateSakaiUserServlet extends AbstractUserPostServlet {

  /**
   *
   */
  private static final long serialVersionUID = -5060795742204221361L;

  
  private UserPostProcessorRegister postProcessorTracker = new UserPostProcessorRegister();

  /**
   * default log
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String PROP_SELF_REGISTRATION_ENABLED = "self.registration.enabled";
  private static final Boolean DEFAULT_SELF_REGISTRATION_ENABLED = Boolean.TRUE;

  private Boolean selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;

  /**
   * The JCR Repository we access to resolve resources
   * 
   * @scr.reference
   */
  private SlingRepository repository;

  /** Returns the JCR repository used by this service. */
  protected SlingRepository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   */
  private Session getSession() throws RepositoryException {
    return getRepository().loginAdministrative(null);
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        log.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  // ---------- SCR integration ---------------------------------------------

  /**
   * Activates this component.
   * 
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    Dictionary<?, ?> props = componentContext.getProperties();
    Object propValue = props.get(PROP_SELF_REGISTRATION_ENABLED);
    if (propValue instanceof String) {
      selfRegistrationEnabled = Boolean.parseBoolean((String) propValue);
    } else {
      selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;
    }
    postProcessorTracker.setComponentContext(componentContext);

  }

  /*
   * (non-Javadoc)
   * 
   * @seeorg.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#
   * handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    // make sure user self-registration is enabled
    if (!selfRegistrationEnabled) {
      throw new RepositoryException(
          "Sorry, registration of new users is not currently enabled.  Please try again later.");
    }

    Session session = request.getResourceResolver().adaptTo(Session.class);
    if (session == null) {
      throw new RepositoryException("JCR Session not found");
    }

    // check that the submitted parameter values have valid values.
    final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
    if (principalName == null) {
      throw new RepositoryException("User name was not submitted");
    }
    
    if (principalName.startsWith("g-")) {
      throw new RepositoryException("User name must not begin 'g-'");
    }
    
    String pwd = request.getParameter("pwd");
    if (pwd == null) {
      throw new RepositoryException("Password was not submitted");
    }
    String pwdConfirm = request.getParameter("pwdConfirm");
    if (!pwd.equals(pwdConfirm)) {
      throw new RepositoryException(
          "Password value does not match the confirmation password");
    }

    Session selfRegSession = null;
    try {
      selfRegSession = getSession();

      UserManager userManager = AccessControlUtil.getUserManager(selfRegSession);
      Authorizable authorizable = userManager.getAuthorizable(principalName);

      if (authorizable != null) {
        // user already exists!
        throw new RepositoryException(
            "A principal already exists with the requested name: " + principalName);
      } else {
        Map<String, RequestProperty> reqProperties = collectContent(request, response);

        User user = userManager.createUser(principalName, digestPassword(pwd),
            new Principal() {
              public String getName() {
                return principalName;
              }
            }, PathUtils.getUserPrefix(principalName, DEFAULT_HASH_LEVELS));
        String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
            + user.getID();
        

        log.debug("The user path is: {} ",userPath);

        response.setPath(userPath);
        response.setLocation(externalizePath(request, userPath));
        response.setParentLocation(externalizePath(request,
            AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH));
        changes.add(Modification.onCreated(userPath));

        // write content from form
        writeContent(selfRegSession, user, reqProperties, changes);

        if (selfRegSession.hasPendingChanges()) {
          selfRegSession.save();
        }
      }
    } finally {
      ungetSession(selfRegSession);
    }

    try {
      log.info("Looping all the post processors");
      for (UserPostProcessor userPostProcessor : postProcessorTracker.getProcessors()) {
        log.info("Processor: " + userPostProcessor);
        userPostProcessor.process(request, changes);
      }
      log.info("Finished Looping all the post processors");
    } catch (Exception e) {
      log.warn(e.getMessage(), e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }


  protected void bindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.bindUserPostProcessor(serviceReference);

  }

  protected void unbindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.unbindUserPostProcessor(serviceReference);
  }
  
  


}
