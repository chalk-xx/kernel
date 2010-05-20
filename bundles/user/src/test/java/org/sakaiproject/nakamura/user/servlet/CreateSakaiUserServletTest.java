package org.sakaiproject.nakamura.user.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class CreateSakaiUserServletTest extends AbstractEasyMockTest {


  @Test
  public void testNoPrincipalName() throws RepositoryException {
    badNodeNameParam(null, "User name was not submitted");
  }

  @Test
  public void testBadPrefix() throws RepositoryException {
    badNodeNameParam("g-foo", "User name must not begin 'g-'");
  }

  private void badNodeNameParam(String name, String exception) throws  RepositoryException {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    UserManager userManager = createMock(UserManager.class);
    User user = createMock(User.class);
    expect(request.getResourceResolver()).andReturn(rr).anyTimes();
    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();
    
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("userID");
    expect(userManager.getAuthorizable("userID")).andReturn(user);
    expect(user.isAdmin()).andReturn(false);
    
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn(name);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csus.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals(exception, e.getMessage());
    }
    verify();
  }

  @Test
  public void testNoPwd() throws RepositoryException {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    UserManager userManager = createMock(UserManager.class);
    User user = createMock(User.class);
    expect(request.getResourceResolver()).andReturn(rr).anyTimes();
    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();

    
    
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("userID");
    expect(userManager.getAuthorizable("userID")).andReturn(user);
    expect(user.isAdmin()).andReturn(false);

    
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
    verify();
  }

  @Test
  public void testNotPwdEqualsPwdConfirm() throws RepositoryException {
    CreateSakaiUserServlet csus = new CreateSakaiUserServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    UserManager userManager = createMock(UserManager.class);
    User user = createMock(User.class);
    expect(request.getResourceResolver()).andReturn(rr).anyTimes();
    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();

    
    
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("userID");
    expect(userManager.getAuthorizable("userID")).andReturn(user);
    expect(user.isAdmin()).andReturn(false);
    
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
    verify();
  }
}
