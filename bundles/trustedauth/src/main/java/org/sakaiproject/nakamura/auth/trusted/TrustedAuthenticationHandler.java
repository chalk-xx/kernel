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
package org.sakaiproject.nakamura.auth.trusted;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authentication handler for trusted authentication sources. These sources will
 * authenticate users externally and eventually pass through this handler to establish a
 * trusted relationship continuing into the container.
 */
@Component(immediate = true)
@Service
public final class TrustedAuthenticationHandler implements AuthenticationHandler {
  /**
   * Authentication type name
   */
  public static final String TRUSTED_AUTH = TrustedAuthenticationHandler.class.getName();

  /**
   * Attribute name for storage of the TrustedAuthentication object in the requests
   */
  static final String RA_AUTHENTICATION_TRUST = "sakai-trusted-authentication-trust";
  /**
   * The attribute name for the AuthenticationInformation in the request
   */
  static final String RA_AUTHENTICATION_INFO = "sakai-trusted-authentication-authinfo";

  /**
   * Path on which this authentication should be activated. Its active on all paths, as
   * the authentication itself is performed by the TrustedAuthenticationServlet that
   * places credentials in the session. Those credentials are then used to authenticate
   * all subsequent requests.
   */
  @Property(value = "/")
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  @Property(value = "Trusted Authentication Handler")
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation")
  static final String VENDOR_PROPERTY = "service.vendor";
  
  @Reference
  protected TrustedTokenService trustedTokenService;


  // -------------------- AuthenticationHandler methods --------------------

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#extractCredentials(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    // check for existing authentication information in the request
    Object auth = request.getAttribute(RA_AUTHENTICATION_TRUST);
    if ( auth instanceof TrustedAuthentication ) {
      TrustedAuthentication trustedAuthentication = (TrustedAuthentication) auth;
      if ( trustedAuthentication.isValid() ) {
        Object authInfo = request.getAttribute(RA_AUTHENTICATION_INFO);
        if ( authInfo instanceof AuthenticationInfo ) {
          AuthenticationInfo authenticationInfo = (AuthenticationInfo) authInfo;
          Credentials credentials = (Credentials)authenticationInfo.get(AuthenticationInfo.CREDENTIALS);
          if ( credentials instanceof SimpleCredentials ) {
            return authenticationInfo;
          }
        }
      }
    } 
    // create a new authentication in the request.
    TrustedAuthentication trustedAuthentication = new TrustedAuthentication(request, response);
    if (trustedAuthentication.isValid()) {
      request.setAttribute(RA_AUTHENTICATION_TRUST, trustedAuthentication);

      // construct the authentication info and store credentials on the request
      AuthenticationInfo authInfo = new AuthenticationInfo(TRUSTED_AUTH);
      authInfo.put(AuthenticationInfo.CREDENTIALS, trustedAuthentication.getCredentials());
      request.setAttribute(RA_AUTHENTICATION_INFO, authInfo);
      return authInfo;
    } else {
      // no valid credentials found in the request.
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#dropCredentials(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if ( trustedTokenService instanceof TrustedTokenServiceImpl ) {
      ((TrustedTokenServiceImpl) trustedTokenService).dropCredentials(request,response);
    }
    request.setAttribute(RA_AUTHENTICATION_INFO, null);
    request.setAttribute(RA_AUTHENTICATION_TRUST, null);

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    // forces credentials to be requested from the client, in the trusted senario this
    // would redirect to the login landing page.
    return false;
  }

  /**
   * Authentication information for storage in session and/or request.<br/>
   * <br/>
   * By being an inner, static class with a private constructor, it is harder for an
   * external source to inject into the authentication chain.
   */
  final class TrustedAuthentication {
    private final Credentials cred;

    private TrustedAuthentication(HttpServletRequest req, HttpServletResponse response) {
      // This is placed here by the TrustedAuthenticationServlet, that will be in the same
      // web container as Sling and so sharing a session.
      if ( trustedTokenService instanceof TrustedTokenServiceImpl ) {
        cred = ((TrustedTokenServiceImpl) trustedTokenService).getCredentials(req, response);
      } else {
        cred = null;
      }

    }

    Credentials getCredentials() {
      return cred;
    }
    
    

    boolean isValid() {
      return cred != null;
    }
  }

}
