package org.sakaiproject.kernel.search;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchServletT {

  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private SearchServlet searchServlet;
  private StringWriter stringWriter;

  private static final String SAKAI_QUERY_TEMPLATE = "sakai:query-template";
  private static final String SAKAI_QUERY_LANGUAGE = "sakai:query-language";
  private static final String SQL_QUERY = "select * from \\y where x = {q}";

  @Before
  public void setUp() throws Exception {
    Property property = createMock(Property.class);
    expect(property.getString()).andReturn(SQL_QUERY);
    expect(property.getString()).andReturn(Query.SQL);
    expect(property.getString()).andReturn(SQL_QUERY);

    Node resultNode = createMock(Node.class);

    NodeIterator nodeIterator = createMock(NodeIterator.class);
    expect(nodeIterator.hasNext()).andReturn(true);
    expect(nodeIterator.hasNext()).andReturn(false).times(2);
    expect(nodeIterator.nextNode()).andReturn(resultNode);

    QueryResult queryResult = createMock(QueryResult.class);
    expect(queryResult.getNodes()).andReturn(nodeIterator).times(2);

    Query query = createMock(Query.class);
    expect(query.execute()).andReturn(queryResult).times(2);

    QueryManager queryManager = createMock(QueryManager.class);
    expect(queryManager.createQuery("select * from y where x = foo", Query.SQL))
        .andReturn(query).times(2);

    Workspace workspace = createMock(Workspace.class);
    expect(workspace.getQueryManager()).andReturn(queryManager).times(2);

    Session session = createMock(Session.class);
    expect(session.getWorkspace()).andReturn(workspace).times(2);

    Node node = createMock(Node.class);
    expect(node.hasProperty(SAKAI_QUERY_TEMPLATE)).andReturn(false);
    expect(node.hasProperty(SAKAI_QUERY_TEMPLATE)).andReturn(true).times(2);
    expect(node.hasProperty(SAKAI_QUERY_LANGUAGE)).andReturn(true);
    expect(node.hasProperty(SAKAI_QUERY_LANGUAGE)).andReturn(false);
    expect(node.getProperty(SAKAI_QUERY_TEMPLATE)).andReturn(property).times(2);
    expect(node.getProperty(SAKAI_QUERY_LANGUAGE)).andReturn(property);
    expect(node.getSession()).andReturn(session).times(2);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(node).times(3);

    RequestParameter requestParam = createMock(RequestParameter.class);
    expect(requestParam.getString()).andReturn("25");
    expect(requestParam.getString()).andReturn("foo");
    expect(requestParam.getString()).andReturn("NAN").times(2);
    expect(requestParam.getString()).andReturn("foo");

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource).times(3);
    expect(request.getRequestParameter("items")).andReturn(requestParam).times(
        2);
    expect(request.getRequestParameter("q")).andReturn(requestParam).times(2);

    stringWriter = new StringWriter();

    response = createMock(SlingHttpServletResponse.class);
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter))
        .times(2);

    searchServlet = new SearchServlet();
    replay(property, resultNode, nodeIterator, queryResult, query,
        queryManager, workspace, session, node, resource, requestParam,
        request, response);
  }

  @Test
  public void testDoGet() throws Exception {
    searchServlet.doGet(request, response);
    searchServlet.doGet(request, response);
    searchServlet.doGet(request, response);
  }

  @After
  public void tearDown() throws Exception {
    stringWriter.close();
    verify();
  }
}
