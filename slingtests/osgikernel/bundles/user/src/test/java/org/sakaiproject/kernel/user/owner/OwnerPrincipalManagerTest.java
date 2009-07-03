package org.sakaiproject.kernel.user.owner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sakaiproject.kernel.api.user.UserConstants.JCR_CREATED_BY;

import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class OwnerPrincipalManagerTest extends AbstractEasyMockTest {

  @Test
  public void testOwnerNoCreatorProp() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(false);
    expect(contextNode.getPath()).andReturn("");

    Node aclNode = createMock(Node.class);
    expect(aclNode.getParent()).andReturn(contextNode);

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode));
  }

  @Test
  public void testOwnerCreator() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Property owner = createMock(Property.class);
    expect(owner.getString()).andReturn("foo");

    Session session = createMock(Session.class);
    expect(session.getUserID()).andReturn("foo");

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(true);
    expect(contextNode.getProperty(JCR_CREATED_BY)).andReturn(owner);
    expect(contextNode.getPath()).andReturn("");
    expect(contextNode.getSession()).andReturn(session);

    Node aclNode = createMock(Node.class);
    expect(aclNode.getParent()).andReturn(contextNode);

    replay();
    assertTrue(opm.hasPrincipalInContext("owner", aclNode));
  }

  @Test
  public void testOwnerNotCreator() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Property owner = createMock(Property.class);
    expect(owner.getString()).andReturn("foo");

    Session session = createMock(Session.class);
    expect(session.getUserID()).andReturn("bar");

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(true);
    expect(contextNode.getProperty(JCR_CREATED_BY)).andReturn(owner);
    expect(contextNode.getPath()).andReturn("");
    expect(contextNode.getSession()).andReturn(session);

    Node aclNode = createMock(Node.class);
    expect(aclNode.getParent()).andReturn(contextNode);

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode));
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andThrow(new RepositoryException());

    Node aclNode = createMock(Node.class);
    expect(aclNode.getParent()).andReturn(contextNode);

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode));
  }
}
