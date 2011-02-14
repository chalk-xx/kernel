package org.sakaiproject.nakamura.api.search.solr;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import java.util.Iterator;

public interface SolrSearchBatchResultProcessor {

  String DEFAULT_BATCH_PROCESSOR_PROP = "sakai.solr.search.processor.batch.default";


  void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException;

  SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SolrSearchException;

}
