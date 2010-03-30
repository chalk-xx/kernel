package org.sakaiproject.nakamura.casauth;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.junit.Before;
import org.junit.Test;

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

    cah.extractCredentials(request, response);
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
  public void testDropNoUrl() throws IOException {
    HttpSession session = createMock(HttpSession.class);
    session.invalidate();
    expectLastCall();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getSession(false)).andReturn(session);

    HttpServletResponse response = createMock(HttpServletResponse.class);

    replay(session, request, response);

    cah.dropCredentials(request, response);
  }

  // @Test
  // public void testAuthenticateTicket() {
  //
  // Properties props = new Properties();
  // props.put("auth.cas.server.name", "https://localhost:8443");
  // props.put("auth.cas.server.login", "https://localhost:8443/cas/login");
  //
  // ComponentContext context = createMock(ComponentContext.class);
  // expect(context.getProperties()).andReturn(props);
  //
  // String ticket = "ST-17486-aIHMWmhg5cca21aALeAu-localhost";
  //
  // HttpServletRequest request = createMock(HttpServletRequest.class);
  // expect(request.getSession(false)).andReturn(null).times(2);
  // expect(request.getParameter("sling:authRequestLogin")).andReturn(null);
  // expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost/dev"));
  // expect(request.getMethod()).andReturn("GET");
  // expect(request.getQueryString()).andReturn("ticket=" + ticket).times(2);
  // expect(request.getParameter("ticket")).andReturn(ticket);
  //
  // HttpServletResponse response = createMock(HttpServletResponse.class);
  // expect(response.encodeURL("http://localhost/dev")).andReturn("http://localhost/dev");
  //
  // replay(context, request, response);
  //
  // cah.activate(context);
  // cah.authenticate(request, response);
  // }

  @Test
  public void testAuthenticationAssertionInSession() {

    AttributePrincipal principal = createMock(AttributePrincipal.class);
    expect(principal.getName()).andReturn("foo");

    HashMap<String, String> attribs = new HashMap<String, String>();
    attribs.put("emailaddr", "foo@localhost");

    Assertion assertion = createMock(Assertion.class);
    expect(assertion.getPrincipal()).andReturn(principal);
    expect(assertion.getAttributes()).andReturn(attribs);

    HttpSession session = createMock(HttpSession.class);
    expect(session.getAttribute(CasAuthenticationHandler.CONST_CAS_ASSERTION)).andReturn(
        assertion);

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getSession(false)).andReturn(session);
    expect(request.getParameter("sling:authRequestLogin")).andReturn(null);
    expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost/dev"));
    expect(request.getMethod()).andReturn("GET");
    expect(request.getQueryString()).andReturn(null);

    HttpServletResponse response = createMock(HttpServletResponse.class);
    expect(response.encodeURL("http://localhost/dev")).andReturn("http://localhost/dev");

    replay(principal, assertion, session, request, response);

    cah.extractCredentials(request, response);
  }
}
