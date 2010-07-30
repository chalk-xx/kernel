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
package org.sakaiproject.nakamura.auth.sso.handlers;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
import org.sakaiproject.nakamura.api.auth.sso.SsoAuthConstants;
import org.sakaiproject.nakamura.auth.sso.SsoAuthenticationHandler;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Component
@Service
public class CasArtifactHandler implements ArtifactHandler {
  @Reference
  private SsoAuthenticationHandler ssoAuthnHandler;

  /** Defines the parameter to look for for the service. */
  public static final String SERVICE_PARAMETER_DEFAULT = "service";
  @Property(value = SERVICE_PARAMETER_DEFAULT)
  static final String SERVICE_PARAMETER = "auth.sso.service.parameter";
  private String serviceParameter;

  /** Defines the parameter to look for for the artifact. */
  public static final String ARTIFACT_NAME_DEFAULT = "ticket";
  @Property(value = ARTIFACT_NAME_DEFAULT)
  static final String ARTIFACT_NAME = "auth.sso.artifact.name";
  private String artifactName;

  public static final String SERVICE_URL_DEFAULT = SsoAuthConstants.SSO_LOGIN_PATH;
  @Property(value = SERVICE_URL_DEFAULT)
  static final String SERVICE_URL = "auth.sso.service.parameter";
  private String serviceUrl;

  protected static final boolean RENEW_DEFAULT = false;
  @Property(boolValue = RENEW_DEFAULT)
  static final String RENEW = "auth.sso.prop.renew";
  private boolean renew;

  protected static final boolean GATEWAY_DEFAULT = false;
  @Property(boolValue = GATEWAY_DEFAULT)
  static final String GATEWAY = "auth.sso.prop.gateway";
  private boolean gateway;

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    init(props);
  }

  protected void init(Map<?, ?> props) {
    serviceUrl = OsgiUtil.toString(props.get(SERVICE_URL), SERVICE_URL_DEFAULT);
    renew = OsgiUtil.toBoolean(props.get(RENEW), RENEW_DEFAULT);
    gateway = OsgiUtil.toBoolean(props.get(GATEWAY), GATEWAY_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#canHandle(javax.servlet.http.HttpServletRequest)
   */
  public boolean canHandle(HttpServletRequest request) {
    // TODO Auto-generated method stub
    return false;
  }

  public String getArtifactName() {
    return ARTIFACT_NAME_DEFAULT;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getUsername(javax.servlet.http.HttpServletRequest)
   */
  public String getUsername(HttpServletRequest request) {
    String artifact = request.getParameter(ARTIFACT_NAME_DEFAULT);
    ArtifactHandler handler;
    handler.getUsername(ticket, request);
    AuthenticationInfo authnInfo = null;
    Cas20ServiceTicketValidator sv = new Cas20ServiceTicketValidator(ssoServerUrl);
    try {
      Assertion a = sv.validate(ticket, serviceUrl);
      request.getSession().setAttribute(CONST_CAS_ASSERTION, a);
      authnInfo = createAuthnInfo(a);
    } catch (TicketValidationException e) {
      LOGGER.error(e.getMessage());
    }
    return authnInfo;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#constructRedirectUrl()
   */
  public String decorateRedirectUrl(HttpServletRequest request) {
    String renewParam = request.getParameter("renew");
    String gatewayParam = request.getParameter("gateway");

    boolean renew = false;
    boolean gateway = false;

    if (renewParam == null) {
      renew = this.renew;
    } else {
      renew = Boolean.parseBoolean(renewParam);
    }

    if (gatewayParam == null) {
      gateway = this.gateway;
    } else {
      gateway = Boolean.parseBoolean(gatewayParam);
    }

    String retval = (renew ? "&renew=true" : "") + (gateway ? "&gateway=true" : "");
    return retval;
  }
}
