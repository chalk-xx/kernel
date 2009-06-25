package org.sakaiproject.kernel.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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

  public void writeNode(JSONWriter write, Node resultNode) throws JSONException, RepositoryException {
    ExtendedJSONWriter.writeNodeToWriter(write, resultNode);    
  }

}
