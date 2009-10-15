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
package org.sakaiproject.kernel.auth.trusted;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.kernel.auth.trusted.TrustedAuthenticationHandler.TrustedAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * Servlet for storing authentication credentials from requests using an
 * external trusted mechanism such as CAS.
 * </p>
 * <p>
 * This servlet does not perform the authentication itself but looks for
 * information in the request from the authentication authority. This
 * information is then stored in the session for use by the authentication
 * handler on subsequent calls.
 * </p>
 */
@Component(enabled = false, immediate = true, metatype = true)
@Service
public class TrustedAuthenticationServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(TrustedAuthenticationServlet.class);
  private static final long serialVersionUID = 1L;
  private static final String PARAM_DESTINATION = "d";

  @Property(value = "Trusted Authentication Servlet", propertyPrivate = true)
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation", propertyPrivate = true)
  static final String VENDOR_PROPERTY = "service.vendor";

  /** Property for the path to which to register this servlet. */
  @Property(value = "/trusted")
  static final String REGISTRATION_PATH = "sakai.auth.trusted.path.registration";

  /**
   * Property for the default destination to go to if no destination is
   * specified.
   */
  @Property(value = "/dev")
  static final String DEFAULT_DESTINATION = "sakai.auth.trusted.destination.default";

  /** Reference to web container to register this servlet. */
  @Reference
  private WebContainer webContainer;

  /** The registration path for this servlet. */
  private String registrationPath;

  /** The default destination to go to if none is specified. */
  private String defaultDestination;

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    registrationPath = (String) props.get(REGISTRATION_PATH);
    defaultDestination = (String) props.get(DEFAULT_DESTINATION);

    try {
      webContainer.registerServlet(registrationPath, this, null, null);
    } catch (NamespaceException e) {
      LOG.error(e.getMessage(), e);
      throw new ComponentException(e.getMessage(), e);
    } catch (ServletException e) {
      LOG.error(e.getMessage(), e);
      throw new ComponentException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    HttpSession session = req.getSession(true);

    TrustedAuthentication auth = (TrustedAuthentication) req
        .getAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS);
    // check for authentication on request. if found, store on request
    if (auth != null && auth.isValid()) {
      session.setAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS, auth.getCredentials());
    }
    // if authentication missing or invalid in session, get the information from
    // the request.
    else {
      Credentials cred = getCredentials(req);
      session.setAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS, cred);
    }

    String destination = req.getParameter(PARAM_DESTINATION);
    if (destination == null) {
      destination = defaultDestination;
    }
    resp.sendRedirect(destination);
  }

  /**
   * Get the user ID from the request. Currently checks getRemoteUser() and
   * getUserPrincipal() but may need to change based on how the external
   * authentication passes the user information back. Once the user is
   * determined, {@link Credentials} are constructed with the user and a trusted
   * attribute.
   *
   * @param req
   *          The request to sniff for a user.
   * @return
   */
  private Credentials getCredentials(HttpServletRequest req) {
    String userId = null;
    if (req.getUserPrincipal() != null) {
      userId = req.getUserPrincipal().getName();
    } else if (req.getRemoteUser() != null) {
      userId = req.getRemoteUser();
    }
    SimpleCredentials sc = new SimpleCredentials(userId, null);
    TrustedUser user = new TrustedUser(userId);
    sc.setAttribute(TrustedAuthenticationHandler.class.getName(), user);
    return sc;
  }

  /**
   * "Trusted" inner class for passing the user on to the authentication
   * handler.<br/>
   * <br/>
   * By being a static, inner class with a private constructor, it is harder for
   * an external source to inject into the authentication chain.
   */
  static final class TrustedUser {
    private final String user;

    /**
     * Constructor.
     *
     * @param user
     *          The user to represent.
     */
    private TrustedUser(String user) {
      this.user = user;
    }

    /**
     * Get the user that is being represented.
     *
     * @return The represented user.
     */
    String getUser() {
      return user;
    }
  }
}
