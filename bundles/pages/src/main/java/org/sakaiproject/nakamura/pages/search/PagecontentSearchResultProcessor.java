package org.sakaiproject.nakamura.pages.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.PathUtils;

/**
 * Formats user profile node search results
 *
 */
@Component(label = "PagecontentSearchResultProcessor", description = "Formatter for pagecontent search results.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "Pagecontent"),
    @Property(name = "sakai.search.resourcetype", value = "sakai/pagecontent")
})
public class PagecontentSearchResultProcessor implements SolrSearchResultProcessor {

  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&(" + SearchConstants.REG_PROCESSOR_NAMES + "=Resource))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  private SolrSearchResultProcessor searchResultProcessor;

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  PagecontentSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory,
      SolrSearchResultProcessor searchResultProcessor) {
    if ( searchServiceFactory == null ) {
      throw new NullPointerException("Search Service Factory Must be set when not using as a component");
    }
    this.searchResultProcessor = searchResultProcessor;
    this.searchServiceFactory = searchServiceFactory;
  }


  public PagecontentSearchResultProcessor() {
  }

  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result)
      throws JSONException {
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));
    try {
      ContentManager cm = session.getContentManager();
      String parentPath = PathUtils.getParentReference(result.getPath());
      Content parentContent = cm.get(parentPath);
      if (parentContent.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
        String type = (String) parentContent.getProperty(SLING_RESOURCE_TYPE_PROPERTY);
        if (type.equals("sakai/page")) {
          searchResultProcessor.writeResult(request, write, result);
          return;
        }
      }
      Content content = cm.get(result.getPath());
      int maxTraversalDepth = SearchUtil.getTraversalDepth(request);
      ExtendedJSONWriter.writeContentTreeToWriter(write, content, maxTraversalDepth);
    } catch (StorageClientException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }
}
