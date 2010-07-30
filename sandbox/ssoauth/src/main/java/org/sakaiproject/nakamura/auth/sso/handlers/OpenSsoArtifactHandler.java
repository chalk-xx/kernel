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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
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

  // ---------- generic fields ----------
  public static final String LOGIN_URL_DEFAULT = "https://localhost/sso/UI/Login";
  @Property(value = LOGIN_URL_DEFAULT)
  private String loginUrl = null;

  public static final String LOGOUT_URL_DEFAULT = "https://localhost/sso/UI/Logout";
  @Property(value = LOGOUT_URL_DEFAULT)
  private String logoutUrl = null;

  public static final String SERVER_URL_DEFAULT = "https://localhost/sso";
  @Property(value = SERVER_URL_DEFAULT)
  private String serverUrl = null;

  public static final String ATTRIBUTE_NAME_DEFAULT = "uid";
  @Property(value = ATTRIBUTE_NAME_DEFAULT)
  static final String ATTRIBUTES_NAMES = "auth.sso.attribute";
  private String attributeName;

  /** Defines the parameter to look for for the artifact. */
  public static final String ARTIFACT_NAME_DEFAULT = "iPlanetDirectoryPro";
  public static final String SUCCESSFUL_BODY_DEFAULT = "boolean=true\n";
  private static final String USRDTLS_ATTR_NAME_STUB = "userdetails.attribute.name=";
  private static final String USRDTLS_ATTR_VAL_STUB = "userdetails.attribute.value=";

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

    attributeName = OsgiUtil.toString(props.get(ATTRIBUTES_NAMES), ATTRIBUTE_NAME_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getArtifactName()
   */
  public String getArtifactName() {
    return ARTIFACT_NAME_DEFAULT;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getArtifact(javax.servlet.http.HttpServletRequest)
   */
  public String getArtifact(HttpServletRequest request) {
    String artifact = null;
    Cookie[] cookies = request.getCookies();
    for (Cookie cookie : cookies) {
      if (ARTIFACT_NAME_DEFAULT.equals(cookie.getName())) {
        artifact = cookie.getValue();
        break;
      }
    }
    return artifact;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#extractCredentials(javax.servlet.http.HttpServletRequest)
   */
  public String extractCredentials(String artifact, String responseBody,
      HttpServletRequest request) {
    String username = null;

    try {
      if (SUCCESSFUL_BODY_DEFAULT.equals(responseBody)) {
        GetMethod get = new GetMethod(serverUrl + "identity/attributes?attributes_names="
            + attributeName + "&subjectid=" + artifact);
        HttpClient httpClient = new HttpClient();
        int returnCode = httpClient.executeMethod(get);
        String body = get.getResponseBodyAsString();

        if (returnCode >= 200 && returnCode < 300) {
          BufferedReader br = new BufferedReader(new StringReader(body));
          String attrLine = USRDTLS_ATTR_NAME_STUB + attributeName;
          String line = null;
          boolean getNextValue = false;
          while ((line = br.readLine()) != null) {
            if (getNextValue && line.startsWith(USRDTLS_ATTR_VAL_STUB)) {
              username = line.substring(USRDTLS_ATTR_VAL_STUB.length());
            } else if (attrLine.equals(line)) {
              getNextValue = true;
            }
          }
        }
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return username;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getValidateUrl(java.lang.String,
   *      javax.servlet.http.HttpServletRequest)
   */
  public String getValidateUrl(String artifact, HttpServletRequest request) {
    String url = serverUrl + "/identity/isTokenValid?tokenid=" + artifact;
    return url;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#decorateRedirectUrl(java.util.Map)
   */
  public String getLoginUrl(String serviceUrl, HttpServletRequest request) {
    String url = loginUrl + "?goto=" + serviceUrl;
    return url;
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
