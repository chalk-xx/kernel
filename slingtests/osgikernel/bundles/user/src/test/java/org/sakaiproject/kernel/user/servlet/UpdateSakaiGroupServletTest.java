package org.sakaiproject.kernel.user.servlet;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.junit.Test;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.jcr.Session;
import javax.jcr.Value;

public class UpdateSakaiGroupServletTest extends AbstractEasyMockTest {

  @Test
  public void testHandleOperation() throws Exception {
    UpdateSakaiGroupServlet usgs = new UpdateSakaiGroupServlet();

    ArrayList<Modification> changes = new ArrayList<Modification>();

    Group authorizable = createMock(Group.class);
    Principal principal = createMock(Principal.class);
    User currentUser = createMock(User.class);
    PrincipalIterator principalIterator = createMock(PrincipalIterator.class);
    Principal gprincipal = createMock(Principal.class);
    Value group = createMock(Value.class);
    Value[] values = new Value[] { group, group };
    SlingRepository slingRepository = createMock(SlingRepository.class);
    usgs.bindRepository(slingRepository);
    JackrabbitSession adminSession = createMock(JackrabbitSession.class);
    
    expect(authorizable.isGroup()).andReturn(true).times(2);
    expect(authorizable.getID()).andReturn("g-foo");

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);

    expect(authorizable.getPrincipal()).andReturn(principal);
    UserManager userManager = createMock(UserManager.class);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(userManager);
    expect(session.getUserID()).andReturn("notadmin");
    expect(userManager.getAuthorizable("notadmin")).andReturn(currentUser);
    expect(currentUser.isAdmin()).andReturn(false);
    expect(currentUser.getPrincipals()).andReturn(principalIterator);
    
    expect(principalIterator.hasNext()).andReturn(true);
    expect(principalIterator.nextPrincipal()).andReturn(gprincipal);
    expect(gprincipal.getName()).andReturn("1");
    expect(principalIterator.hasNext()).andReturn(true);
    expect(principalIterator.nextPrincipal()).andReturn(gprincipal);
    expect(gprincipal.getName()).andReturn("matches2");
    expect(principalIterator.hasNext()).andReturn(false);
    
    expect(authorizable.hasProperty(UserConstants.ADMIN_PRINCIPALS_PROPERTY)).andReturn(true);
    expect(authorizable.getProperty(UserConstants.ADMIN_PRINCIPALS_PROPERTY)).andReturn(values);
    expect(group.getString()).andReturn("notthisgroup");
    expect(group.getString()).andReturn("matches2");
    
    expect(slingRepository.loginAdministrative(null)).andReturn(adminSession);
    expect(adminSession.getUserManager()).andReturn(userManager);
    expect(userManager.getAuthorizable(principal)).andReturn(authorizable);
    
    expect(adminSession.getUserManager()).andReturn(userManager);
    
    expect(adminSession.hasPendingChanges()).andReturn(true);
    adminSession.save();
    expectLastCall();

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session).times(1);
    

    Vector<String> params = new Vector<String>();
    HashMap<String, RequestParameter[]> rpm = new HashMap<String, RequestParameter[]>();

    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(rpm.entrySet());

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource).times(2);
    expect(request.getResourceResolver()).andReturn(rr).times(2);
    expect(request.getParameterNames()).andReturn(params.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});

    adminSession.logout();
    expectLastCall();
    
    HtmlResponse response = new HtmlResponse();

    replay();

    usgs.handleOperation(request, response, changes);
    
    verify();
  }
}
