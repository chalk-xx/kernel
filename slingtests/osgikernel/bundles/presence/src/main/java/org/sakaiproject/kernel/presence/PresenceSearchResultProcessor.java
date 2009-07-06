package org.sakaiproject.kernel.presence;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.presence.PresenceService;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Formats presence search results
 * 
 * @scr.component immediate="true" label="PresenceSearchResultProcessor"
 *                description="Formatter for presence search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Presence"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 * @scr.reference name="PresenceService"
 *                interface="org.sakaiproject.kernel.api.presence.PresenceService"
 */
public class PresenceSearchResultProcessor implements SearchResultProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PresenceSearchResultProcessor.class);

  protected PresenceService presenceService;

  protected void bindPresenceService(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  protected void unbindPresenceService(PresenceService presenceService) {
    this.presenceService = null;
  }

  public void writeNode(JSONWriter write, Node node) throws JSONException, RepositoryException {
    String targetUser = node.getName();
    write.object();
    write.key("target");
    write.value(targetUser);
    write.key(PresenceService.PRESENCE_STATUS_PROP);
    String status = presenceService.getStatus(targetUser);
    write.value( status );
    write.key("profile");
    LOGGER.info("Getting info for {} ", targetUser);
    Node profileNode = (Node) node.getSession().getItem(PersonalUtils.getProfilePath(targetUser));
    ExtendedJSONWriter.writeNodeToWriter(write, profileNode);
    write.key("details");
    ExtendedJSONWriter.writeNodeToWriter(write, node);
    write.endObject();
  }

}
