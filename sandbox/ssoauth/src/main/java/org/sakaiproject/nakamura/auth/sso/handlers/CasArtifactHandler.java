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
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true)
@Service
@Properties(value = {
    @Property(name = ArtifactHandler.LOGIN_URL, value = CasArtifactHandler.LOGIN_URL_DEFAULT),
    @Property(name = ArtifactHandler.LOGOUT_URL, value = CasArtifactHandler.LOGOUT_URL_DEFAULT),
    @Property(name = ArtifactHandler.SERVER_URL, value = CasArtifactHandler.SERVER_URL_DEFAULT),
    @Property(name = CasArtifactHandler.RENEW, boolValue = CasArtifactHandler.RENEW_DEFAULT),
    @Property(name = CasArtifactHandler.GATEWAY, boolValue = CasArtifactHandler.GATEWAY_DEFAULT)
})
public class CasArtifactHandler implements ArtifactHandler {
  private static final Logger logger = LoggerFactory.getLogger(CasArtifactHandler.class);

  //---------- common fields ----------
  protected static final String LOGIN_URL_DEFAULT = "https://localhost:8443/cas/login";
  protected static final String LOGOUT_URL_DEFAULT = "https://localhost:8443/cas/logout";
  protected static final String SERVER_URL_DEFAULT = "https://localhost:8443/cas";
  protected static final boolean RENEW_DEFAULT = false;
  protected static final boolean GATEWAY_DEFAULT = false;
  protected static final String VALIDATE_URL_TMPL = "%s/serviceValidate?%s=%s";
  protected static final String LOGIN_URL_TMPL = "%s?service=%s";

  private String loginUrl = null;
  private String logoutUrl = null;
  private String serverUrl = null;

  // ---------- CAS specific fields ----------
  static final String RENEW = "auth.sso.prop.renew";
  private boolean renew;

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
    loginUrl = OsgiUtil.toString(props.get(LOGIN_URL), LOGIN_URL_DEFAULT);
    logoutUrl = OsgiUtil.toString(props.get(LOGOUT_URL), LOGOUT_URL_DEFAULT);
    serverUrl = OsgiUtil.toString(props.get(SERVER_URL), SERVER_URL_DEFAULT);

    renew = OsgiUtil.toBoolean(props.get(RENEW), RENEW_DEFAULT);
    gateway = OsgiUtil.toBoolean(props.get(GATEWAY), GATEWAY_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getArtifactName()
   */
  public String getArtifactName() {
    return "ticket";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#extractArtifact(javax.servlet.http.HttpServletRequest)
   */
  public String extractArtifact(HttpServletRequest request) {
    return request.getParameter(getArtifactName());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getUsername(javax.servlet.http.HttpServletRequest)
   */
  public String extractCredentials(String artifact, String responseBody, HttpServletRequest request) {
    String username = null;
    String failureCode = null;
    String failureMessage = null;
    try {
      XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
      xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
      xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
      xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
      XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(new StringReader(
          responseBody));

      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();

        // process the event if we're starting an element
        if (event.isStartElement()) {
          StartElement startEl = event.asStartElement();
          QName startElName = startEl.getName();
          String startElLocalName = startElName.getLocalPart();

          /*
           * Example of failure XML
          <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
            <cas:authenticationFailure code='INVALID_REQUEST'>
              &#039;service&#039; and &#039;ticket&#039; parameters are both required
            </cas:authenticationFailure>
          </cas:serviceResponse>
          */
          if ("authenticationFailure".equalsIgnoreCase(startElLocalName)) {
            // get code of the failure
            Attribute code = startEl.getAttributeByName(QName.valueOf("code"));
            failureCode = code.getValue();

            // get the message of the failure
            event = eventReader.nextEvent();
            assert event.isCharacters();
            Characters chars = event.asCharacters();
            failureMessage = chars.getData();
            break;
          }

          /*
           * Example of success XML
          <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
            <cas:authenticationSuccess>
              <cas:user>NetID</cas:user>
            </cas:authenticationSuccess>
          </cas:serviceResponse>
          */
          if ("authenticationSuccess".equalsIgnoreCase(startElLocalName)) {
            // skip the user tag start
            event = eventReader.nextEvent();
            // move on to the body of the tag
            event = eventReader.nextEvent();
            assert event.isCharacters();
            Characters chars = event.asCharacters();
            username = chars.getData();
            break;
          }
        }
      }
    } catch (XMLStreamException e) {
      logger.error(e.getMessage(), e);
    }

    if (failureCode != null || failureMessage != null) {
      logger.error("Error parsing response from server [code=" + failureCode
          + ", message=" + failureMessage);
    }
    return username;
//      // TODO parse response from server to get credentials
//      Assertion a = sv.validate(artifact, serverUrl);
//
//      request.getSession().setAttribute(CONST_CAS_ASSERTION, a);
//      authnInfo = createAuthnInfo(a);
//    } catch (TicketValidationException e) {
//      LOGGER.error(e.getMessage());
//    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getValidateUrl(java.lang.String,
   *      javax.servlet.http.HttpServletRequest)
   */
  public String getValidateUrl(String artifact, HttpServletRequest request) {
    String url = String.format(VALIDATE_URL_TMPL, serverUrl, getArtifactName(), artifact);
    return url;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#constructRedirectUrl()
   */
  public String getLoginUrl(String serviceUrl, HttpServletRequest request) {
    String urlToRedirectTo = String.format(LOGIN_URL_TMPL, loginUrl, serviceUrl);

    String renewParam = request.getParameter("renew");
    boolean renew = this.renew;
    if (renewParam != null) {
      renew = Boolean.parseBoolean(renewParam);
    }

    String gatewayParam = request.getParameter("gateway");
    boolean gateway = this.gateway;
    if (gatewayParam != null) {
      gateway = Boolean.parseBoolean(gatewayParam);
    }

    urlToRedirectTo += (renew ? "&renew=true" : "") + (gateway ? "&gateway=true" : "");
    return urlToRedirectTo;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getLogoutUrl(javax.servlet.http.HttpServletRequest)
   */
  public String getLogoutUrl(HttpServletRequest request) {
    return logoutUrl;
  }
}
