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
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
 */
@Component(immediate = true, metatype = true)
@Service
public final class TrustedAuthenticationServlet extends HttpServlet {
  /**
   * 
   */
  private static final long serialVersionUID = 4265672306115024805L;

  private static final Logger LOG = LoggerFactory
      .getLogger(TrustedAuthenticationServlet.class);
  
  private static final String PARAM_DESTINATION = "d";

  @Property(value = "Trusted Authentication Servlet", propertyPrivate = true)
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation", propertyPrivate = true)
  static final String VENDOR_PROPERTY = "service.vendor";

  /** Property for the path to which to register this servlet. */
  @Property(value = "/trusted")
  static final String REGISTRATION_PATH = "sakai.auth.trusted.path.registration";

  /**
   * Property for the default destination to go to if no destination is specified.
   */
  @Property(value = "/dev")
  static final String DEFAULT_DESTINATION = "sakai.auth.trusted.destination.default";

  /** Reference to web container to register this servlet. */
  @Reference
  protected transient WebContainer webContainer;

  @Reference
  protected transient TrustedTokenService trustedTokenService;

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
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    
    if (trustedTokenService instanceof TrustedTokenServiceImpl) {
      ((TrustedTokenServiceImpl) trustedTokenService).injectToken(req, resp);

      String destination = req.getParameter(PARAM_DESTINATION);

      if (destination == null) {
        destination = defaultDestination;
      }
      // ensure that the redirect is safe and not susceptible to
      resp.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
    }
  }


 
}
