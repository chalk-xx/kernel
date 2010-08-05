package org.sakaiproject.nakamura.auth.sso;

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
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class SsoAuthenticationHandlerTest {
  private SsoAuthenticationHandler ssoAuthenticationHandler;
  private SsoAuthenticationPlugin ssoAuthenticationPlugin;
  private SimpleCredentials ssoCredentials;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  HttpSession session;
  @Mock
  ValueFactory valueFactory;
  @Mock
  private Principal ssoPrincipal;
  @Mock
  private SlingRepository repository;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private UserManager userManager;

  @Before
  public void setUp() throws RepositoryException {
    ssoAuthenticationHandler = new SsoAuthenticationHandler(repository, null, null);
    when(adminSession.getUserManager()).thenReturn(userManager);
    when(adminSession.getValueFactory()).thenReturn(valueFactory);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
  }

  // AuthenticationHandler tests.

  @Test
  public void testAuthenticateNoTicket() {
    assertNull(ssoAuthenticationHandler.extractCredentials(request, response));
  }

  @Test
  public void testDropNoSession() throws IOException {
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void testDropNoAssertion() throws IOException {
    when(session.getAttribute(SsoAuthenticationHandler.CONST_SSO_ASSERTION)).thenReturn(null);
    when(request.getSession(false)).thenReturn(session);
    ssoAuthenticationHandler.dropCredentials(request, response);
  }

  @Test
  public void testDropWithAssertion() throws IOException {
    setUpSsoCredentials();
    when(request.getSession(false)).thenReturn(session);
    ssoAuthenticationHandler.dropCredentials(request, response);
    verify(session).removeAttribute(SsoAuthenticationHandler.CONST_SSO_ASSERTION);
  }

  private void setUpSsoCredentials() {
    ArgumentCaptor<Object> sessionCaptor = ArgumentCaptor.forClass(Object.class);

    session.setAttribute(eq(SsoAuthenticationHandler.CONST_SSO_ASSERTION), sessionCaptor.capture());
    when(ssoPrincipal.getName()).thenReturn("joe");
    when(session.getAttribute(SsoAuthenticationHandler.CONST_SSO_ASSERTION)).thenReturn(sessionCaptor.getValue());
    when(request.getSession(false)).thenReturn(session);
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    ssoCredentials = (SimpleCredentials) authenticationInfo.get(AuthenticationInfo.CREDENTIALS);
  }

  @Test
  public void testExtractCredentialsFromAssertion() {
    setUpSsoCredentials();
    assertEquals(ssoCredentials.getUserID(), "joe");
  }

  // LoginModulePlugin tests.

  @Test
  public void testCanHandleSsoCredentials() throws RepositoryException {
    setUpSsoCredentials();
    assertTrue(ssoAuthenticationHandler.canHandle(ssoCredentials));
  }

  @Test
  public void testCannotHandleOtherCredentials() {
    SimpleCredentials credentials = new SimpleCredentials("joe", new char[0]);
    assertFalse(ssoAuthenticationHandler.canHandle(credentials));
  }

  @Test
  public void testGetPrincipal() {
    setUpSsoCredentials();
    assertEquals("joe", ssoAuthenticationHandler.getPrincipal(ssoCredentials).getName());
  }

  @Test
  public void testImpersonate() throws FailedLoginException, RepositoryException {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, ssoAuthenticationHandler.impersonate(null, null));
  }

  // AuthenticationPlugin tests.

  @Test
  public void testDoNotAuthenticateUser() throws RepositoryException {
    ssoAuthenticationPlugin = new SsoAuthenticationPlugin(ssoAuthenticationHandler);
    assertFalse(ssoAuthenticationPlugin.authenticate(ssoCredentials));
  }

  @Test
  public void testAuthenticateUser() throws RepositoryException {
    setUpSsoCredentials();
    ssoAuthenticationPlugin = new SsoAuthenticationPlugin(ssoAuthenticationHandler);
    assertTrue(ssoAuthenticationPlugin.authenticate(ssoCredentials));
  }

  // AuthenticationFeedbackHandler tests.

  private void setAutocreateUser(String bool) {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(SsoAuthenticationHandler.SSO_AUTOCREATE_USER, bool);
    ssoAuthenticationHandler.activate(properties);
  }

  @Test
  public void testUnknownUserNoCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("false");
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(anyString(), anyString());
    verify(userManager, never()).createUser(anyString(), anyString(), any(Principal.class), anyString());
  }

  @Test
  public void testUnknownUserWithFailedCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("true");
    doThrow(new AuthorizableExistsException("Hey Joe")).when(userManager).createUser(anyString(), anyString());
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertTrue(actionTaken);
    verify(userManager).createUser(eq("joe"), anyString());
  }

  @Test
  public void testKnownUserWithCreation() throws AuthorizableExistsException, RepositoryException {
    setAutocreateUser("true");
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("joe");
    when(userManager.getAuthorizable("joe")).thenReturn(jcrUser);
    setUpSsoCredentials();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager, never()).createUser(eq("joe"), anyString());
  }

  private void setUpPseudoCreateUserService() throws Exception {
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("joe");
    ItemBasedPrincipal principal = mock(ItemBasedPrincipal.class);
    when(principal.getPath()).thenReturn(UserConstants.USER_REPO_LOCATION + "/joes");
    when(jcrUser.getPrincipal()).thenReturn(principal);
    when(userManager.createUser(eq("joe"), anyString())).thenReturn(jcrUser);
  }

  @Test
  public void testUnknownUserWithCreation() throws Exception {
    setAutocreateUser("true");
    setUpSsoCredentials();
    setUpPseudoCreateUserService();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(userManager).createUser(eq("joe"), anyString());
  }

  @Test
  public void testPostProcessingAfterUserCreation() throws Exception {
    AuthorizablePostProcessService postProcessService = mock(AuthorizablePostProcessService.class);
    ssoAuthenticationHandler.authzPostProcessService = postProcessService;
    setAutocreateUser("true");
    setUpSsoCredentials();
    setUpPseudoCreateUserService();
    AuthenticationInfo authenticationInfo = ssoAuthenticationHandler.extractCredentials(request, response);
    boolean actionTaken = ssoAuthenticationHandler.authenticationSucceeded(request, response, authenticationInfo);
    assertFalse(actionTaken);
    verify(postProcessService).process(any(Authorizable.class), any(Session.class), any(Modification.class));
  }
}
