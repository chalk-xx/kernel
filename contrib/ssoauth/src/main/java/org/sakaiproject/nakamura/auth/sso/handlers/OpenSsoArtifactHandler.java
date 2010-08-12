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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Artifact handler specifically designed to handle the interactions with OpenSSO.
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true)
@Service
@Properties({
  @Property(name = ArtifactHandler.HANDLER_NAME, value = OpenSsoArtifactHandler.DEFAULT_HANDLER_NAME),
  @Property(name = ArtifactHandler.LOGIN_URL, value = OpenSsoArtifactHandler.DEFAULT_LOGIN_URL),
  @Property(name = ArtifactHandler.LOGOUT_URL, value = OpenSsoArtifactHandler.DEFAULT_LOGOUT_URL),
  @Property(name = ArtifactHandler.SERVER_URL, value = OpenSsoArtifactHandler.DEFAULT_SERVER_URL),
  @Property(name = OpenSsoArtifactHandler.ATTRIBUTES_NAMES, value = OpenSsoArtifactHandler.DEFAULT_ATTRIBUTE_NAME)
})
public class OpenSsoArtifactHandler implements ArtifactHandler {

  /** Defines the parameter to look for for the artifact. */
  public static final String DEFAULT_ARTIFACT_NAME = "iPlanetDirectoryPro";
  public static final String DEFAULT_SUCCESSFUL_BODY = "boolean=true\n";
  public static final String DEFAULT_LOGIN_URL = "https://localhost/sso/UI/Login";
  public static final String DEFAULT_LOGOUT_URL = "https://localhost/sso/UI/Logout";
  public static final String DEFAULT_SERVER_URL = "https://localhost/sso";
  public static final String DEFAULT_ATTRIBUTE_NAME = "uid";

  protected static final String DEFAULT_HANDLER_NAME = "openSso";
  protected static final String USRDTLS_ATTR_NAME_STUB = "userdetails.attribute.name=";
  protected static final String USRDTLS_ATTR_VAL_STUB = "userdetails.attribute.value=";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OpenSsoArtifactHandler.class);

  // ---------- generic fields ----------
  private String loginUrl = null;

  private String logoutUrl;

  private String serverUrl;

  static final String ATTRIBUTES_NAMES = "sakai.auth.sso.opensso.user.attribute";
  private String attributeName;

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    init(props);
  }

  protected void init(Map<?, ?> props) {
    loginUrl = OsgiUtil.toString(props.get(LOGIN_URL), DEFAULT_LOGIN_URL);
    logoutUrl = OsgiUtil.toString(props.get(LOGOUT_URL), DEFAULT_LOGOUT_URL);
    serverUrl = OsgiUtil.toString(props.get(SERVER_URL), DEFAULT_SERVER_URL);

    attributeName = OsgiUtil.toString(props.get(ATTRIBUTES_NAMES), DEFAULT_ATTRIBUTE_NAME);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#getArtifactName()
   */
  public String getArtifactName() {
    return DEFAULT_ARTIFACT_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#extractArtifact(javax.servlet.http.HttpServletRequest)
   */
  public String extractArtifact(HttpServletRequest request) {
    String artifact = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (DEFAULT_ARTIFACT_NAME.equals(cookie.getName())) {
          artifact = cookie.getValue();
          break;
        }
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
      if (DEFAULT_SUCCESSFUL_BODY.equals(responseBody)) {
        String url = serverUrl + "/identity/attributes?attributes_names=" + attributeName + "&subjectid=" + artifact;
        GetMethod get = new GetMethod(url);
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
              break;
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
  public String getValidateUrl(String artifact, String service, HttpServletRequest request) {
    String url = serverUrl + "/identity/isTokenValid?tokenid=" + artifact;
    return url;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler#decorateRedirectUrl(java.util.Map)
   */
  public String getLoginUrl(String service, HttpServletRequest request) {
    String url = loginUrl + "?goto=" + service;
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
