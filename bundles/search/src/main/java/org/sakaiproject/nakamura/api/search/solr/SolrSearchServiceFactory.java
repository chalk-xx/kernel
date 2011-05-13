package org.sakaiproject.nakamura.api.search.solr;

import org.apache.sling.api.SlingHttpServletRequest;

public interface SolrSearchServiceFactory {

  SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query,
      boolean asAnon) throws SolrSearchException;

  SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SolrSearchException;
}
