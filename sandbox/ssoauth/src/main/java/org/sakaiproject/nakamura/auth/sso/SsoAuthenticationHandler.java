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
package org.sakaiproject.nakamura.auth.sso;

import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_MULTIPLE;

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
import org.apache.sling.api.auth.Authenticator;
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
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
import org.sakaiproject.nakamura.api.auth.sso.SsoAuthConstants;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
@Component(immediate=true, label="%auth.sso.name", description="%auth.sso.description", enabled=true, metatype=true)
@Properties(value={
    @Property(name=AuthenticationHandler.PATH_PROPERTY, value="/"),
    @Property(name=org.osgi.framework.Constants.SERVICE_RANKING, value="5"),
    @Property(name=AuthenticationHandler.TYPE_PROPERTY, value=SsoAuthConstants.SSO_AUTH_TYPE, propertyPrivate=true)
})
@Services({
  @Service(value=SsoAuthenticationHandler.class),
  @Service(value=AuthenticationHandler.class),
  @Service(value=LoginModulePlugin.class),
  @Service(value=AuthenticationFeedbackHandler.class)
})
public final class SsoAuthenticationHandler implements AuthenticationHandler, LoginModulePlugin, AuthenticationFeedbackHandler {

  @Property(value="https://localhost:8443")
  protected static final String SSO_SERVER_NAME = "auth.sso.server.name";
  private String ssoServerUrl = null;

  public static final String SSO_LOGIN_URL_DEFAULT = "https://localhost:8443/cas/login";
  @Property(value = SSO_LOGIN_URL_DEFAULT)
  protected static final String SSO_LOGIN_URL = "auth.sso.url.login";
  private String ssoServerLoginUrl = null;

  public static final String SSO_LOGOUT_URL_DEFAULT = "https://localhost:8443/cas/login";
  @Property(value = SSO_LOGOUT_URL_DEFAULT)
  protected static final String SSO_LOGOUT_URL = "auth.sso.url.logout";
  private String ssoServerLogoutUrl = null;

  public static final String SSO_VALIDATE_URL_DEFAULT = "https://localhost:8443/cas/validate";
  @Property(value = SSO_VALIDATE_URL_DEFAULT)
  protected static final String SSO_VALIDATE_URL = "auth.sso.url.validate";
  private String ssoServerValidateUrl = null;

  public static final boolean SSO_AUTOCREATE_USER_DEFAULT = false;
  @Property(boolValue = SSO_AUTOCREATE_USER_DEFAULT)
  protected static final String SSO_AUTOCREATE_USER = "auth.sso.autocreate";
  private boolean autoCreateUser;

  /** Represents the constant for where the assertion will be located in memory. */
  static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SsoAuthenticationHandler.class);

  // TODO Only needed for the automatic user creation.
  @Reference
  protected transient SlingRepository repository;

  @Reference
  protected transient AuthorizablePostProcessService authorizablePostProcessService;

  @Reference(referenceInterface = ArtifactHandler.class, cardinality = MANDATORY_MULTIPLE)
  protected transient HashMap<String, ArtifactHandler> artifactHandlers = new HashMap<String, ArtifactHandler>();

  /**
   * Define the set of authentication-related query parameters which should
   * be removed from the "service" URL sent to the CAS server.
   */
  Set<String> filteredQueryStrings = new HashSet<String>(Arrays.asList(REQUEST_LOGIN_PARAMETER));

  private boolean renew = false;

  private GatewayResolver gatewayStorage = new DefaultGatewayResolverImpl();

  //----------- OSGi integration ----------------------------

  @Activate
  protected void activate(Map<?, ?> properties) {
    init(properties);
  }

  @Modified
  protected void modified(Map<?, ?> properties) {
    init(properties);
  }

  protected void bindArtifactHandlers(ArtifactHandler handler, Map<?, ?> props) {
    String handlerName = OsgiUtil.toString(props.get(ArtifactHandler.HANDLER_NAME), "");
    ArtifactHandler cachedHandler = artifactHandlers.get(handlerName);
    if (cachedHandler != null) {
      throw new ComponentException("'" + ArtifactHandler.HANDLER_NAME + "' is already mapped to " + cachedHandler.getClass().getName());
    } else {
      artifactHandlers.put(handlerName, handler);
      filteredQueryStrings.add(handler.getArtifactName());
    }
  }

  protected void unbindArtifactHandlers(ArtifactHandler handler, Map<?, ?> props) {
    if (artifactHandlers.size() > 0) {
      String handlerName = OsgiUtil.toString(props.get(ArtifactHandler.HANDLER_NAME), "");
      artifactHandlers.remove(handlerName);
      filteredQueryStrings.remove(handler.getArtifactName());
    }
  }

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
        // TODO get this from the
        if (ssoServerLogoutUrl != null && ssoServerLogoutUrl.length() > 0) {
          String target = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
          if (target == null || target.length() == 0) {
            target = request.getParameter(Authenticator.LOGIN_RESOURCE);
          }
          if (target != null && target.length() > 0 && !("/".equals(target))) {
            LOGGER.info("CAS logout about to override requested redirect to {} and instead redirect to {}", target, ssoServerLogoutUrl);
          } else {
            LOGGER.debug("CAS logout will request redirect to {}", ssoServerLogoutUrl);
          }
          request.setAttribute(Authenticator.LOGIN_RESOURCE, ssoServerLogoutUrl);
        }
      }
    }
  }

  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.debug("extractCredentials called");

    // go through artifact list to find one that can handle this request
    // Once that is found, use it here for constructServiceUrl,
    ArtifactHandler handler = null;
    for (Entry<String, ArtifactHandler> handlerEntry : artifactHandlers.entrySet()) {
      ArtifactHandler h = handlerEntry.getValue();
      if (h.canHandle(request)) {
        request.setAttribute(ArtifactHandler.HANDLER_NAME, handlerEntry.getKey());
        handler = h;
        break;
      }
    }

    if (handler != null) {
      String username = handler.getUsername(request);
    }

    AuthenticationInfo authnInfo = null;
    final HttpSession session = request.getSession(false);
    final Assertion assertion = session != null ? (Assertion) session
        .getAttribute(CONST_CAS_ASSERTION) : null;
    if (assertion != null) {
      LOGGER.debug("assertion found");
      authnInfo = createAuthnInfo(assertion);
    } else {
      final String serviceUrl = constructServiceUrl(request);
      final String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
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
    // TODO get handler and have it construct the redirect url
    ArtifactHandler handler = null;
    LOGGER.debug("requestCredentials called");
    final String serviceUrl = constructServiceUrl(request);
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
    String urlToRedirectTo = ssoServerLoginUrl + (ssoServerLoginUrl.indexOf("?") != -1 ? "&" : "?")
    + serviceName + "=" + URLEncoder.encode(serviceUrl, "UTF-8");
    final String urlToRedirectTo = handler.constructRedirectUrl(options)
//    final String urlToRedirectTo = CommonUtils.constructRedirectUrl(
//        this.ssoServerLoginUrl, serviceParameterName, modifiedServiceUrl,
//        this.renew, gateway);
    LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
    response.sendRedirect(urlToRedirectTo);
    return true;
  }

  //----------- LoginModulePlugin interface ----------------------------

  @SuppressWarnings("unchecked")
  public void addPrincipals(Set principals) {
  }

  public boolean canHandle(Credentials credentials) {
    return (getSsoPrincipal(credentials) != null);
  }

  @SuppressWarnings("unchecked")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
  }

  public AuthenticationPlugin getAuthentication(Principal principal, Credentials credentials)
      throws RepositoryException {
    AuthenticationPlugin plugin = null;
    if (canHandle(credentials)) {
      plugin = new SsoAuthenticationPlugin(this);
    }
    return plugin;
  }

  private SsoPrincipal getCasPrincipal(Credentials credentials) {
    SsoPrincipal casPrincipal = null;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
      Object attribute = simpleCredentials.getAttribute(SsoPrincipal.class.getName());
      if (attribute instanceof SsoPrincipal) {
        casPrincipal = (SsoPrincipal) attribute;
      }
    }
    return casPrincipal;
  }

  public Principal getPrincipal(Credentials credentials) {
    return getSsoPrincipal(credentials);
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

  //----------- Public ----------------------------
  public String getServerUrl() {
    return ssoServerUrl;
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

  private void init(Map<?, ?> prop) {
    ssoServerUrl = OsgiUtil.toString(prop.get(SSO_SERVER_NAME), "");
    ssoServerLoginUrl = OsgiUtil.toString(prop.get(SSO_LOGIN_URL), "");
    ssoServerLogoutUrl = OsgiUtil.toString(prop.get(SSO_LOGOUT_URL), "");
    ssoServerValidateUrl = OsgiUtil.toString(prop.get(SSO_LOGOUT_URL), "");
    autoCreateUser = OsgiUtil.toBoolean(prop.get(SSO_AUTOCREATE_USER), false);
  }

  private static final class SsoPrincipal implements Principal {
    private static final long serialVersionUID = -6232157660434175773L;
    private String principalName;

    public SsoPrincipal(String principalName) {
      this.principalName = principalName;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.security.Principal#getName()
     */
    public String getName() {
      return principalName;
    }
  }

  private AuthenticationInfo createAuthnInfo(final String principalName) {
    AuthenticationInfo authnInfo;
    authnInfo = new AuthenticationInfo(SsoAuthConstants.SSO_AUTH_TYPE);
    SimpleCredentials credentials = new SimpleCredentials(principalName, new char[] {});
    credentials.setAttribute(SsoPrincipal.class.getName(), new SsoPrincipal(principalName));
    authnInfo.put(AuthenticationInfo.CREDENTIALS, credentials);
    return authnInfo;
  }

  private AuthenticationInfo getUserFromTicket(String ticket, String serviceUrl,
      HttpServletRequest request) {
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
   * @param request
   * @return the URL to which the CAS server should redirect after successful
   * authentication. By default, this is the same URL from which authentication
   * was initiated (minus authentication-related query strings like "ticket").
   * A request attribute or parameter can be used to specify a different
   * return path.
   */
  private String constructServiceUrl(HttpServletRequest request) {
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

  private SsoPrincipal getSsoPrincipal(Credentials credentials) {
    SsoPrincipal ssoPrincipal = null;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
      Object attribute = simpleCredentials.getAttribute(SsoPrincipal.class.getName());
      if (attribute instanceof SsoPrincipal) {
        ssoPrincipal = (SsoPrincipal) attribute;
      }
    }
    return ssoPrincipal;
  }

  private boolean findOrCreateUser(AuthenticationInfo authInfo) {
    boolean isUserValid = false;
    final SsoPrincipal casPrincipal = getSsoPrincipal((Credentials)authInfo.get(AuthenticationInfo.CREDENTIALS));
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
