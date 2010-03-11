package org.sakaiproject.nakamura.casauth;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;

public class CasAuthenticationTest {

  @Test
  public void testAuthenticate() throws RepositoryException {
    SimpleCredentials sc = new SimpleCredentials("foo", new char[0]);
    Principal principal = new CasLoginModulePlugin().getPrincipal(sc);

    Authorizable authorizable = createMock(Authorizable.class);

    UserManager um = createMock(UserManager.class);
    expect(um.getAuthorizable("foo")).andReturn(authorizable);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    session.logout();
    expectLastCall();
    expect(session.getUserManager()).andReturn(um);

    SlingRepository repository = createMock(SlingRepository.class);
    expect(repository.loginAdministrative(null)).andReturn(session);

    replay(authorizable, um, session, repository);

    CasAuthentication ca = new CasAuthentication(principal, repository);
    assertTrue(ca.authenticate(sc));
  }

  @Test
  public void testAuthenticateNewUser() throws RepositoryException {
    SimpleCredentials sc = new SimpleCredentials("foo", new char[0]);
    Principal principal = new CasLoginModulePlugin().getPrincipal(sc);

    UserManager um = createMock(UserManager.class);
    expect(um.getAuthorizable("foo")).andReturn(null);
    expect(
        um.createUser(eq("foo"), isA(String.class), isA(Principal.class), eq(PathUtils
            .getUserPrefix("foo", UserConstants.DEFAULT_HASH_LEVELS)))).andReturn(null);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    session.logout();
    expectLastCall();
    expect(session.getUserManager()).andReturn(um);

    SlingRepository repository = createMock(SlingRepository.class);
    expect(repository.loginAdministrative(null)).andReturn(session);

    replay(um, session, repository);

    CasAuthentication ca = new CasAuthentication(principal, repository);
    assertTrue(ca.authenticate(sc));
  }
}
