package org.sakaiproject.kernel.search.processors;

import static org.easymock.EasyMock.expect;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

public abstract class AbstractSearchResultProcessorTest extends AbstractEasyMockTest {

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
