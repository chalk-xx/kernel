package org.sakaiproject.nakamura.auth.cas;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CasAuthenticationHandlerTest {
  CasAuthenticationHandler authnHandler;
  SimpleCredentials creds;

  static final String ARTIFACT = "some-great-token-id";

  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  ValueFactory valueFactory;
  @Mock
  SlingRepository repository;
  @Mock
  JackrabbitSession adminSession;
  @Mock
  UserManager userManager;

  LocalTestServer server;
  HashMap<String, Object> props = new HashMap<String, Object>();

  @Before
  public void setUp() throws RepositoryException {
    authnHandler = new CasAuthenticationHandler(repository);
    authnHandler.activate(props);

    when(adminSession.getUserManager()).thenReturn(userManager);
    when(adminSession.getValueFactory()).thenReturn(valueFactory);
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
    CasAuthenticationHandler handler = new CasAuthenticationHandler();
    handler.authenticationFailed(null, null, null);
  }

  @Test
  public void authenticateNoTicket() {
    assertNull(authnHandler.extractCredentials(request, response));
  }

  @Test
  public void dropNoSession() throws IOException {
    authnHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsNoAssertion() throws IOException {
    authnHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsWithAssertion() throws IOException {
    authnHandler.dropCredentials(request, response);
  }

  @Test
  public void dropCredentialsWithLogoutUrl() throws IOException {
    authnHandler.dropCredentials(request, response);

    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/cas/logout");
  }

  @Test
  public void dropCredentialsWithRedirectTarget() throws IOException {
    when(request.getAttribute(Authenticator.LOGIN_RESOURCE)).thenReturn("goHere");

    authnHandler.dropCredentials(request, response);

    verify(request).setAttribute(Authenticator.LOGIN_RESOURCE, "http://localhost/cas/logout");
  }

  @Test
  public void extractCredentialsNoAssertion() throws Exception {
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("resource=/dev/index.html");

    AuthenticationInfo authenticationInfo = authnHandler.extractCredentials(
        request, response);

    assertNull(authenticationInfo);

    verify(request, never()).setAttribute(eq(CasAuthenticationHandler.AUTHN_INFO),
        isA(AuthenticationInfo.class));
  }

  @Test
  public void extractCredentialsNegativeResponse() throws Exception {
    setUpSsoCredentials(false);

    AuthenticationInfo authenticationInfo = authnHandler.extractCredentials(
        request, response);

    assertSame(AuthenticationInfo.FAIL_AUTH, authenticationInfo);
  }

  @Test
  public void extractCredentialsFromAssertion() throws Exception {
    setUpSsoCredentials(true);

    AuthenticationInfo authenticationInfo = authnHandler.extractCredentials(
        request, response);

    assertNotNull(authenticationInfo);

    creds = (SimpleCredentials) authenticationInfo
        .get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);

    assertEquals("NetID", authenticationInfo.getUser());
    assertEquals("NetID", creds.getUserID());

    verify(request).setAttribute(eq(CasAuthenticationHandler.AUTHN_INFO),
        isA(AuthenticationInfo.class));
  }

  // AuthenticationFeedbackHandler tests.

  @Test
  public void unknownUserNoCreation() throws Exception {
    setUpSsoCredentials(true);
    AuthenticationInfo authenticationInfo = authnHandler.extractCredentials(
        request, response);
    boolean actionTaken = authnHandler.authenticationSucceeded(request,
        response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(anyString(), anyString());
    verify(userManager, never()).createUser(anyString(), anyString(),
        any(Principal.class), anyString());
  }

  @Test
  public void requestCredentials() throws Exception {
    setUpSsoCredentials(true);
    assertTrue(authnHandler.requestCredentials(request, response));
    verify(response).sendRedirect(isA(String.class));
  }

  // ---------- helper methods
  private void setUpSsoCredentials(boolean pass) throws Exception {
    when(request.getParameter(CasAuthenticationHandler.DEFAULT_ARTIFACT_NAME)).thenReturn(ARTIFACT);

    setupValidateHandler(pass);

    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("resource=/dev/index.html");
  }

  private void setupValidateHandler(final boolean pass) throws Exception {
    if (server == null) {
      server = new LocalTestServer(null, null);
      server.start();
    }

    String url = "http://" + server.getServiceHostName() + ":" + server.getServicePort()
        + "/cas";
    props.put(CasAuthenticationHandler.SERVER_URL, url);
    authnHandler.modified(props);

    server.register("/cas/serviceValidate", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        if (pass) {
          response.setEntity(new StringEntity(
              "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n"
              + "  <cas:authenticationSuccess>\n"
              + "    <cas:user>NetID</cas:user>\n"
              + "  </cas:authenticationSuccess>\n"
              + "</cas:serviceResponse>\n"));
        } else {
          response.setEntity(new StringEntity(
                  "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n"
                  + "  <cas:authenticationFailure code='INVALID_REQUEST'>\n"
                  + "    &#039;service&#039; and &#039;ticket&#039; parameters are both required\n"
                  + "  </cas:authenticationFailure>\n"
                  + "</cas:serviceResponse>\n"));
        }
      }
    });

  }

}
