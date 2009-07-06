package org.sakaiproject.kernel.user.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.jcr.Session;

public class UpdateSakaiGroupServletTest extends AbstractEasyMockTest {

  @Test
  public void testHandleOperation() throws Exception {
    UpdateSakaiGroupServlet usgs = new UpdateSakaiGroupServlet();

    ArrayList<Modification> changes = new ArrayList<Modification>();

    Group authorizable = createMock(Group.class);
    expect(authorizable.isGroup()).andReturn(true).times(2);
    expect(authorizable.getID()).andReturn("g-foo");

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);

    UserManager userManager = createMock(UserManager.class);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(userManager);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session).times(3);

    Vector<String> params = new Vector<String>();
    HashMap<String, RequestParameter[]> rpm = new HashMap<String, RequestParameter[]>();

    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(rpm.entrySet());

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource).times(2);
    expect(request.getResourceResolver()).andReturn(rr).times(3);
    expect(request.getParameterNames()).andReturn(params.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});

    HtmlResponse response = new HtmlResponse();

    replay();

    usgs.handleOperation(request, response, changes);
  }
}
