package org.sakaiproject.nakamura.auth.opensso;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
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
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class OpenSsoAuthenticationHandlerTest {
  OpenSsoAuthenticationHandler ssoAuthenticationHandler;
  SimpleCredentials ssoCredentials;

  static final String ARTIFACT = "some-great-token-id";

  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  ValueFactory valueFactory;
  @Mock
  Repository repository;
  @Mock
  Session adminSession;
  @Mock
  AuthorizableManager authMgr;
  @Mock
  LiteAuthorizablePostProcessService authzPostProcessService;

  LocalTestServer server;
  HashMap<String, Object> props = new HashMap<String, Object>();

  @Before
  public void setUp() throws Exception {
    when(request.getServerName()).thenReturn("localhost");

    ssoAuthenticationHandler = new OpenSsoAuthenticationHandler(repository,
        authzPostProcessService);
    ssoAuthenticationHandler.activate(props);

    when(adminSession.getAuthorizableManager()).thenReturn(authMgr);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void coverageBooster() throws Exception {
    OpenSsoAuthenticationHandler handler = new OpenSsoAuthenticationHandler();
    handler.authenticationFailed(null, null, null);
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
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsWithAssertion() throws IOException {
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsWithLogoutUrl() throws IOException {
    ssoAuthenticationHandler.dropCredentials(request, response);

    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/sso/UI/Logout");
  }

  @Test
  public void dropCredentialsWithRedirectTarget() throws IOException {
    when(request.getAttribute(Authenticator.LOGIN_RESOURCE)).thenReturn("goHere");

    ssoAuthenticationHandler.dropCredentials(request, response);

    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/sso/UI/Logout");
  }

  @Test
  public void extractCredentialsNoAssertion() throws Exception {
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("resource=/dev/index.html");

    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);

    assertNull(authenticationInfo);

    verify(request, never()).setAttribute(eq(OpenSsoAuthenticationHandler.AUTHN_INFO),
        isA(AuthenticationInfo.class));
  }

  @Test
  public void extractCredentialsFromAssertion() throws Exception {
    setUpSsoCredentials();

    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);

    assertNotNull(authenticationInfo);

    ssoCredentials = (SimpleCredentials) authenticationInfo
        .get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);

    assertEquals("someUserId", authenticationInfo.getUser());
    assertEquals("someUserId", ssoCredentials.getUserID());

    verify(request).setAttribute(eq(OpenSsoAuthenticationHandler.AUTHN_INFO),
        isA(AuthenticationInfo.class));
  }

  // AuthenticationFeedbackHandler tests.

  @SuppressWarnings("unchecked")
  @Test
  public void unknownUserNoCreation() throws Exception {
    setAutocreateUser(false);
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertTrue(actionTaken);
    verify(authMgr, never()).createUser(anyString(), anyString(), anyString(), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void findUnknownUserWithFailedCreation() throws Exception {
    setAutocreateUser(true);
    when(authMgr.createUser(anyString(), anyString(), anyString(), any(Map.class))).thenReturn(Boolean.FALSE);
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertTrue(actionTaken);
    verify(authMgr).createUser(eq("someUserId"), anyString(), anyString(), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void findKnownUserWithCreation() throws Exception {
    setAutocreateUser(true);
    User liteUser = mock(User.class);
    when(liteUser.getId()).thenReturn("someUserId");
    when(authMgr.findAuthorizable("someUserId")).thenReturn(liteUser);
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(authMgr, never()).createUser(eq("someUserId"), anyString(), anyString(), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  private User setUpPseudoCreateUserService() throws Exception {
    User liteUser = mock(User.class);
    when(liteUser.getId()).thenReturn("someUserId");
    ItemBasedPrincipal principal = mock(ItemBasedPrincipal.class);
    when(principal.getPath()).thenReturn(UserConstants.USER_REPO_LOCATION + "/someUserIds");
    when(authMgr.createUser(eq("someUserId"), anyString(), anyString(), any(Map.class))).thenReturn(Boolean.TRUE);
    return liteUser;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void findUnknownUserWithCreation() throws Exception {
    setAutocreateUser(true);
    setUpSsoCredentials();
    setUpPseudoCreateUserService();
    User liteUser = setUpPseudoCreateUserService();
    when(authMgr.findAuthorizable("someUserId")).thenReturn(null).thenReturn(liteUser);
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(authMgr).createUser(eq("someUserId"), anyString(), anyString(), any(Map.class));
  }

  @Test
  public void postProcessingAfterUserCreation() throws Exception {
    LiteAuthorizablePostProcessService postProcessService = mock(LiteAuthorizablePostProcessService.class);
    ssoAuthenticationHandler.authzPostProcessService = postProcessService;
    setAutocreateUser(true);
    setUpSsoCredentials();
    User liteUser = setUpPseudoCreateUserService();
    when(authMgr.findAuthorizable("someUserId")).thenReturn(null).thenReturn(liteUser);
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(
        request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(postProcessService).process(any(Authorizable.class), any(Session.class),
        any(ModificationType.class), any(SlingHttpServletRequest.class));
  }

  @Test
  public void requestCredentialsWithHandler() throws Exception {
    setUpSsoCredentials();
    assertTrue(ssoAuthenticationHandler.requestCredentials(request, response));
    verify(response).sendRedirect(isA(String.class));
  }

  // ---------- helper methods
  private void setUpSsoCredentials() throws Exception {
    Cookie[] cookies = new Cookie[2];
    cookies[0] = new Cookie("some-other-cookie", "nothing-great");
    cookies[1] = new Cookie(OpenSsoAuthenticationHandler.DEFAULT_ARTIFACT_NAME, ARTIFACT);
    when(request.getCookies()).thenReturn(cookies);

    setupValidateHandler();

    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("resource=/dev/index.html");
  }

  private void setupValidateHandler() throws Exception {
    if (server == null) {
      server = new LocalTestServer(null, null);
      server.start();
    }

    String url = "http://" + server.getServiceHostName() + ":"
        + server.getServicePort() + "/sso"; // /identity/isTokenValid";
    props.put(OpenSsoAuthenticationHandler.SERVER_URL, url);
    ssoAuthenticationHandler.modified(props);

    server.register("/sso/identity/isTokenValid", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        response.setEntity(new StringEntity(
            OpenSsoAuthenticationHandler.DEFAULT_SUCCESSFUL_BODY));
      }
    });

    server.register("/sso/identity/attributes", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        String output = "Random Opening Line That Shouldn't Matter\n"
            + OpenSsoAuthenticationHandler.USRDTLS_ATTR_NAME_STUB
            + OpenSsoAuthenticationHandler.DEFAULT_ATTRIBUTE_NAME + "\n"
            + OpenSsoAuthenticationHandler.USRDTLS_ATTR_VAL_STUB + "someUserId";
        response.setEntity(new StringEntity(output));
      }
    });

  }

  private void setAutocreateUser(boolean bool) {
    props.put(OpenSsoAuthenticationHandler.SSO_AUTOCREATE_USER, bool);
    ssoAuthenticationHandler.modified(props);
  }
}
