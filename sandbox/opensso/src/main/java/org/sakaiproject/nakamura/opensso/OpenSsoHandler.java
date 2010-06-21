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
package org.sakaiproject.nakamura.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Handler class that encapsulates both extracting the userID from the request, and
 * formulating a redirect url to the central service.
 * 
 * 
 * The protocol is documented here
 * http://developers.sun.com/identity/reference/techart/troubleshooting2.html
 * 
 */
public class OpenSsoHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSsoHandler.class);
  private HttpServletRequest request;
  private HttpServletResponse response;

  /**
   * @param request
   * @param response
   */
  public OpenSsoHandler(HttpServletRequest request, HttpServletResponse response) {

    this.request = request;
    this.response = response;

  }

  /**
   * @throws IOException
   * 
   */
  public void sendAuthenticationFailed(String ssoServerUrl, String destination)
      throws IOException {
    StringBuffer location = request.getRequestURL();
    location.append("?sakaiauth:login=2");
    if (destination != null && destination.trim().length() > 0) {
      location.append("&d=").append(URLEncoder.encode(destination, "UTF-8"));
    }
    String returnUrl = URLEncoder.encode(location.toString(), "UTF-8");
    response.sendRedirect(ssoServerUrl + returnUrl);
  }

  /**
   * @return
   */
  public String getUserName() {
    try {
      System.err.println("Trying to get User");
      SSOTokenManager.debug.setDebug("ERROR");
      SSOTokenManager manager = SSOTokenManager.getInstance();
      SSOToken token = manager.createSSOToken(request);
      System.err.println("Got Token ");
      if (manager.isValidToken(token)) {
        System.err.println("Token  Valid ");
        return token.getPrincipal().getName();
      }
      System.err.println("Token Not Valid ");
    } catch (SSOException e) {
      LOGGER.warn(e.getMessage(),e);
    }
    return null;
  }

}
