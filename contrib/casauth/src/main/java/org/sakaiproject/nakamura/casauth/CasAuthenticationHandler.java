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
package org.sakaiproject.nakamura.casauth;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.auth.Authenticator;
import org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler;
import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.commons.auth.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.apache.sling.servlets.post.Modification;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.authentication.DefaultGatewayResolverImpl;
import org.jasig.cas.client.authentication.GatewayResolver;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.sakaiproject.nakamura.api.casauth.CasAuthConstants;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This class integrates CAS SSO with the Sling authentication framework.
 * Most of its logic is copied from org.jasig.cas.client servlet filters.
 * The integration is needed only due to limitations on servlet filter
 * support in the OSGi / Sling environment.
 */
@Component(immediate=true, label="%auth.cas.name", description="%auth.cas.description", enabled=true, metatype=true)
@Properties(value={
    @Property(name=AuthenticationHandler.PATH_PROPERTY, value="/"),
    @Property(name=org.osgi.framework.Constants.SERVICE_RANKING, value="5"),
    @Property(name=AuthenticationHandler.TYPE_PROPERTY, value=CasAuthConstants.CAS_AUTH_TYPE, propertyPrivate=true)
})
@Services({
  @Service(value=CasAuthenticationHandler.class),
  @Service(value=AuthenticationHandler.class),
  @Service(value=LoginModulePlugin.class),
  @Service(value=AuthenticationFeedbackHandler.class)
})
public final class CasAuthenticationHandler implements AuthenticationHandler, LoginModulePlugin, AuthenticationFeedbackHandler {

  @Property(value="https://localhost:8443")
  protected static final String CAS_SERVER_NAME = "auth.cas.server.name";
  private String casServerUrl = null;

  @Property(value="https://localhost:8443/cas/login")
  protected static final String CAS_LOGIN_URL = "auth.cas.server.login";
  private String casServerLoginUrl = null;

  @Property(value="")
  protected static final String CAS_LOGOUT_URL = "auth.cas.server.logout";
  private String casServerLogoutUrl = null;

  @Property(boolValue=false)
  protected static final String CAS_AUTOCREATE_USER = "auth.cas.autocreate";
  private boolean autoCreateUser;

  /** Defines the parameter to look for for the service. */
  private static final String SERVICE_PARAMETER_NAME = "service";

  /** Defines the parameter to look for for the artifact. */
  private static final String ARTIFACT_PARAMETER_NAME = "ticket";

  /** Represents the constant for where the assertion will be located in memory. */
  static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CasAuthenticationHandler.class);

  // TODO Only needed for the automatic user creation.
  @Reference
  protected transient SlingRepository repository;
  @Reference
  protected transient AuthorizablePostProcessService authorizablePostProcessService;

  /**
   * Define the set of authentication-related query parameters which should
   * be removed from the "service" URL sent to the CAS server.
   */
  Set<String> filteredQueryStrings = new HashSet<String>(Arrays.asList(REQUEST_LOGIN_PARAMETER, ARTIFACT_PARAMETER_NAME));

  private boolean renew = false;

  private GatewayResolver gatewayStorage = new DefaultGatewayResolverImpl();

  //----------- AuthenticationHandler interface ----------------------------

  public void dropCredentials(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    final HttpSession session = request.getSession(false);
    if (session != null) {
      final Assertion assertion = (Assertion) session.getAttribute(CONST_CAS_ASSERTION);
      if (assertion != null) {
        LOGGER.debug("CAS Authentication attribute will be removed");
        session.removeAttribute(CONST_CAS_ASSERTION);

        // If we are also supposed to redirect to the CAS server to log out
        // of SSO, set up the redirect now.
        if (casServerLogoutUrl != null && casServerLogoutUrl.length() > 0) {
          String target = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
          if (target == null || target.length() == 0) {
            target = request.getParameter(Authenticator.LOGIN_RESOURCE);
          }
          if (target != null && target.length() > 0 && !("/".equals(target))) {
            LOGGER.info("CAS logout about to override requested redirect to {} and instead redirect to {}", target, casServerLogoutUrl);
          } else {
            LOGGER.debug("CAS logout will request redirect to {}", casServerLogoutUrl);
          }
          request.setAttribute(Authenticator.LOGIN_RESOURCE, casServerLogoutUrl);
        }
      }
    }
  }

  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.debug("extractCredentials called");
    AuthenticationInfo authnInfo = null;
    final HttpSession session = request.getSession(false);
    final Assertion assertion = session != null ? (Assertion) session
        .getAttribute(CONST_CAS_ASSERTION) : null;
    if (assertion != null) {
      LOGGER.debug("assertion found");
      authnInfo = createAuthnInfo(assertion);
    } else {
      final String serviceUrl = constructServiceUrl(request, response);
      final String ticket = CommonUtils.safeGetParameter(request, ARTIFACT_PARAMETER_NAME);
      final boolean wasGatewayed = this.gatewayStorage.hasGatewayedAlready(request,
          serviceUrl);

      if (CommonUtils.isNotBlank(ticket) || wasGatewayed) {
        LOGGER.debug("found ticket: \"{}\" or was gatewayed", ticket);
        authnInfo = getUserFromTicket(ticket, serviceUrl, request);
      } else {
        LOGGER.debug("no ticket and no assertion found");
      }
    }
    return authnInfo;
  }

  public boolean requestCredentials(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    LOGGER.debug("requestCredentials called");
    final String serviceUrl = constructServiceUrl(request, response);
    Boolean gateway = Boolean.parseBoolean(request.getParameter("gateway"));
    final String modifiedServiceUrl;
    if (gateway) {
      LOGGER.debug("Setting gateway attribute in session");
      modifiedServiceUrl = this.gatewayStorage.storeGatewayInformation(request,
          serviceUrl);
    } else {
      modifiedServiceUrl = serviceUrl;
    }
    LOGGER.debug("Service URL = \"{}\"", modifiedServiceUrl);
    final String urlToRedirectTo = CommonUtils.constructRedirectUrl(
        this.casServerLoginUrl, SERVICE_PARAMETER_NAME, modifiedServiceUrl,
        this.renew, gateway);
    LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
    response.sendRedirect(urlToRedirectTo);
    return true;
  }

  //----------- LoginModulePlugin interface ----------------------------

  @SuppressWarnings("unchecked")
  public void addPrincipals(Set principals) {
  }

  public boolean canHandle(Credentials credentials) {
    return (getCasPrincipal(credentials) != null);
  }

  @SuppressWarnings("unchecked")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
  }

  public AuthenticationPlugin getAuthentication(Principal principal, Credentials credentials)
      throws RepositoryException {
    AuthenticationPlugin plugin = null;
    if (canHandle(credentials)) {
      plugin = new CasAuthenticationPlugin(this);
    }
    return plugin;
  }

  public Principal getPrincipal(Credentials credentials) {
    return getCasPrincipal(credentials);
  }

  public int impersonate(Principal principal, Credentials credentials)
      throws RepositoryException, FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

  //----------- AuthenticationFeedbackHandler interface ----------------------------

  /**
   * {@inheritDoc}
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public void authenticationFailed(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationFailed called");
    final HttpSession session = request.getSession(false);
    if (session != null) {
      final Assertion assertion = (Assertion) session.getAttribute(CONST_CAS_ASSERTION);
      if (assertion != null) {
        LOGGER.warn("CAS assertion is set", new Exception());
      }
    }
  }

  /**
   * If a redirect is configured, this method will take care of the redirect.
   * <p>
   * If user auto-creation is configured, this method will check for an existing Authorizable
   * that matches the principal. If not found, it creates a new Jackrabbit user
   * with all properties blank except for the ID and a randomly generated password.
   * WARNING: Currently this will not perform the extra work done by the Nakamura
   * CreateUserServlet, and the resulting user will not be associated with a
   * valid profile.
   * <p>
   * TODO This really needs to be dropped to allow for user pull, person directory
   * integrations, etc. See SLING-1563 for the related issue of user population via OpenID.
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationSucceeded called");

    // If the plug-in is intended to verify the existence of a matching Authorizable,
    // check that now.
    if (this.autoCreateUser) {
      boolean isUserValid = findOrCreateUser(authInfo);
      if (!isUserValid) {
        LOGGER.warn("CAS authentication succeeded but corresponding user not found or created");
        try {
          dropCredentials(request, response);
        } catch (IOException e) {
          LOGGER.error("Failed to drop credentials after CAS authentication by invalid user", e);
        }
        return true;
      }
    }

    // Check for the default post-authentication redirect.
    return DefaultAuthenticationFeedbackHandler.handleRedirect(request, response);
  }

  //----------- OSGi integration ----------------------------

  @Activate
  protected void activate(Map<?, ?> properties) {
    init(properties);
  }

  @Modified
  protected void modified(Map<?, ?> properties) {
    init(properties);
  }

  //----------- Package ----------------------------

  /**
   * In imitation of sling.formauth, use the "resource" parameter to determine
   * where the browser should go after successful authentication.
   * <p>
   * TODO The "sling.auth.redirect" parameter seems to make more sense, but it
   * currently causes a redirect to happen in SlingAuthenticator's
   * getAnonymousResolver method before handlers get a chance to requestCredentials.
   *
   * @param request
   * @return the path to which the browser should be directed after successful
   * authentication, or null if no destination was specified
   */
  String getReturnPath(HttpServletRequest request) {
    final String returnPath;
    Object resObj = request.getAttribute(Authenticator.LOGIN_RESOURCE);
    if ((resObj instanceof String) && ((String) resObj).length() > 0) {
      returnPath = (String) resObj;
    } else {
      String resource = request.getParameter(Authenticator.LOGIN_RESOURCE);
      if ((resource != null) && (resource.length() > 0)) {
        returnPath = resource;
      } else {
        returnPath = null;
      }
    }
    return returnPath;
  }

  //----------- Internal ----------------------------

  private void init(Map<?, ?> properties) {
    casServerUrl = OsgiUtil.toString(properties.get(CAS_SERVER_NAME), "");
    casServerLoginUrl = OsgiUtil.toString(properties.get(CAS_LOGIN_URL), "");
    casServerLogoutUrl = OsgiUtil.toString(properties.get(CAS_LOGOUT_URL), "");
    autoCreateUser = OsgiUtil.toBoolean(properties.get(CAS_AUTOCREATE_USER), false);
  }

  private static class CasPrincipal implements AttributePrincipal {
    private static final long serialVersionUID = -6232157660434175773L;
    private AttributePrincipal principal;
    public CasPrincipal(AttributePrincipal principal) {
      this.principal = principal;
    }
    public boolean equals(Object another) {
      return principal.equals(another);
    }
    @SuppressWarnings("unchecked")
    public Map getAttributes() {
      return principal.getAttributes();
    }
    public String getName() {
      return principal.getName();
    }
    public String getProxyTicketFor(String service) {
      return principal.getProxyTicketFor(service);
    }
    public int hashCode() {
      return principal.hashCode();
    }
    public String toString() {
      return principal.toString();
    }
  }

  private AuthenticationInfo createAuthnInfo(final Assertion assertion) {
    AuthenticationInfo authnInfo;
    AttributePrincipal principal = assertion.getPrincipal();
    authnInfo = new AuthenticationInfo(CasAuthConstants.CAS_AUTH_TYPE);
    SimpleCredentials credentials = new SimpleCredentials(principal.getName(), new char[] {});
    credentials.setAttribute(CasPrincipal.class.getName(), new CasPrincipal(principal));
    authnInfo.put(AuthenticationInfo.CREDENTIALS, credentials);
    return authnInfo;
  }

  private AuthenticationInfo getUserFromTicket(String ticket, String serviceUrl,
      HttpServletRequest request) {
    AuthenticationInfo authnInfo = null;
    Cas20ServiceTicketValidator sv = new Cas20ServiceTicketValidator(casServerUrl);
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
   * @param request
   * @param response
   * @return the URL to which the CAS server should redirect after successful
   * authentication. By default, this is the same URL from which authentication
   * was initiated (minus authentication-related query strings like "ticket").
   * A request attribute or parameter can be used to specify a different
   * return path.
   */
  private String constructServiceUrl(HttpServletRequest request,
      HttpServletResponse response) {
    StringBuilder serviceUrl = getServerName(request);
    String requestedReturnPath = getReturnPath(request);
    if (requestedReturnPath != null && requestedReturnPath.length() > 0) {
      serviceUrl.append(requestedReturnPath);
    } else {
      serviceUrl.append(request.getRequestURI());
      String queryString = request.getQueryString();
      if (queryString != null) {
        boolean noQueryString = true;
        String[] parameters = queryString.split("&");
        for (String parameter : parameters) {
          String[] keyAndValue = parameter.split("=", 2);
          String key = keyAndValue[0];
          if (!filteredQueryStrings.contains(key)) {
            if (noQueryString) {
              serviceUrl.append("?");
              noQueryString = false;
            } else {
              serviceUrl.append("&");
            }
            serviceUrl.append(parameter);
          }
        }
      }
    }
    return serviceUrl.toString();
  }

  private CasPrincipal getCasPrincipal(Credentials credentials) {
    CasPrincipal casPrincipal = null;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
      Object attribute = simpleCredentials.getAttribute(CasPrincipal.class.getName());
      if (attribute instanceof CasPrincipal) {
        casPrincipal = (CasPrincipal) attribute;
      }
    }
    return casPrincipal;
  }

  private boolean findOrCreateUser(AuthenticationInfo authInfo) {
    boolean isUserValid = false;
    final CasPrincipal casPrincipal = getCasPrincipal((Credentials)authInfo.get(AuthenticationInfo.CREDENTIALS));
    if (casPrincipal != null) {
      final String principalName = casPrincipal.getName();
      // Check for a matching Authorizable. If one isn't found, create
      // a new user.
      Session session = null;
      try {
        session = repository.loginAdministrative(null); // usage checked and ok KERN-577
        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(principalName);
        if (authorizable == null) {
          createUser(principalName, session);
        }
        isUserValid = true;
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    }
    return isUserValid;
  }

  /**
   * TODO This logic should probably be supplied by a shared service rather
   * than copied and pasted across components.
   */
  private User createUser(String principalName, Session session) throws Exception {
    LOGGER.info("Creating user {}", principalName);
    UserManager userManager = AccessControlUtil.getUserManager(session);
    User user = userManager.createUser(principalName, RandomStringUtils.random(32));
    ItemBasedPrincipal principal = (ItemBasedPrincipal) user.getPrincipal();
    String path = principal.getPath();
    path = path.substring(UserConstants.USER_REPO_LOCATION.length());
    ValueFactory valueFactory = session.getValueFactory();
    user.setProperty("path", valueFactory.createValue(path));
    if (authorizablePostProcessService != null) {
      authorizablePostProcessService.process(user, session, Modification.onCreated(user.getID()));
    }
    return user;
  }

  private StringBuilder getServerName(HttpServletRequest request) {
    StringBuilder serverName = new StringBuilder();
    String scheme = request.getScheme();
    int port = request.getServerPort();
    serverName.append(scheme).append("://").append(request.getServerName());
    if ((port > 0) && (!"http".equals(scheme) || port != 80)
        && (!"https".equals(scheme) || port != 443)) {
      serverName.append(':').append(port);
    }
    return serverName;
  }
}
