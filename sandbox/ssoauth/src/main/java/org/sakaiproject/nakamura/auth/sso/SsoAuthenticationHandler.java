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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
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
import java.io.UnsupportedEncodingException;
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
 * This class integrates SSO with the Sling authentication framework.
 * The integration is needed only due to limitations on servlet filter
 * support in the OSGi / Sling environment.
 */
@Component(metatype=true)
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
public final class SsoAuthenticationHandler implements AuthenticationHandler,
    LoginModulePlugin, AuthenticationFeedbackHandler {

  public static final boolean SSO_AUTOCREATE_USER_DEFAULT = false;
  @Property(boolValue = SSO_AUTOCREATE_USER_DEFAULT)
  protected static final String SSO_AUTOCREATE_USER = "auth.sso.autocreate";
  private boolean autoCreateUser;

  /** Represents the constant for where the assertion will be located in memory. */
  static final String CONST_SSO_ASSERTION = "_const_sso_assertion_";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SsoAuthenticationHandler.class);

  // needed for the automatic user creation.
  @Reference
  protected SlingRepository repository;

  @Reference
  protected AuthorizablePostProcessService authzPostProcessService;

  @Reference(referenceInterface = ArtifactHandler.class, cardinality = MANDATORY_MULTIPLE)
  protected HashMap<String, ArtifactHandler> artifactHandlers = new HashMap<String, ArtifactHandler>();

  /**
   * Define the set of authentication-related query parameters which should
   * be removed from the "service" URL sent to the SSO server.
   */
  Set<String> filteredQueryStrings = new HashSet<String>(
      Arrays.asList(REQUEST_LOGIN_PARAMETER));

  public SsoAuthenticationHandler() {
  }

  protected SsoAuthenticationHandler(SlingRepository repository,
      AuthorizablePostProcessService authzPostProcessService,
      HashMap<String, ArtifactHandler> artifactHandlers) {
    this.repository = repository;
    this.authzPostProcessService = authzPostProcessService;
    this.artifactHandlers = artifactHandlers;
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

  protected void bindArtifactHandlers(ArtifactHandler handler, Map<?, ?> props) {
    String handlerName = OsgiUtil.toString(props.get(ArtifactHandler.HANDLER_NAME), "");
    ArtifactHandler cachedHandler = artifactHandlers.get(handlerName);
    if (cachedHandler != null) {
      throw new ComponentException("'" + ArtifactHandler.HANDLER_NAME
          + "' is already mapped to " + cachedHandler.getClass().getName());
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
      final SsoPrincipal principal = getSsoPrincipal(session);
      if (principal != null) {
        LOGGER.debug("SSO Authentication attribute will be removed");
        session.removeAttribute(CONST_SSO_ASSERTION);

        // If we are also supposed to redirect to the SSO server to log out
        // of SSO, set up the redirect now.
        ArtifactHandler handler = artifactHandlers.get(principal.getHandlerName());
        if (handler == null) {
          LOGGER.warn("Unable to find handler for principal [handler="
              + principal.getHandlerName());
          return;
        }

        String logoutUrl = handler.getLogoutUrl(request);
        if (StringUtils.isNotBlank(logoutUrl)) {
          String target = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
          if (target == null || target.length() == 0) {
            target = request.getParameter(Authenticator.LOGIN_RESOURCE);
          }
          if (target != null && target.length() > 0 && !("/".equals(target))) {
            LOGGER
                .info(
                    "SSO logout about to override requested redirect to {} and instead redirect to {}",
                    target, logoutUrl);
          } else {
            LOGGER.debug("SSO logout will request redirect to {}", logoutUrl);
          }
          request.setAttribute(Authenticator.LOGIN_RESOURCE, logoutUrl);
        }
      }
    }
  }

  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.debug("extractCredentials called");

    AuthenticationInfo authnInfo = null;

    final HttpSession session = request.getSession(false);
    SsoPrincipal principal = getSsoPrincipal(session);
    if (principal != null) {
      LOGGER.debug("assertion found");
      authnInfo = createAuthnInfo(principal);
    } else {
      // go through artifact handler list to find one that can handle this request.
      ArtifactHandler handler = null;
      String handlerName = null;
      String artifact = null;
      for (Entry<String, ArtifactHandler> handlerEntry : artifactHandlers.entrySet()) {
        ArtifactHandler h = handlerEntry.getValue();
        artifact = h.extractArtifact(request);
        if (artifact != null) {
          handlerName = handlerEntry.getKey();
          request.setAttribute(ArtifactHandler.HANDLER_NAME, handlerName);
          handler = h;
          break;
        }
      }

      if (handler != null) {
        // make REST call to validate artifact
        try {
          // validate URL
          String validateUrl = handler.getValidateUrl(artifact, request);
          GetMethod get = new GetMethod(validateUrl);
          HttpClient httpClient = new HttpClient();
          int returnCode = httpClient.executeMethod(get);
          if (returnCode >= 200 && returnCode < 300) {
            String body = get.getResponseBodyAsString();
            String credentials = handler.extractCredentials(artifact, body, request);
            if (credentials != null) {
              principal = new SsoPrincipal(credentials, artifact, handlerName);
              request.getSession().setAttribute(CONST_SSO_ASSERTION, principal);
              authnInfo = createAuthnInfo(principal);
            } else {
              LOGGER.warn("Unable to extract credentials.");
            }
          } else {
            LOGGER.error("Failed response from validation server: [" + returnCode + "]");
          }
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      }
    }

    return authnInfo;
  }

  /**
   * Called after extractCredentials has returne non-null but logging into the repository
   * with the provided AuthenticationInfo failed.<br/>
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    LOGGER.debug("requestCredentials called");
    String handlerName = (String) request.getAttribute(ArtifactHandler.HANDLER_NAME);
    if (handlerName != null) {
      ArtifactHandler handler = artifactHandlers.get(handlerName);
      final String serviceUrl = constructServiceParameter(request);
      LOGGER.debug("Service URL = \"{}\"", serviceUrl);
      String urlToRedirectTo = handler.getLoginUrl(serviceUrl, request);
      LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
      response.sendRedirect(urlToRedirectTo);
      return true;
    } else {
      return false;
    }
  }

  //----------- LoginModulePlugin interface ----------------------------

  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set principals) {
  }

  public boolean canHandle(Credentials credentials) {
    return (getSsoPrincipal(credentials) != null);
  }

  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
  }

  public AuthenticationPlugin getAuthentication(Principal principal,
      Credentials credentials) throws RepositoryException {
    AuthenticationPlugin plugin = null;
    if (canHandle(credentials)) {
      plugin = new SsoAuthenticationPlugin(this);
    }
    return plugin;
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
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public void authenticationFailed(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationFailed called");
    final HttpSession session = request.getSession(false);
    if (session != null) {
      final SsoPrincipal principal = (SsoPrincipal) session
          .getAttribute(CONST_SSO_ASSERTION);
      if (principal != null) {
        LOGGER.warn("SSO assertion is set", new Exception());
      }
    }
  }

  /**
   * If a redirect is configured, this method will take care of the redirect.
   * <p>
   * If user auto-creation is configured, this method will check for an existing
   * Authorizable that matches the principal. If not found, it creates a new Jackrabbit
   * user with all properties blank except for the ID and a randomly generated password.
   * WARNING: Currently this will not perform the extra work done by the Nakamura
   * CreateUserServlet, and the resulting user will not be associated with a valid
   * profile.
   * <p>
   * TODO This really needs to be dropped to allow for user pull, person directory
   * integrations, etc. See SLING-1563 for the related issue of user population via
   * OpenID.
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationSucceeded called");

    // If the plug-in is intended to verify the existence of a matching Authorizable,
    // check that now.
    if (this.autoCreateUser) {
      boolean isUserValid = findOrCreateUser(authInfo);
      if (!isUserValid) {
        LOGGER
            .warn("SSO authentication succeeded but corresponding user not found or created");
        try {
          dropCredentials(request, response);
        } catch (IOException e) {
          LOGGER.error(
              "Failed to drop credentials after SSO authentication by invalid user", e);
        }
        return true;
      }
    }

    // Check for the default post-authentication redirect.
    return DefaultAuthenticationFeedbackHandler.handleRedirect(request, response);
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
    autoCreateUser = OsgiUtil.toBoolean(prop.get(SSO_AUTOCREATE_USER), false);
  }

  private static final class SsoPrincipal implements Principal {
    private static final long serialVersionUID = -6232157660434175773L;
    private String principalName;
    private String artifact;
    private String handlerName;

    public SsoPrincipal(String principalName, String artifact, String handlerName) {
      this.principalName = principalName;
      this.artifact = artifact;
      this.handlerName = handlerName;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.security.Principal#getName()
     */
    public String getName() {
      return principalName;
    }

    public String getArtifact() {
      return artifact;
    }

    public String getHandlerName() {
      return handlerName;
    }
  }

  private AuthenticationInfo createAuthnInfo(final SsoPrincipal principal) {
    AuthenticationInfo authnInfo = new AuthenticationInfo(SsoAuthConstants.SSO_AUTH_TYPE);
    SimpleCredentials credentials = new SimpleCredentials(principal.getName(),
        new char[] {});
    credentials.setAttribute(SsoPrincipal.class.getName(), principal);
    authnInfo.put(AuthenticationInfo.CREDENTIALS, credentials);
    return authnInfo;
  }

  /**
   * @param request
   * @return the URL to which the SSO server should redirect after successful
   * authentication. By default, this is the same URL from which authentication
   * was initiated (minus authentication-related query strings like "ticket").
   * A request attribute or parameter can be used to specify a different
   * return path.
   */
  private String constructServiceParameter(HttpServletRequest request)
      throws UnsupportedEncodingException {
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
    String url = URLEncoder.encode(serviceUrl.toString(), "UTF-8");
    return url;
  }

  private SsoPrincipal getSsoPrincipal(HttpSession session) {
    SsoPrincipal prin = null;
    if (session != null) {
      Object obj = session.getAttribute(CONST_SSO_ASSERTION);
      if (obj != null && obj instanceof SsoPrincipal) {
        prin = (SsoPrincipal) obj;
      }
    }
    return prin;
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
    final SsoPrincipal principal = getSsoPrincipal((Credentials) authInfo
        .get(AuthenticationInfo.CREDENTIALS));
    if (principal != null) {
      final String principalName = principal.getName();
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

    String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
        + user.getID();
    if (authzPostProcessService != null) {
      authzPostProcessService.process(user, session, Modification.onCreated(userPath));
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
