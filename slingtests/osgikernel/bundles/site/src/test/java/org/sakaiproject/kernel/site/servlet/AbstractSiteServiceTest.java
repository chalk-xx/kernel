package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.junit.Before;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.site.SiteServiceImpl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

public class AbstractSiteServiceTest extends AbstractEasyMockTest {

  protected EventAdmin eventAdmin;
  protected UserManager userManager;

  protected SiteServiceImpl siteService;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    userManager = createMock(UserManager.class);
    eventAdmin = createMock(EventAdmin.class);
  }

  protected Group createDummyGroup(String groupName) throws RepositoryException {
    Group group = createMock(Group.class);
    registerAuthorizable(group, groupName);
    expect(group.isMember(isA(Authorizable.class))).andReturn(false).anyTimes();
    return group;
  }

  protected Group createDummyGroupWithMember(String groupName, Authorizable member)
      throws RepositoryException {
    List<Authorizable> members = new ArrayList<Authorizable>();
    members.add(member);
    return createDummyGroupWithMembers(groupName, members);
  }

  protected Group createDummyGroupWithMembers(String groupName, List<? extends Authorizable> members)
      throws RepositoryException {
    Group group = createMock(Group.class);
    registerAuthorizable(group, groupName);
    for (Authorizable member : members) {
      expect(group.isMember(eq(member))).andReturn(true).anyTimes();
    }
    expect(group.getDeclaredMembers()).andReturn(members.iterator()).anyTimes();
    return group;
  }

  protected User createDummyUser(String userName) throws RepositoryException {
    List<Group> groups = new ArrayList<Group>();
    return createDummyUserWithGroups(userName, groups);
  }

  protected User createDummyUserWithGroups(String userName, List<Group> groups) throws RepositoryException {
    User user = createMock(User.class);
    registerAuthorizable(user, userName);
    expect(user.getID()).andReturn(userName).anyTimes();
    expect(user.memberOf()).andReturn(groups.iterator()).anyTimes();
    return user;
  }

  private void registerAuthorizable(Authorizable authorizable, String name)
      throws RepositoryException {
    Principal authorizablePrincipal = createMock(Principal.class);
    expect(authorizable.getPrincipal()).andReturn(authorizablePrincipal).anyTimes();
    expect(authorizablePrincipal.getName()).andReturn(name).anyTimes();
    expect(userManager.getAuthorizable(eq(name))).andReturn(authorizable).anyTimes();
  }

  protected void preRequest() {
    replay();
    siteService = new SiteServiceImpl();
    siteService.bindUserManager(userManager);
    siteService.bindEventAdmin(eventAdmin);
  }

  protected void postRequest() {
    siteService.unbindUserManager(userManager);
    siteService.unbindEventAdmin(eventAdmin);
    verify();
  }

}
