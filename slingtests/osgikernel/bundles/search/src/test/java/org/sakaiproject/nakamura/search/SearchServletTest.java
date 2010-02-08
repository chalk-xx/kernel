package org.sakaiproject.nakamura.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_PAGE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_AGGREGATE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_BATCHRESULTPROCESSOR;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_PROPERTY_PROVIDER;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_LIMIT_RESULTS;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.search.AbstractSearchResultSet;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
  private SearchResultProcessor proc;

  private static final String SQL_QUERY = "select * from \\y where x = '{q}'";

  @Before
  public void setUp() throws Exception {
    super.setUp();
    searchServlet = new SearchServlet();
    searchServlet.init();
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"DLS_DEAD_LOCAL_STORE"})
  public void testGoodQuery() throws ValueFormatException, RepositoryException,
      IOException, ServletException {

    Row row = createMock(Row.class);
    
    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(
        row, "select * from y where x = 'foo'");

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    addStringPropertyToNode(queryNode, SAKAI_QUERY_LANGUAGE, Query.SQL);
    expect(queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_BATCHRESULTPROCESSOR)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_AGGREGATE)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_LIMIT_RESULTS)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("bob");
    expect(request.getResource()).andReturn(resource);
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null).anyTimes();
    addStringRequestParameter(request, "items", "25");
    addStringRequestParameter(request, "q", "foo");
    
    @SuppressWarnings("unused")
    Session session = createMock(Session.class);
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
    expect(queryNode.hasProperty(SAKAI_AGGREGATE)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_LIMIT_RESULTS)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getRemoteUser()).andReturn("bob").anyTimes();
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null).anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    addStringRequestParameter(request, "items", itemCount);
    addStringRequestParameter(request, "q", queryParameter);

    executeQuery(queryNode);
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"DLS_DEAD_LOCAL_STORE"})
  private Node prepareNodeSessionWithQueryManagerAndResultNode(Row resultRow,
      String expectedQuery) throws RepositoryException {
    Node queryNode = createMock(Node.class);

    final RowIterator iterator = createMock(RowIterator.class);
    if (resultRow == null) {
      expect(iterator.hasNext()).andReturn(false);
    } else {
      expect(iterator.hasNext()).andReturn(true);
      expect(iterator.nextRow()).andReturn(resultRow);
      expect(iterator.hasNext()).andReturn(false);
    }
    @SuppressWarnings("unused")
    QueryResult queryResult = createMock(QueryResult.class);
    //expect(queryResult.getRows()).andReturn(iterator);

    proc = new SearchResultProcessor() {
      public void writeNode(SlingHttpServletRequest request, JSONWriter write,
          Aggregator aggregator, Row row) throws JSONException, RepositoryException {
        // TODO Auto-generated method stub
      }

      public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
          Query query) throws SearchException {
        return new AbstractSearchResultSet(iterator, 0);
      }
    };

    Query query = createMock(Query.class);
    // expect(query.execute()).andReturn(queryResult);

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
    response.setHeader("Content-Type", "application/json");
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter));
    searchServlet = new SearchServlet();
    searchServlet.defaultSearchProcessor = proc;
    expect(queryNode.hasProperty(SAKAI_RESULTPROCESSOR)).andReturn(false)
        .anyTimes();

    replay();

    searchServlet.doGet(request, response);
    stringWriter.close();

    verify();
  }
}
