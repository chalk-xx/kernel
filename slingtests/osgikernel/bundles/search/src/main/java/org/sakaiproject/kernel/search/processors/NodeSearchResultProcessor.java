package org.sakaiproject.kernel.search.processors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;

/**
 * Formats user profile node search results
 * 
 * @scr.component immediate="true" label="NodeSearchResultProcessor"
 *                description="Formatter for user search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Node"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class NodeSearchResultProcessor implements SearchResultProcessor {

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    if ( aggregator != null ) {
      aggregator.add(node);
    }
    ExtendedJSONWriter.writeNodeToWriter(write, node);
  }

}
