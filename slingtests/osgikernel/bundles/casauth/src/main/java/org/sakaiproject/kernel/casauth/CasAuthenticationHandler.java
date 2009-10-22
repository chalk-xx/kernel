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
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(immediate = false, label = "%auth.cas.name", description = "%auth.cas.description")
@Service
public final class CasAuthenticationHandler implements AuthenticationHandler {

  @Property(value = "https://localhost:8443")
  protected String serverName = "auth.cas.server.name";

  @Property(value = "https://localhost:8443/cas/login")
  protected String loginUrl = "auth.cas.server.login";

  private String casServerUrl = null;

  public AuthenticationInfo authenticate(HttpServletRequest request,
      HttpServletResponse response) {
    AuthenticationInfo authnInfo = null;

    return authnInfo;
  }

  public boolean requestAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    // TODO Auto-generated method stub
    return false;
  }

  private boolean validateTicket(String ticket, String url) {
    AttributePrincipal principal = null;
    Cas20ProxyTicketValidator sv = new Cas20ProxyTicketValidator(casServerUrl);
    sv.setAcceptAnyProxy(true);
    try {
      Assertion a = sv.validate(ticket, url);
      principal = a.getPrincipal();
    } catch (TicketValidationException e) {
      e.printStackTrace(); // bad style, but only for demonstration purpose.
    }
    return principal != null;
  }

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext context) {
    Dictionary properties = context.getProperties();
    casServerUrl = (String) properties.get(serverName);
  }
}
