package org.sakaiproject.nakamura.casauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class CasAuthenticationHandlerTest {
  private CasAuthenticationHandler casAuthenticationHandler;
  private CasAuthenticationPlugin casAuthenticationPlugin;
  private SimpleCredentials casCredentials;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  HttpSession session;
  @Mock
  Assertion assertion;
  @Mock
  private AttributePrincipal casPrincipal;
  @Mock
  private SlingRepository repository;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private UserManager userManager;

  @Before
  public void setUp() throws RepositoryException {
    casAuthenticationHandler = new CasAuthenticationHandler();
    casAuthenticationHandler.repository = repository;
    when(adminSession.getUserManager()).thenReturn(userManager);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
  }

  // AuthenticationHandler tests.

  @Test
  public void testAuthenticateNoTicket() {
    assertNull(casAuthenticationHandler.extractCredentials(request, response));
  }

  @Test
  public void testDropNoSession() throws IOException {
    casAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void testDropNoAssertion() throws IOException {
    when(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).thenReturn(null);
    when(request.getSession(false)).thenReturn(session);
    casAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void testDropWithAssertion() throws IOException {
    Assertion assertion = mock(Assertion.class);
    when(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).thenReturn(assertion);
    when(request.getSession(false)).thenReturn(session);
    casAuthenticationHandler.dropCredentials(request, response);
    verify(session).removeAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION);
  }

  private void setUpCasCredentials() {
    when(casPrincipal.getName()).thenReturn("joe");
    when(assertion.getPrincipal()).thenReturn(casPrincipal);
    when(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).thenReturn(
        assertion);
    when(request.getSession(false)).thenReturn(session);
    AuthenticationInfo authenticationInfo = casAuthenticationHandler.extractCredentials(request, response);
    casCredentials = (SimpleCredentials) authenticationInfo.get(AuthenticationInfo.CREDENTIALS);
  }

  @Test
  public void testExtractCredentialsFromAssertion() {
    setUpCasCredentials();
    assertEquals(casCredentials.getUserID(), "joe");
  }

  // LoginModulePlugin tests.

  @Test
  public void testCanHandleCasCredentials() throws RepositoryException {
    setUpCasCredentials();
    assertTrue(casAuthenticationHandler.canHandle(casCredentials));
  }

  @Test
  public void testCannotHandleOtherCredentials() {
    SimpleCredentials credentials = new SimpleCredentials("joe", new char[0]);
    assertFalse(casAuthenticationHandler.canHandle(credentials));
  }

  @Test
  public void testGetPrincipal() {
    setUpCasCredentials();
    assertEquals("joe", casAuthenticationHandler.getPrincipal(casCredentials).getName());
  }

  @Test
  public void testImpersonate() throws FailedLoginException, RepositoryException {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, casAuthenticationHandler.impersonate(null, null));
  }

  // AuthenticationPlugin tests.

  @Test
  public void testDoNotAuthenticateUser() throws RepositoryException {
    casAuthenticationPlugin = new CasAuthenticationPlugin(casAuthenticationHandler);
    assertFalse(casAuthenticationPlugin.authenticate(casCredentials));
  }

  @Test
  public void testAuthenticateUser() throws RepositoryException {
    setUpCasCredentials();
    casAuthenticationPlugin = new CasAuthenticationPlugin(casAuthenticationHandler);
    assertTrue(casAuthenticationPlugin.authenticate(casCredentials));
  }

  // AuthenticationFeedbackHandler tests.

  private void setAutocreateUser(String bool) {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(CasAuthenticationHandler.CAS_AUTOCREATE_USER, bool);
    casAuthenticationHandler.activate(properties);
  }

  @Test
  public void testUnknownUserNoCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("false");
    setUpCasCredentials();
    AuthenticationInfo authenticationInfo = casAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = casAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(anyString(), anyString());
    verify(userManager, never()).createUser(anyString(), anyString(), any(Principal.class), anyString());
  }

  @Test
  public void testUnknownUserWithCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("true");
    setUpCasCredentials();
    AuthenticationInfo authenticationInfo = casAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = casAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager).createUser(eq("joe"), anyString());
  }

  @Test
  public void testUnknownUserWithFailedCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("true");
    doThrow(new AuthorizableExistsException("Hey Joe")).when(userManager).createUser(anyString(), anyString());
    setUpCasCredentials();
    AuthenticationInfo authenticationInfo = casAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = casAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertTrue(actionTaken);
    verify(userManager).createUser(eq("joe"), anyString());
  }

  @Test
  public void testKnownUserWithCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("true");
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("joe");
    when(userManager.getAuthorizable("joe")).thenReturn(jcrUser);
    setUpCasCredentials();
    AuthenticationInfo authenticationInfo = casAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = casAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(eq("joe"), anyString());
  }
}
