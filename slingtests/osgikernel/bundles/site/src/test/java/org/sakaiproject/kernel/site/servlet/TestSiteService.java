package org.sakaiproject.kernel.site.servlet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.site.SiteServiceImpl;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class TestSiteService extends AbstractEasyMockTest {

  @Test
  public void testNoDuplicateMembers() throws RepositoryException {
    UserManager userManager = createMock(UserManager.class);
    SlingRepository slingRepository = createMock(SlingRepository.class);
    SiteServiceImpl siteService = new SiteServiceImpl();
    siteService.bindSlingRepository(slingRepository);
    Node siteNode = createMock(Node.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    expect(siteNode.getSession()).andReturn(session);
    expect(session.getUserManager()).andReturn(userManager);
    addPropertyToNode(siteNode, SiteService.AUTHORIZABLE, new Value[] { new MockValue("group1"),
        new MockValue("group2") });

    Group group1 = createMock(Group.class);
    Group group2 = createMock(Group.class);
    expect(userManager.getAuthorizable("group1")).andReturn(group1);
    expect(userManager.getAuthorizable("group2")).andReturn(group2);
    expect(group1.getDeclaredMembers()).andReturn(createUserIterator("bob"));
    expect(group1.getID()).andReturn("group1").anyTimes();
    expect(group2.getDeclaredMembers()).andReturn(createUserIterator("bob"));
    expect(group2.getID()).andReturn("group2").anyTimes();

    replay();
    Iterator<User> members = siteService.getMembers(siteNode, 0, 3, null);
    Set<String> userNames = new HashSet<String>();
    while (members.hasNext()) {
      User result = members.next();
      assertFalse("Expected users to be unique - already have user: " + result.getID(), userNames
          .contains(result.getID()));
      userNames.add(result.getID());
    }
    assertEquals("Expected one user back", 1, userNames.size());
    verify();
  }

  private Iterator<Authorizable> createUserIterator(String userName) throws RepositoryException {
    final User mockUser = createMock(User.class);
    expect(mockUser.getID()).andReturn(userName).anyTimes();
    return new Iterator<Authorizable>() {

      boolean had = false;

      public boolean hasNext() {
        return !had;
      }

      public Authorizable next() {
        had = true;
        return mockUser;
      }

      public void remove() {
      }

    };
  }
}
