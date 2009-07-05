package org.sakaiproject.kernel.user.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

public class CreateSakaiGroupServletTest extends AbstractEasyMockTest {

  @Test
  public void testNullGroupName() {
    handleBadGroupName(null, "Group name was not submitted");
  }

  @Test
  public void testWrongPrefix() {
    handleBadGroupName("foo", "Group names must begin with 'g-'");
  }

  private void handleBadGroupName(String name, String expectedMessage) {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn(name);

    HtmlResponse response = new HtmlResponse();

    replay();
    try {
      csgs.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  @Test
  public void testNoSession() {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(null);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csgs.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals("JCR Session not found", e.getMessage());
    }
  }

  @Test
  public void testPrincipalExists() {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    Authorizable authorizable = createMock(Authorizable.class);

    UserManager userManager = createMock(UserManager.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    try {
      expect(userManager.getAuthorizable("g-foo")).andReturn(authorizable);
      expect(session.getUserManager()).andReturn(userManager);
    } catch (AccessDeniedException e1) {
      e1.printStackTrace();
    } catch (UnsupportedRepositoryOperationException e1) {
      e1.printStackTrace();
    } catch (RepositoryException e1) {
      e1.printStackTrace();
    }

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr);

    HtmlResponse response = new HtmlResponse();

    replay();

    try {
      csgs.handleOperation(request, response, null);
      fail();
    } catch (RepositoryException e) {
      assertEquals(
          "Failed to create new group.: A principal already exists with the requested name: g-foo",
          e.getMessage());
    }
  }

  @Test
  public void testPrincipalNotExists() {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    UserManager userManager = createMock(UserManager.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    Group group = createMock(Group.class);

    try {
      expect(userManager.getAuthorizable("g-foo")).andReturn(null);
      expect(
          userManager.createGroup((Principal) EasyMock.anyObject(), EasyMock
              .matches("0d/f7/03/71/g_foo/"))).andReturn(group);
      expect(session.getUserManager()).andReturn(userManager).times(2);
      expect(group.getID()).andReturn("g-foo").times(2);
      expect(group.isGroup()).andReturn(true);
    } catch (AccessDeniedException e1) {
      e1.printStackTrace();
    } catch (UnsupportedRepositoryOperationException e1) {
      e1.printStackTrace();
    } catch (RepositoryException e1) {
      e1.printStackTrace();
    }

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session).times(2);
    expect(rr.map("/system/userManager/group/g-foo")).andReturn("");
    expect(rr.map("/system/userManager/group")).andReturn("");

    Vector<RequestParameter> parameters = new Vector<RequestParameter>();
    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(
        new HashSet<Entry<String, RequestParameter[]>>());

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr).times(4);
    expect(request.getParameterNames()).andReturn(parameters.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getAttribute("javax.servlet.include.context_path")).andReturn("")
        .times(2);
    expect(request.getParameter(":displayExtension")).andReturn("").times(2);
    expect(request.getResource()).andReturn(null);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});

    HtmlResponse response = new HtmlResponse();

    replay();

    List<Modification> changes = new ArrayList<Modification>();

    try {
      csgs.handleOperation(request, response, changes);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }
}
