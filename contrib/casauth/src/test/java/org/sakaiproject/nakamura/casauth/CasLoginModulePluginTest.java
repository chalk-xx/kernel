package org.sakaiproject.nakamura.casauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
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

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class CasLoginModulePluginTest {
  private CasAuthenticationHandler casLoginModulePlugin;
  private CasAuthentication casAuthentication;
  private SimpleCredentials casCredentials;
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
    casLoginModulePlugin = new CasAuthenticationHandler();
    when(adminSession.getUserManager()).thenReturn(userManager);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
  }
  
  private void setUpCasCredentials() {
    when(casPrincipal.getName()).thenReturn("joe");
    Assertion assertion = mock(Assertion.class);
    when(assertion.getPrincipal()).thenReturn(casPrincipal);
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).thenReturn(
        assertion);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(session);
    HttpServletResponse response = mock(HttpServletResponse.class);
    AuthenticationInfo authenticationInfo = casLoginModulePlugin.extractCredentials(request, response);
    casCredentials = (SimpleCredentials) authenticationInfo.get(AuthenticationInfo.CREDENTIALS);
  }
  
  @Test
  public void testCanHandleCasCredentials() throws RepositoryException {
    setUpCasCredentials();
    assertTrue(casLoginModulePlugin.canHandle(casCredentials));
  }

  @Test
  public void testCannotHandleOtherCredentials() {
    SimpleCredentials credentials = new SimpleCredentials("joe", new char[0]);
    assertFalse(casLoginModulePlugin.canHandle(credentials));
  }
  
  @Test
  public void testGetPrincipal() {
    setUpCasCredentials();
    assertEquals("joe", casLoginModulePlugin.getPrincipal(casCredentials).getName());
  }
  
  @Test
  public void testImpersonate() throws FailedLoginException, RepositoryException {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, casLoginModulePlugin.impersonate(null, null));
  }
  
  @Test
  public void testAuthenticateExistingUser() throws RepositoryException {
    User jcrUser = mock(User.class);
    when(jcrUser.getID()).thenReturn("joe");
    when(userManager.getAuthorizable(anyString())).thenReturn(jcrUser);
    setUpCasCredentials();
    casAuthentication = new CasAuthentication(casPrincipal, repository, casLoginModulePlugin);
    assertTrue(casAuthentication.authenticate(casCredentials));
  }
  
  @Test
  public void testDoNotAuthenticateUser() throws RepositoryException {
    casAuthentication = new CasAuthentication(casPrincipal, repository, casLoginModulePlugin);
    assertFalse(casAuthentication.authenticate(casCredentials));
  }

  @Test
  public void testAuthenticateUnknownUser() throws RepositoryException {
    setUpCasCredentials();
    casAuthentication = new CasAuthentication(casPrincipal, repository, casLoginModulePlugin);
    assertTrue(casAuthentication.authenticate(casCredentials));
    verify(userManager).createUser(eq("joe"), anyString());
  }
}
