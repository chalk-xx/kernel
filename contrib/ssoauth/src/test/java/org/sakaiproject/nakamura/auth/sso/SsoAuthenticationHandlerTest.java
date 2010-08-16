package org.sakaiproject.nakamura.auth.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class SsoAuthenticationHandlerTest {
  SsoAuthenticationHandler ssoAuthenticationHandler;
  SimpleCredentials ssoCredentials;

  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  HttpSession session;
  @Mock
  ValueFactory valueFactory;
  @Mock
  SlingRepository repository;
  @Mock
  JackrabbitSession adminSession;
  @Mock
  UserManager userManager;
  @Mock
  ArtifactHandler artifactHandler;
  @Mock
  AuthorizablePostProcessService authzPostProcessService;

  LocalTestServer server;
  HashMap<String, Object> props = new HashMap<String, Object>();

  @Before
  public void setUp() throws RepositoryException {
    props.put(ArtifactHandler.HANDLER_NAME, "someHandler");
    ssoAuthenticationHandler = new SsoAuthenticationHandler(repository,
        authzPostProcessService, null);
    ssoAuthenticationHandler.activate(props);

    when(adminSession.getUserManager()).thenReturn(userManager);
    when(adminSession.getValueFactory()).thenReturn(valueFactory);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
    ssoAuthenticationHandler.unbindArtifactHandlers(artifactHandler, props);
  }

  @Test
  public void coverageBooster() throws Exception {
    SsoAuthenticationHandler handler = new SsoAuthenticationHandler();
    handler.authenticationFailed(null, null, null);

    HashMap<String, ArtifactHandler> handlers = new HashMap<String, ArtifactHandler>();
    handlers.put("whatever", artifactHandler);
    handler = new SsoAuthenticationHandler(repository, authzPostProcessService, handlers);
  }

  @Test(expected = ComponentException.class)
  public void failtoRebindWithSameName() {
    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
  }

  @Test
  public void bindNewDefault() {
    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
    props.put(ArtifactHandler.HANDLER_NAME, "yayName");
    props.put(ArtifactHandler.DEFAULT_HANDLER, true);
    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
  }

  @Test
  public void authenticateNoTicket() {
    assertNull(ssoAuthenticationHandler.extractCredentials(request, response));
  }

  @Test
  public void dropNoSession() throws IOException {
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsNoAssertion() throws IOException {
    when(session.getAttribute(ArtifactHandler.HANDLER_NAME)).thenReturn(null);
    when(request.getSession(false)).thenReturn(session);
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsWithAssertion() throws IOException {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(ArtifactHandler.HANDLER_NAME)).thenReturn("someHandler");
    ssoAuthenticationHandler.dropCredentials(request, response);
    verify(session).removeAttribute(ArtifactHandler.HANDLER_NAME);
  }

  @Test
  public void dropCredentialsWithLogoutUrl() throws IOException {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(ArtifactHandler.HANDLER_NAME)).thenReturn("someHandler");
    when(artifactHandler.getLogoutUrl(request)).thenReturn("http://localhost/logout");

    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
    ssoAuthenticationHandler.dropCredentials(request, response);

    verify(session).removeAttribute(ArtifactHandler.HANDLER_NAME);
    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/logout");
  }

  @Test
  public void dropCredentialsWithRedirectTarget() throws IOException {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(ArtifactHandler.HANDLER_NAME)).thenReturn("someHandler");
    when(artifactHandler.getLogoutUrl(request)).thenReturn("http://localhost/logout");
    when(request.getAttribute(Authenticator.LOGIN_RESOURCE)).thenReturn("goHere");

    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);
    ssoAuthenticationHandler.dropCredentials(request, response);

    verify(session).removeAttribute(ArtifactHandler.HANDLER_NAME);
    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/logout");
  }

  @Test
  public void extractCredentialsFromAssertion() throws Exception {
    setUpSsoCredentials();
    when(request.getSession()).thenReturn(session);
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);

    ssoCredentials = (SimpleCredentials) authenticationInfo
        .get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);

    assertEquals("someUser", authenticationInfo.getUser());
    assertEquals("someUser", ssoCredentials.getUserID());

    verify(request).setAttribute(eq(SsoAuthenticationHandler.AUTHN_INFO),
        isA(AuthenticationInfo.class));
  }

  // AuthenticationFeedbackHandler tests.

  @Test
  public void unknownUserNoCreation() throws Exception {
    setAutocreateUser("false");
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(anyString(), anyString());
    verify(userManager, never()).createUser(anyString(), anyString(),
        any(Principal.class), anyString());
  }

  @Test
  public void findUnknownUserWithFailedCreation() throws Exception {
    setAutocreateUser("true");
    doThrow(new AuthorizableExistsException("Hey someUser")).when(userManager).createUser(
        anyString(), anyString());
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertTrue(actionTaken);
    verify(userManager).createUser(eq("someUser"), anyString());
  }

  @Test
  public void findKnownUserWithCreation() throws Exception {
    setAutocreateUser("true");
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("someUser");
    when(userManager.getAuthorizable("someUser")).thenReturn(jcrUser);
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(eq("someUser"), anyString());
  }

  private void setUpPseudoCreateUserService() throws Exception {
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("someUser");
    ItemBasedPrincipal principal = mock(ItemBasedPrincipal.class);
    when(principal.getPath()).thenReturn(UserConstants.USER_REPO_LOCATION + "/someUsers");
    when(jcrUser.getPrincipal()).thenReturn(principal);
    when(userManager.createUser(eq("someUser"), anyString())).thenReturn(jcrUser);
  }

  @Test
  public void findUnknownUserWithCreation() throws Exception {
    setAutocreateUser("true");
    setUpSsoCredentials();
    setUpPseudoCreateUserService();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager).createUser(eq("someUser"), anyString());
  }

  @Test
  public void postProcessingAfterUserCreation() throws Exception {
    AuthorizablePostProcessService postProcessService = mock(AuthorizablePostProcessService.class);
    ssoAuthenticationHandler.authzPostProcessService = postProcessService;
    setAutocreateUser("true");
    setUpSsoCredentials();
    setUpPseudoCreateUserService();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(postProcessService).process(any(Authorizable.class), any(Session.class),
        any(Modification.class));
  }

  @Test
  public void requestCredentialsWithoutHandler() throws Exception {
    assertFalse(ssoAuthenticationHandler.requestCredentials(request, null));
  }

  @Test
  public void requestCredentialsWithHandler() throws Exception {
    setUpSsoCredentials();
    when(request.getAttribute(ArtifactHandler.HANDLER_NAME)).thenReturn(
        props.get(ArtifactHandler.HANDLER_NAME));
    when(artifactHandler.getLoginUrl(isA(String.class), eq(request))).thenReturn(
        "http://localhost/login");
    assertTrue(ssoAuthenticationHandler.requestCredentials(request, response));
    verify(response).sendRedirect(isA(String.class));
  }

  // ---------- helper methods
  private void setUpSsoCredentials() throws Exception {
    String validateUrl = setupValidateHandler();

    when(artifactHandler.extractArtifact(request)).thenReturn("artifact");
    when(
        artifactHandler.getValidateUrl(isA(String.class), isA(String.class),
            isA(HttpServletRequest.class))).thenReturn(validateUrl);
    when(
        artifactHandler.extractCredentials(isA(String.class), isA(String.class),
            isA(HttpServletRequest.class))).thenReturn("someUser");

    ssoAuthenticationHandler.bindArtifactHandlers(artifactHandler, props);

    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("resource=/dev/index.html");
  }

  private String setupValidateHandler() throws Exception {
    if (server == null) {
      server = new LocalTestServer(null, null);
      server.start();
    }

    String validateUrl = "http://" + server.getServiceHostName() + ":"
        + server.getServicePort() + "/validate";
    server.register("/validate", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        response.setEntity(new StringEntity("some great response"));
      }
    });
    return validateUrl;
  }

  private void setAutocreateUser(String bool) {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(SsoAuthenticationHandler.SSO_AUTOCREATE_USER, bool);
    ssoAuthenticationHandler.modified(properties);
  }
}
