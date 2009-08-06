package org.sakaiproject.kernel.user.servlet;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.DelegatedUserAccessControlProvider;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

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
    verify();
  }

  @Test
  public void testNoSession() throws RepositoryException {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(null);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr);

    HtmlResponse response = new HtmlResponse();

    replay();

    csgs.handleOperation(request, response, null);
    assertEquals(403, response.getStatusCode());
    verify();
  }

  @Test
  public void testPrincipalExists() throws RepositoryException {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    Authorizable authorizable = createMock(Authorizable.class);

    User user = createMock(User.class);
    UserManager userManager = createMock(UserManager.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    ResourceResolver rr = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingRepository repository = createMock(SlingRepository.class);

    csgs.repository = repository;

    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr);
    expect(rr.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("admin");
    expect(userManager.getAuthorizable("admin")).andReturn(user);
    expect(user.isAdmin()).andReturn(true);

    expect(repository.loginAdministrative(null)).andReturn(session);
    expect(session.getUserManager()).andReturn(userManager);

    session.logout();
    expectLastCall().anyTimes();

    expect(userManager.getAuthorizable("g-foo")).andReturn(authorizable);

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
    verify();
  }

  @Test
  public void testPrincipalNotExists() throws RepositoryException {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    UserManager userManager = createMock(UserManager.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    Group group = createMock(Group.class);
    User user = createMock(User.class);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    ResourceResolver rr = createMock(ResourceResolver.class);
    SlingRepository repository = createMock(SlingRepository.class);
    ValueFactory valueFactory = createMock(ValueFactory.class);
    Value value = createMock(Value.class);
    csgs.repository = repository;

    expect(request.getResourceResolver()).andReturn(rr);
    expect(rr.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("admin");
    expect(userManager.getAuthorizable("admin")).andReturn(user);
    expect(user.isAdmin()).andReturn(true);

    expect(repository.loginAdministrative(null)).andReturn(session);
    expect(session.getUserManager()).andReturn(userManager);

    session.logout();
    expectLastCall().anyTimes();

    expect(userManager.getAuthorizable("g-foo")).andReturn(null);
    expect(
        userManager.createGroup((Principal) EasyMock.anyObject(), EasyMock
            .matches("0d/f7/03/71/g_foo/"))).andReturn(group);
    expect(session.getUserManager()).andReturn(userManager).times(1);
    expect(group.getID()).andReturn("g-foo").times(2);
    expect(group.isGroup()).andReturn(true);

    expect(rr.adaptTo(Session.class)).andReturn(session);
    expect(rr.map("/system/userManager/group/g-foo")).andReturn("");
    expect(rr.map("/system/userManager/group")).andReturn("");

    Vector<RequestParameter> parameters = new Vector<RequestParameter>();
    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(
        new HashSet<Entry<String, RequestParameter[]>>());

    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getResourceResolver()).andReturn(rr).times(3);
    expect(request.getParameterNames()).andReturn(parameters.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getAttribute("javax.servlet.include.context_path")).andReturn("")
        .times(2);
    expect(request.getParameter(":displayExtension")).andReturn("").times(2);
    expect(request.getResource()).andReturn(null);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});

    expect(user.getID()).andReturn("admin");
    expect(
        group.hasProperty(DelegatedUserAccessControlProvider.ADMIN_PRINCIPALS_PROPERTY))
        .andReturn(false);
    expect(session.getValueFactory()).andReturn(valueFactory);
    Capture<String> valueCapture = new Capture<String>();

    expect(valueFactory.createValue(capture(valueCapture))).andReturn(value);

    Capture<Value[]> valuesCapture = new Capture<Value[]>();
    Capture<String> propertyName = new Capture<String>();
    group.setProperty(capture(propertyName), capture(valuesCapture));
    expectLastCall();

    expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    expectLastCall();

    HtmlResponse response = new HtmlResponse();

    replay();

    List<Modification> changes = new ArrayList<Modification>();

    try {
      csgs.handleOperation(request, response, changes);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    assertTrue(valueCapture.hasCaptured());
    assertTrue(valuesCapture.hasCaptured());
    assertTrue(propertyName.hasCaptured());
    assertEquals("admin", valueCapture.getValue());
    assertEquals(DelegatedUserAccessControlProvider.ADMIN_PRINCIPALS_PROPERTY,
        propertyName.getValue());
    assertEquals(1, valuesCapture.getValue().length);
    verify();
  }
}
