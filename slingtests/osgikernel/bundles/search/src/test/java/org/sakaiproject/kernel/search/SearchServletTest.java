package org.sakaiproject.kernel.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;

public class SearchServletTest extends AbstractEasyMockTest {

  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private SearchServlet searchServlet;
  private StringWriter stringWriter;

  private static final String SQL_QUERY = "select * from \\y where x = '{q}'";

  @Before
  public void setUp() throws Exception {
    super.setUp();
    searchServlet = new SearchServlet();
  }

  @Test
  public void testNoQueryTemplate() throws ValueFormatException,
      RepositoryException, IOException, ServletException {
    Node node = createMock(Node.class);
    expect(node.hasProperty(SAKAI_QUERY_TEMPLATE)).andReturn(false);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(node);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    replay();

    searchServlet.doGet(request, response);

    verify();
  }

  @Test
  public void testGoodQuery() throws ValueFormatException, RepositoryException,
      IOException, ServletException {

    Node resultNode = createMock(Node.class);
    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(
        resultNode, "select * from y where x = 'foo'");

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    addStringPropertyToNode(queryNode, SAKAI_QUERY_LANGUAGE, Query.SQL);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    addStringRequestParameter(request, "items", "25");
    addStringRequestParameter(request, "q", "foo");

    executeQuery(queryNode);
  }

  @Test
  public void testDefaultLanguageAndBadItemCount() throws ValueFormatException,
      RepositoryException, IOException, ServletException {
    executeSimpleQueryWithNoResults("foo", "NAN",
        "select * from y where x = 'foo'");
  }

  @Test
  public void testSqlEscaping() throws RepositoryException, IOException,
      ServletException {
    executeSimpleQueryWithNoResults("fo'o", "NAN",
        "select * from y where x = 'fo\\'o'");
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    Node queryNode = createMock(Node.class);

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    expect(queryNode.hasProperty(SAKAI_QUERY_LANGUAGE)).andThrow(
        new RepositoryException());

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    response.sendError(500, null);
    expectLastCall();

    searchServlet = new SearchServlet();

    replay();

    searchServlet.doGet(request, response);
    
    verify();
  }

  private void executeSimpleQueryWithNoResults(String queryParameter,
      String itemCount, String expectedSqlQuery) throws RepositoryException,
      IOException, ServletException {
    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(null,
        expectedSqlQuery);

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    expect(queryNode.hasProperty(SAKAI_QUERY_LANGUAGE)).andReturn(false);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    addStringRequestParameter(request, "items", itemCount);
    addStringRequestParameter(request, "q", queryParameter);

    executeQuery(queryNode);
  }

  private Node prepareNodeSessionWithQueryManagerAndResultNode(Node resultNode,
      String expectedQuery) throws RepositoryException {
    Node queryNode = createMock(Node.class);

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
    expect(queryManager.createQuery(expectedQuery, Query.SQL)).andReturn(query);

    Workspace workspace = createMock(Workspace.class);
    expect(workspace.getQueryManager()).andReturn(queryManager);

    Session session = createMock(Session.class);
    expect(session.getWorkspace()).andReturn(workspace);

    expect(queryNode.getSession()).andReturn(session);

    return queryNode;
  }

  private void executeQuery(Node queryNode) throws IOException,
      ServletException, RepositoryException {
    stringWriter = new StringWriter();
    response = createMock(SlingHttpServletResponse.class);
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter));
    searchServlet = new SearchServlet();
    expect(queryNode.hasProperty(SAKAI_RESULTPROCESSOR)).andReturn(false)
        .anyTimes();

    replay();

    searchServlet.doGet(request, response);
    stringWriter.close();

    verify();
  }

}
