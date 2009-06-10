package org.sakaiproject.kernel.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

public class UserSearchServletTest extends AbstractEasyMockTest {
  private UserSearchServlet userSearchServlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private StringWriter stringWriter;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    userSearchServlet = new UserSearchServlet();
  }

  @Test
  public void testDoGet() throws Exception {
    Node resultNode = createMock(Node.class);
    Session session = prepareSessionWithQueryManagerAndResultNode(resultNode,
        "/jcr:root/rep:security/rep:authorizables/rep:users//element(*,foo:User)");
    SlingRepository slingRepository = createMock(SlingRepository.class);
    expect(slingRepository.loginAdministrative(null)).andReturn(session);
    userSearchServlet.bindSlingRepository(slingRepository);

    request = createMock(SlingHttpServletRequest.class);

    response = createMock(SlingHttpServletResponse.class);
    stringWriter = new StringWriter();
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter));

    replay();

    userSearchServlet.doGet(request, response);

    stringWriter.close();
  }

  @Test
  public void testHandleRepositoryException() throws Exception {

    SlingRepository slingRepository = createMock(SlingRepository.class);
    expect(slingRepository.loginAdministrative(null)).andThrow(
        new RepositoryException());
    userSearchServlet.bindSlingRepository(slingRepository);

    request = createMock(SlingHttpServletRequest.class);

    response = createMock(SlingHttpServletResponse.class);
    stringWriter = new StringWriter();
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter));
    response.sendError(500, null);
    expectLastCall();

    replay();

    userSearchServlet.doGet(request, response);

    stringWriter.close();
  }

  private Session prepareSessionWithQueryManagerAndResultNode(Node resultNode,
      String expectedQuery) throws RepositoryException {

    NodeIterator nodeIterator = createMock(NodeIterator.class);
    if (resultNode == null) {
      expect(nodeIterator.hasNext()).andReturn(false);
    } else {
      expect(nodeIterator.hasNext()).andReturn(true);
      expect(nodeIterator.nextNode()).andReturn(resultNode);
      expect(nodeIterator.hasNext()).andReturn(false);
    }

    QueryResult queryResult = createMock(QueryResult.class);
    expect(queryResult.getNodes()).andReturn(nodeIterator);

    Query query = createMock(Query.class);
    expect(query.execute()).andReturn(queryResult);

    QueryManager queryManager = createMock(QueryManager.class);
    expect(queryManager.createQuery(expectedQuery, Query.XPATH)).andReturn(
        query);

    Workspace workspace = createMock(Workspace.class);
    expect(workspace.getQueryManager()).andReturn(queryManager);

    Session session = createMock(Session.class);
    expect(session.getWorkspace()).andReturn(workspace);
    expect(session.getNamespacePrefix("internal")).andReturn("foo");

    return session;
  }
}
