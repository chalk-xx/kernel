package org.sakaiproject.nakamura.api.search.solr;

import org.apache.sling.api.SlingHttpServletRequest;

public interface SolrSearchServiceFactory {

  SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, String query) throws SolrSearchException;

}
