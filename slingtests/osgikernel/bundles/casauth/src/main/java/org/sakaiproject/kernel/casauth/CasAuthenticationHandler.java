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
package org.sakaiproject.kernel.casauth;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.jasig.cas.client.authentication.DefaultGatewayResolverImpl;
import org.jasig.cas.client.authentication.GatewayResolver;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component(immediate = false, label = "%auth.cas.name", description = "%auth.cas.description", enabled = false, metatype = true)
@Service
public final class CasAuthenticationHandler implements AuthenticationHandler {

  @Property(value = "https://localhost:8443")
  protected static final String serverName = "auth.cas.server.name";

  @Property(value = "https://localhost:8443/cas/login")
  protected static final String loginUrl = "auth.cas.server.login";

  /**
   * Path on which this authentication should be activated.
   */
  @Property(value = "/")
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  /** Defines the parameter to look for for the service. */
  private String serviceParameterName = "service";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CasAuthenticationHandler.class);

  /** Represents the constant for where the assertion will be located in memory. */
  public static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";

  /** Defines the parameter to look for for the artifact. */
  private String artifactParameterName = "ticket";

  /** Sets where response.encodeUrl should be called on service urls when constructed. */
  private boolean encodeServiceUrl = true;

  private boolean renew = false;

  private GatewayResolver gatewayStorage = new DefaultGatewayResolverImpl();

  private String casServerUrl = null;

  private String casServerLoginUrl = null;

  public static final String AUTH_TYPE = CasAuthenticationHandler.class.getName();

  public AuthenticationInfo authenticate(HttpServletRequest request,
      HttpServletResponse response) {
    AuthenticationInfo authnInfo = null;
    final HttpSession session = request.getSession(false);
    final Assertion assertion = session != null ? (Assertion) session
        .getAttribute(CONST_CAS_ASSERTION) : null;
    if (assertion != null) {
      authnInfo = new AuthenticationInfo(AUTH_TYPE, new SimpleCredentials(assertion
          .getPrincipal().getName(), new char[0]));
    } else {
      final String serviceUrl = constructServiceUrl(request, response);
      final String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
      final boolean wasGatewayed = this.gatewayStorage.hasGatewayedAlready(request,
          serviceUrl);

      if (CommonUtils.isNotBlank(ticket) || wasGatewayed) {
        authnInfo = getUserFromTicket(ticket, serviceUrl);
      } else {
        LOGGER.debug("no ticket and no assertion found");
      }
    }
    return authnInfo;
  }

  public boolean requestAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    final String serviceUrl = constructServiceUrl(request, response);
    final String modifiedServiceUrl;

    Boolean gateway = Boolean.parseBoolean(request.getParameter("gateway"));
    if (gateway) {
      LOGGER.debug("setting gateway attribute in session");
      modifiedServiceUrl = this.gatewayStorage.storeGatewayInformation(request,
          serviceUrl);
    } else {
      modifiedServiceUrl = serviceUrl;
    }

    final String urlToRedirectTo = CommonUtils.constructRedirectUrl(
        this.casServerLoginUrl, this.serviceParameterName, modifiedServiceUrl,
        this.renew, gateway);

    response.sendRedirect(urlToRedirectTo);
    return true;
  }

  private AuthenticationInfo getUserFromTicket(String ticket, String serviceUrl) {
    AuthenticationInfo authnInfo = null;
    Cas20ServiceTicketValidator sv = new Cas20ServiceTicketValidator(casServerUrl);
    // sv.setAcceptAnyProxy(true);
    try {
      Assertion a = sv.validate(ticket, serviceUrl);
      authnInfo = new AuthenticationInfo(AUTH_TYPE, new SimpleCredentials(a
          .getPrincipal().getName(), new char[0]));
    } catch (TicketValidationException e) {
      LOGGER.error(e.getMessage());
    }
    return authnInfo;
  }

  private String constructServiceUrl(HttpServletRequest request,
      HttpServletResponse response) {
    String serviceUrl = CommonUtils.constructServiceUrl(request, response, null, request
        .getServerName(), this.artifactParameterName, this.encodeServiceUrl);
    return serviceUrl;
  }

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext context) {
    Dictionary properties = context.getProperties();
    casServerUrl = (String) properties.get(serverName);
    casServerLoginUrl = (String) properties.get(loginUrl);
  }
}
