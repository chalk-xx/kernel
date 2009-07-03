package org.sakaiproject.kernel.user.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class CreateSakaiUserServletTest extends AbstractEasyMockTest {

  @Test
  public void testNoSession() {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(null);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResourceResolver()).andReturn(rr);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csus.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals("JCR Session not found", e.getMessage());
    }
  }

  @Test
  public void testNoPrincipalName() {
    badNodeNameParam(null, "User name was not submitted");
  }

  @Test
  public void testBadPrefix() {
    badNodeNameParam("g-foo", "User name must not begin 'g-'");
  }

  private void badNodeNameParam(String name, String exception) {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResourceResolver()).andReturn(rr);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn(name);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csus.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals(exception, e.getMessage());
    }
  }

  @Test
  public void testNoPwd() {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResourceResolver()).andReturn(rr);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("foo");
    expect(request.getParameter("pwd")).andReturn(null);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csus.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals("Password was not submitted", e.getMessage());
    }
  }

  @Test
  public void testNotPwdEqualsPwdConfirm() {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResourceResolver()).andReturn(rr);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("foo");
    expect(request.getParameter("pwd")).andReturn("bar");
    expect(request.getParameter("pwdConfirm")).andReturn("baz");

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csus.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals("Password value does not match the confirmation password", e
          .getMessage());
    }
  }
}
