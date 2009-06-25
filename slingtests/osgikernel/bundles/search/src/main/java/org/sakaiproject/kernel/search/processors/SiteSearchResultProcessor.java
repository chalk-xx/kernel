package org.sakaiproject.kernel.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Formats user profile node search results
 * 
 * @scr.component immediate="true" label="SiteSearchResultProcessor"
 *                description="Formatter for user search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Site"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class SiteSearchResultProcessor implements SearchResultProcessor {

  /**
   * @scr.reference
   */
  private SiteService siteService;

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteSearchResultProcessor.class);
  
  public void writeNode(JSONWriter write, Node resultNode) throws JSONException,
      RepositoryException {
    if (!siteService.isSite(resultNode)) {
      LOGGER.warn("Search result was not a site node: " + resultNode.getPath());
      throw new JSONException("Unable to write non-site node result");
    }
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(resultNode)));
    write.key("path");
    write.value(resultNode.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, resultNode);
    write.endObject();
  }

  public void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }
  
  public void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

}
