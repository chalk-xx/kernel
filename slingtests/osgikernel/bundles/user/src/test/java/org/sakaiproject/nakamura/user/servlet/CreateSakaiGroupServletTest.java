package org.sakaiproject.nakamura.user.servlet;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

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
    expect(request.getRemoteUser()).andReturn(SecurityConstants.ADMIN_ID);  

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
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn(SecurityConstants.ANONYMOUS_ID);  
    
    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(null);

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
    expect(request.getRemoteUser()).andReturn(SecurityConstants.ADMIN_ID);  

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
    }
    verify();
  }

  @Test
  public void testPrincipalNotExists() throws Exception {
    CreateSakaiGroupServlet csgs = new CreateSakaiGroupServlet();

    UserManager userManager = createMock(UserManager.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    Group group = createMock(Group.class);
    User user = createMock(User.class);
    ItemBasedPrincipal principal = createNiceMock(ItemBasedPrincipal.class);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    ResourceResolver rr = createMock(ResourceResolver.class);
    SlingRepository repository = createMock(SlingRepository.class);
    ValueFactory valueFactory = createMock(ValueFactory.class);
    Value value = createMock(Value.class);
    csgs.repository = repository;
    expect(request.getRemoteUser()).andReturn(SecurityConstants.ADMIN_ID);  

    expect(request.getResourceResolver()).andReturn(rr).anyTimes();
    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();
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
        userManager.createGroup((Principal) EasyMock.anyObject())).andReturn(group);
    expect(session.getUserManager()).andReturn(userManager).times(1);
    expect(group.getID()).andReturn("g-foo").times(2);
    expect(group.isGroup()).andReturn(true);
    expect(group.getPrincipal()).andReturn(principal);
    expect(principal.getPath()).andReturn("/rep:security/rep:authorizables/rep:groups/group/path");
    expect(session.getValueFactory()).andReturn(valueFactory);
    expect(valueFactory.createValue("/group/path")).andReturn(value);
    group.setProperty("path", value);
    expectLastCall();

    expect(rr.map("/system/userManager/group/g-foo")).andReturn("");
    expect(rr.map("/system/userManager/group")).andReturn("");

    Vector<RequestParameter> parameters = new Vector<RequestParameter>();
    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(
        new HashSet<Entry<String, RequestParameter[]>>());

    expect(request.getParameter(SlingPostConstants.RP_NODE_NAME)).andReturn("g-foo");
    expect(request.getParameterNames()).andReturn(parameters.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getAttribute("javax.servlet.include.context_path")).andReturn("")
        .times(2);
    expect(request.getParameter(":displayExtension")).andReturn("").times(2);
    expect(request.getResource()).andReturn(null);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});
    

    expect(user.getID()).andReturn("admin");
    // might need to adjust here
    expect(group.hasProperty(UserConstants.PROP_GROUP_MANAGERS)).andReturn(false);
    expect(session.getValueFactory()).andReturn(valueFactory);
    Capture<String> valueCapture = new Capture<String>();

    expect(valueFactory.createValue(capture(valueCapture))).andReturn(value);

    Capture<Value[]> valuesCapture = new Capture<Value[]>();
    Capture<String> propertyName = new Capture<String>();
    group.setProperty(capture(propertyName), capture(valuesCapture));
    expectLastCall();
    
    List<Modification> changes = new ArrayList<Modification>();


    expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    expectLastCall();

    AuthorizablePostProcessService authorizablePostProcessService = createMock(AuthorizablePostProcessService.class);
    authorizablePostProcessService.process((Authorizable)EasyMock.anyObject(), (Session)EasyMock.anyObject(), (Modification)EasyMock.anyObject());
    expectLastCall();

    HtmlResponse response = new HtmlResponse();

    replay();

    csgs.postProcessorService = authorizablePostProcessService;

    try {
      csgs.handleOperation(request, response, changes);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    assertTrue(valueCapture.hasCaptured());
    assertTrue(valuesCapture.hasCaptured());
    assertTrue(propertyName.hasCaptured());
    assertEquals("admin", valueCapture.getValue());
    assertEquals(UserConstants.PROP_GROUP_MANAGERS,
        propertyName.getValue());
    assertEquals(1, valuesCapture.getValue().length);
    verify();
  }
}
