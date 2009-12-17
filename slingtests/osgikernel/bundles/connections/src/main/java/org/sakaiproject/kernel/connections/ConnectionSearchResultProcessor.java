package org.sakaiproject.kernel.connections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;

/**
 * Formats connection search results
 * 
 * @scr.component immediate="true" label="ConnectionSearchResultProcessor"
 *                description="Formatter for connection search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Connection"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class ConnectionSearchResultProcessor implements SearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionSearchResultProcessor.class);

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    if ( aggregator != null ) {
      aggregator.add(node);
    }

    String targetUser = node.getName();
    write.object();
    write.key("target");
    write.value(targetUser);
    write.key("profile");
    LOGGER.info("Getting info for {} ", targetUser);
    Node profileNode = (Node) node.getSession().getItem(
        PersonalUtils.getProfilePath(targetUser));
    ExtendedJSONWriter.writeNodeToWriter(write, profileNode);
    write.key("details");
    ExtendedJSONWriter.writeNodeToWriter(write, node);
    write.endObject();
  }

}
