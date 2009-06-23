package org.sakaiproject.kernel.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public abstract class AbstractSearchResultProcessor implements SearchResultProcessor {

  public void output(JSONWriter write, NodeIterator resultNodes, long start, long end) throws RepositoryException,
      JSONException {
    resultNodes.skip(start);
    for (long i = start; i < end && resultNodes.hasNext(); i++) {
      Node resultNode = resultNodes.nextNode();
      writeNode(write, resultNode);
    }
  }

  protected abstract void writeNode(JSONWriter write, Node resultNode) throws JSONException, RepositoryException;

}
