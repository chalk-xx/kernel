package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Test;
import org.osgi.service.event.Event;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class TestUnjoinServlet extends AbstractSitePostTest {

  @Override
  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteUnJoinServlet servlet = new SiteUnJoinServlet();
    servlet.bindSiteService(siteService);    
    servlet.doPost(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

  @Test
  public void testUnjoinWhenNotTargetNotOnSite() throws RepositoryException, IOException, ServletException {
    goodRequestSetupNoGroups();
    createDummyGroup(TEST_GROUP);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));

    makeRequest();
  }
  
  @Test
  public void testUnjoinWhenNotOnSystem() throws RepositoryException, ServletException, IOException
  {
    goodRequestSetup();
    createDummyGroup(TEST_GROUP);
    setSiteGroups(new String[] {TEST_GROUP});
    expect(session.getUserID()).andReturn(TEST_USERNAME).anyTimes();
    createDummyGroup(TEST_USERNAME);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testUnjoinNonGroupAuthorizable() throws RepositoryException, ServletException, IOException
  {
    goodRequestSetupNoGroups();
    createDummyUser(TEST_GROUP);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }

  @Test
  public void testUnjoin() throws RepositoryException, IOException, ServletException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    Group testGroup = createDummyGroupWithMember(TEST_GROUP, user);
    expect(testGroup.removeMember(eq(user))).andReturn(true);
    eventAdmin.postEvent(isA(Event.class));

    response.sendError(eq(HttpServletResponse.SC_OK));
    makeRequest();
  }
  
  @Test
  public void testUnjoinRemoveFails() throws RepositoryException, IOException, ServletException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    Group testGroup = createDummyGroupWithMember(TEST_GROUP, user);
    expect(testGroup.removeMember(eq(user))).andReturn(false);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));
    makeRequest();
  }
}
