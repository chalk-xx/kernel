package org.sakaiproject.nakamura.site.search;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats user profile node search results
 * 
 * @scr.component immediate="true" label="SiteSearchResultProcessor"
 *                description="Formatter for user search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Site"
 * @scr.property name="sakai.seach.resourcetype" value="sakai/site"
 * @scr.service interface="org.sakaiproject.nakamura.api.search.SearchResultProcessor"
 */
public class SiteSearchResultProcessor implements SearchResultProcessor {

  /**
   * @scr.reference
   */
  private SiteService siteService;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SiteSearchResultProcessor.class);

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if (!siteService.isSite(resultNode)) {
      LOGGER.warn("Search result was not a site node: " + resultNode.getPath());
      throw new JSONException("Unable to write non-site node result");
    }
    if (aggregator != null) {
      aggregator.add(resultNode);
    }
    writeNode(write, resultNode);
  }

  public void writeNode(JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(resultNode)));
    write.key("path");
    write.value(resultNode.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, resultNode);
    write.endObject();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return SearchUtil.getSearchResultSet(request, query);
  }

  public void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

}
