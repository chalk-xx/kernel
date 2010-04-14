package org.sakaiproject.nakamura.search.processors;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats user profile node search results
 * 
 */
@Component(immediate = true, label = "PagecontentSearchResultProcessor", description = "Formatter for pagecontent search results.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.processor", value = "Pagecontent"),
    @Property(name = "sakai.seach.resourcetype", value = "sakai/pagecontent")
    })
@Service(value = SearchResultProcessor.class)
public class PagecontentSearchResultProcessor implements SearchResultProcessor {

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    Node parentNode = node.getParent();
    if (parentNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      String type = parentNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)
          .getString();
      if (type.equals("sakai/page")) {
        PageSearchResultProcessor proc = new PageSearchResultProcessor();
        proc.writeNode(request, write, aggregator, row);
        return;
      }
    }
    if (aggregator != null) {
      aggregator.add(node);
    }
    ExtendedJSONWriter.writeNodeToWriter(write, node);
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

}
