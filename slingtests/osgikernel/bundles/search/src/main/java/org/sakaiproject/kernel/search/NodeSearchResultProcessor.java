package org.sakaiproject.kernel.search;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

/**
 * Formats user profile node search results
 * 
 * @scr.component immediate="true" label="NodeSearchResultProcessor"
 *                description="Formatter for user search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Node"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class NodeSearchResultProcessor implements SearchResultProcessor {

  public void output(JSONWriter write, QueryResult result, int nitems) throws RepositoryException,
      JSONException {
    NodeIterator resultNodes = result.getNodes();
    while (resultNodes.hasNext()) {
      Node resultNode = resultNodes.nextNode();
      ExtendedJSONWriter.writeNodeToWriter(write, resultNode);
    }
  }

}
