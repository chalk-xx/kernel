package org.sakaiproject.kernel.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.sakaiproject.kernel.api.search.SearchConstants.PARAMS_PAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_BATCHRESULTPROCESSOR;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_PROPERTY_PROVIDER;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
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

    Session session = createMock(Session.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    
    response = createMock(SlingHttpServletResponse.class);
    replay();

    searchServlet.doGet(request, response);

    verify();
  }

  @Test
  public void testGoodQuery() throws ValueFormatException, RepositoryException,
      IOException, ServletException {

    Row row = createMock(Row.class);
    Value val = createMock(Value.class);
    expect(val.getString()).andReturn("/foo/bar");
    expect(row.getValue("jcr:path")).andReturn(val);
    
    
    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(
        row, "select * from y where x = 'foo'");

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    addStringPropertyToNode(queryNode, SAKAI_QUERY_LANGUAGE, Query.SQL);
    expect(queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_BATCHRESULTPROCESSOR)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("bob");
    expect(request.getResource()).andReturn(resource);
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null);
    addStringRequestParameter(request, "items", "25");
    addStringRequestParameter(request, "q", "foo");
    
    Session session = createMock(Session.class);
    Node resultNode = createMock(Node.class);
    PropertyIterator propIterator = createMock(PropertyIterator.class);
    expect(propIterator.hasNext()).andReturn(false);
    expect(resultNode.getProperties()).andReturn(propIterator);
    
    expect(session.getItem("/foo/bar")).andReturn(resultNode);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);

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
        "select * from y where x = 'fo\\''o'");
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

    Session session = createMock(Session.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    
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
    expect(queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_BATCHRESULTPROCESSOR)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getRemoteUser()).andReturn("bob").anyTimes();
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null);
    addStringRequestParameter(request, "items", itemCount);
    addStringRequestParameter(request, "q", queryParameter);
    

    Session session = createMock(Session.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);

    executeQuery(queryNode);
  }

  private Node prepareNodeSessionWithQueryManagerAndResultNode(Row resultRow,
      String expectedQuery) throws RepositoryException {
    Node queryNode = createMock(Node.class);

    RowIterator iterator = createMock(RowIterator.class);
    if (resultRow == null) {
      expect(iterator.hasNext()).andReturn(false);
    } else {
      expect(iterator.hasNext()).andReturn(true);
      expect(iterator.nextRow()).andReturn(resultRow);
      expect(iterator.hasNext()).andReturn(false);
    }
    iterator.skip(0);
    expect(iterator.getSize()).andReturn(500L).anyTimes();

    QueryResult queryResult = createMock(QueryResult.class);
    expect(queryResult.getRows()).andReturn(iterator);
    
    expect(queryResult.getColumnNames()).andReturn(new String[] {});

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
