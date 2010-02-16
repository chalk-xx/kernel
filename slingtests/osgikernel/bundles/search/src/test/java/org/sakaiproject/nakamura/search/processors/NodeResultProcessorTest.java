package org.sakaiproject.nakamura.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.junit.Test;

import javax.jcr.RepositoryException;

public class NodeResultProcessorTest extends AbstractSearchResultProcessorTest {

  @Test
  public void testResultCountLimit() throws RepositoryException, JSONException {
    NodeSearchResultProcessor nodeSearchResultProcessor = new NodeSearchResultProcessor();
    simpleResultCountCheck(nodeSearchResultProcessor);
  }
}
