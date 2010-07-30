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

import static org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler.HANDLER_NAME;
import static org.sakaiproject.nakamura.auth.sso.handlers.OpenSsoArtifactHandler.HANDLER_NAME_DEFAULT;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
import org.sakaiproject.nakamura.auth.sso.SsoAuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Artifact handler specifically designed to handle the interactions with OpenSSO.
 */
@Component(configurationFactory = true, metatype = true)
@Service
@Property(name = HANDLER_NAME, value = HANDLER_NAME_DEFAULT)
public class OpenSsoArtifactHandler implements ArtifactHandler {

  protected static final String HANDLER_NAME_DEFAULT = "openSso";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OpenSsoArtifactHandler.class);

  /** Defines the parameter to look for for the service. */
  public static final String COOKIE_NAME_DEFAULT = "iPlanetDirectoryPro";

  public static final String SERVICE_PARAMETER_NAME_DEFAULT = "goto";

  /** Defines the parameter to look for for the artifact. */
  public static final String ARTIFACT_PARAMETER_NAME_DEFAULT = "tokenid";

  public static final String SUCCESSFUL_BODY_DEFAULT = "boolean=true\n";

  private static final String USRDETAILS_ATTR_NAME_STUB = "userdetails.attribute.name=";
  private static final String USRDETAILS_ATTR_VAL_STUB = "userdetails.attribute.value=";

  public static final String ATTRIBUTES_NAMES_DEFAULT = "uid";
  @Property(value = ATTRIBUTES_NAMES_DEFAULT)
  static final String ATTRIBUTES_NAMES = "auth.sso.attribute";
  private String attributesNames;

  @Reference
  private SsoAuthenticationHandler authHandler;

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  protected void modified(Map<?, ?> props) {
    init(props);
  }

  protected void init(Map<?, ?> props) {
    attributesNames = OsgiUtil.toString(props.get(ATTRIBUTES_NAMES),
        ATTRIBUTES_NAMES_DEFAULT);
  }

  public String getArtifactName() {
    return ARTIFACT_PARAMETER_NAME_DEFAULT;
  }

  public boolean canHandle(HttpServletRequest request) {
    return getArtifact(request) != null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getUsername(javax.servlet.http.HttpServletRequest)
   */
  public String getUsername(HttpServletRequest request) {
    String username = null;
    String serverUrl = authHandler.getServerUrl();
    String artifact = getArtifact(request);
    try {
      // validate URL
      GetMethod get = new GetMethod(serverUrl + "identity/isTokenValid?"
          + ARTIFACT_PARAMETER_NAME_DEFAULT + "=" + artifact);
      HttpClient httpClient = new HttpClient();
      int returnCode = httpClient.executeMethod(get);
      String body = get.getResponseBodyAsString();

      if (returnCode >= 200 && returnCode < 300 && SUCCESSFUL_BODY_DEFAULT.equals(body)) {
        get = new GetMethod(serverUrl + "identity/attributes?attributes_names="
            + attributesNames + "&subjectid=" + artifact);
        returnCode = httpClient.executeMethod(get);
        body = get.getResponseBodyAsString();

        if (returnCode >= 200 && returnCode < 300) {
          BufferedReader br = new BufferedReader(new StringReader(body));
          String attrLine = USRDETAILS_ATTR_NAME_STUB + attributesNames;
          String line = null;
          boolean getNextValue = false;
          while ((line = br.readLine()) != null) {
            if (getNextValue && line.startsWith(USRDETAILS_ATTR_VAL_STUB)) {
              username = line.substring(USRDETAILS_ATTR_VAL_STUB.length());
            } else if (attrLine.equals(line)) {
              getNextValue = true;
            }
          }
        }
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
    return username;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#constructRedirectUrl(java.util.Map)
   */
  public String constructRedirectUrl(Map<String, Object> options) {
    // TODO Auto-generated method stub
    return null;
  }

  private String getArtifact(HttpServletRequest request) {
    String artifact = null;
    Cookie[] cookies = request.getCookies();
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME_DEFAULT.equals(cookie.getName())) {
        artifact = cookie.getValue();
        break;
      }
    }
    return artifact;
  }
}
