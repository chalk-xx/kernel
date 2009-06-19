package org.sakaiproject.kernel.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

public abstract class AbstractSearchResultProcessor implements SearchResultProcessor {

  public void output(JSONWriter write, QueryResult result, int nitems) throws RepositoryException,
      JSONException {
    NodeIterator resultNodes = result.getNodes();
    for (int i = 0; i < nitems && resultNodes.hasNext(); i++) {
      Node resultNode = resultNodes.nextNode();
      writeNode(write, resultNode);
    }
  }

  protected abstract void writeNode(JSONWriter write, Node resultNode) throws JSONException, RepositoryException;

}
