package org.sakaiproject.kernel.site;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Test;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.site.search.SiteSearchResultProcessor;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

public class TestSiteSearchResultProcessor extends AbstractEasyMockTest {

  @Test
  public void testResultCountLimit() throws RepositoryException, JSONException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    expect(siteService.isSite(isA(Item.class))).andReturn(true).anyTimes();
    expect(siteService.getMemberCount(isA(Node.class))).andReturn(20).anyTimes();
    simpleResultCountCheck(siteSearchResultProcessor);
  }

  @Test
  public void testNonSiteNode() throws RepositoryException, JSONException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    expect(siteService.isSite(isA(Item.class))).andReturn(false);
    Node resultNode = createMock(Node.class);
    expect(resultNode.getPath()).andReturn("");
    replay();
    try {
      siteSearchResultProcessor.writeNode(null, null, resultNode, null);
      fail();
    } catch (JSONException e) {
      assertEquals("Unable to write non-site node result", e.getMessage());
    }
  }
  
  protected void simpleResultCountCheck(SearchResultProcessor processor) throws RepositoryException, JSONException
  {
    int itemCount = 12;
    QueryResult queryResult = createMock(QueryResult.class);
    NodeIterator results = createMock(NodeIterator.class);
    expect(queryResult.getNodes()).andReturn(results);
    expect(results.getSize()).andReturn(500L).anyTimes();
    Node dummyNode = createMock(Node.class);
    expect(results.hasNext()).andReturn(true).anyTimes();
    expect(results.nextNode()).andReturn(dummyNode).times(itemCount);
    PropertyIterator propertyIterator = createMock(PropertyIterator.class);
    expect(propertyIterator.hasNext()).andReturn(false).anyTimes();
    expect(dummyNode.getProperties()).andReturn(propertyIterator).anyTimes();
    expect(dummyNode.getPath()).andReturn("/apath").anyTimes();
    replay();
    JSONWriter write = new JSONWriter(new PrintWriter(new ByteArrayOutputStream()));
    write.array();
    NodeIterator resultNodes = queryResult.getNodes();
    int i=0;
    while (resultNodes.hasNext() && i < itemCount) {
      processor.writeNode(null, write, resultNodes.nextNode(), null);
      i++;
    }
    write.endArray();
    verify();
  }
}