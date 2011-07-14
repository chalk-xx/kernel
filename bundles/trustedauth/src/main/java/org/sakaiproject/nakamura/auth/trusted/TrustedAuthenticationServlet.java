/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.auth.trusted;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenTypes;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet for storing authentication credentials from requests using an external trusted
 * mechanism such as CAS.
 * </p>
 * <p>
 * This servlet does not perform the authentication itself but looks for information in
 * the request from the authentication authority. This information is then stored in the
 * session for use by the authentication handler on subsequent calls.
 * </p>
 * <p>
 * This servlet is mounted outside sling. In essence we Trust the external authentication and 
 * simply store the trusted user in a a trusted token in the form of a cookie.
 * </p>
 */
@ServiceDocumentation(name = "Trusted Authentication Servlet documentation", okForVersion = "0.11",
  shortDescription = "Allows authentication by a trusted external source.",
  description = "Allows authentication by a trusted external source. This servlet does not perform the authentication itself but looks for information in\n" +
    " the request from the authentication authority. This information is then stored in the\n" +
    " session for use by the authentication handler on subsequent calls." +
    " This servlet is mounted outside sling. In essence we Trust the external authentication and \n" +
    " simply store the trusted user in a trusted token in the form of a cookie.",
  bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/trustedauth"),
  methods = {
    @ServiceMethod(name = "GET", description = "",
      parameters = {
        @ServiceParameter(name = "d", description = "The destination path to be redirected to after saving the authentication token.")
      },
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
@Component(immediate = true, metatype = true)
@Service
public final class TrustedAuthenticationServlet extends HttpServlet implements HttpContext {
  /**
   * 
   */
  private static final long serialVersionUID = 4265672306115024805L;

  
  private static final String PARAM_DESTINATION = "d";

  @Property(value = "Trusted Authentication Servlet", propertyPrivate = true)
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation", propertyPrivate = true)
  static final String VENDOR_PROPERTY = "service.vendor";

  /** Property for the path to which to register this servlet. */
  @Property(value = "/system/trustedauth")
  static final String REGISTRATION_PATH = "sakai.auth.trusted.path.registration";

  /**
   * Property for the default destination to go to if no destination is specified.
   */
  @Property(value = "/dev")
  static final String DEFAULT_DESTINATION = "sakai.auth.trusted.destination.default";

  private static final String DEFAULT_NO_USER_REDIRECT_FORMAT = "/system/trustedauth-nouser?u={0}";

  @Property(value = DEFAULT_NO_USER_REDIRECT_FORMAT)
  private static final String NO_USER_REDIRECT_LOCATION_FORMAT = "sakai.auth.trusted.nouserlocationformat";

  private static final Logger LOGGER = LoggerFactory.getLogger(TrustedAuthenticationServlet.class);





  @Reference
  protected transient HttpService httpService;

  @Reference
  protected transient TrustedTokenService trustedTokenService;
  
  @Reference
  protected transient Repository repository;

  /** The registration path for this servlet. */
  private String registrationPath;

  /** The default destination to go to if none is specified. */
  private String defaultDestination;


  private String noUserRedirectLocationFormat;

  @SuppressWarnings("rawtypes")
  @Activate
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    noUserRedirectLocationFormat = OsgiUtil.toString(props.get(NO_USER_REDIRECT_LOCATION_FORMAT), DEFAULT_NO_USER_REDIRECT_FORMAT);
    registrationPath = OsgiUtil.toString(props.get(REGISTRATION_PATH), "/system/trustedauth");
    defaultDestination = OsgiUtil.toString(props.get(DEFAULT_DESTINATION), "/dev");
    try {
      httpService.registerServlet(registrationPath, this, null, null);
      LOGGER.info("Registered {} at {} ",this,registrationPath);
    } catch (ServletException e) {
      LOGGER.error(e.getMessage(),e);
    } catch (NamespaceException e) {
      LOGGER.error(e.getMessage(),e);
    }
  }

  protected void deactivate(ComponentContext context) {
    httpService.unregister(registrationPath);
    LOGGER.info("Unregistered {} from {} ",this,registrationPath);
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="BC_VACUOUS_INSTANCEOF",justification="Could be injected from annother bundle")
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    
    if (trustedTokenService instanceof TrustedTokenServiceImpl) {
      final AuthenticatedAction authAction = new AuthenticatedAction();
      ((TrustedTokenServiceImpl) trustedTokenService).injectToken(req, resp, TrustedTokenTypes.AUTHENTICATED_TRUST, new UserValidator(){

        public String validate(String userId) {
          if ( userId != null ) {        
            // we found a user, check if it really exists.
            Session session = null;
            try {
              session = repository.loginAdministrative();
              AuthorizableManager am = session.getAuthorizableManager();
              Authorizable a = am.findAuthorizable(userId);
              if ( a == null ) {
                LOGGER.info("Authenticated User {} does not exist");
                authAction.setAction(AuthenticatedAction.REDIRECT);
                return null;
              }
            } catch (Exception e) {
              LOGGER.warn("Failed to check user ",e);
            } finally {
              if ( session != null ) {
                try {
                  session.logout();
                } catch (ClientPoolException e) {
                  LOGGER.warn("Failed to close admin session ",e);
                }
              }
            }
          }
          return userId;
        }
      });
      String destination = req.getParameter(PARAM_DESTINATION);
      if (destination == null) {
        destination = defaultDestination;
      }
      if ( authAction.isRedirect() ) {
        String redirectLocation = MessageFormat.format(noUserRedirectLocationFormat, URLEncoder.encode(destination, "UTF-8"));
        resp.sendRedirect(redirectLocation);
      } else {
        if (destination == null) {
          destination = defaultDestination;
        }
        // ensure that the redirect is safe and not susceptible to
        resp.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
      }
    } else {
      LOGGER.debug("Trusted Token Service is not the correct implementation and so cant inject tokens. ");
    }
  }

  public String getMimeType(String mimetype) {
    return null;
  }

  public URL getResource(String name) {
    return getClass().getResource(name);
  }

  /**
   * (non-Javadoc) This servlet handles its own security since it is going to trust the
   * external remote user. If we don't do this the Sling handleSecurity takes over and causes problems.
   * 
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    return true;
  }

 
}
