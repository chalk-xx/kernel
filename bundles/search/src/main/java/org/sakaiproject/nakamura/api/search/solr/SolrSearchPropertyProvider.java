package org.sakaiproject.nakamura.api.search.solr;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Map;

public interface SolrSearchPropertyProvider {

  void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap);

}
