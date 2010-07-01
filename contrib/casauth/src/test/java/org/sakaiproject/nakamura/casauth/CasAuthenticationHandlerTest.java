package org.sakaiproject.nakamura.casauth;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CasAuthenticationHandlerTest {
  private CasAuthenticationHandler cah;

  @Before
  public void setup() {
    cah = new CasAuthenticationHandler();
  }

  @Test
  public void testAuthenticateNoSession() throws IOException {
    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getSession(false)).andReturn(null).times(2);
    expect(request.getParameter("sling:authRequestLogin")).andReturn(null);
    expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost/dev"));
    expect(request.getMethod()).andReturn("GET");
    expect(request.getQueryString()).andReturn(null);

    HttpServletResponse response = createMock(HttpServletResponse.class);
    expect(response.encodeURL("http://localhost/dev")).andReturn("http://localhost/dev");

    replay(request, response);

    assertNull(cah.extractCredentials(request, response));
  }

  @Test
  public void testDropNoSessionNoUrl() throws IOException {
    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getSession(false)).andReturn(null);

    HttpServletResponse response = createMock(HttpServletResponse.class);

    replay(request, response);

    cah.dropCredentials(request, response);
  }

  @Test
  public void testAuthenticationAssertionInSession() {
    AttributePrincipal principal = createMock(AttributePrincipal.class);
    expect(principal.getName()).andReturn("foo");
    Assertion assertion = createMock(Assertion.class);
    expect(assertion.getPrincipal()).andReturn(principal);
    expect(assertion.getAttributes()).andReturn(new HashMap<String, String>());
    HttpSession session = createMock(HttpSession.class);
    expect(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).andReturn(
        assertion);
    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getSession(false)).andReturn(session);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    replay(principal, assertion, session, request, response);

    AuthenticationInfo authenticationInfo = cah.extractCredentials(request, response);
    assertNotNull(authenticationInfo);
  }
}
