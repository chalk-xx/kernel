package org.sakaiproject.nakamura.api.search.solr;

import java.util.Iterator;


public interface SolrSearchResultSet {

  Iterator<Result> getResultSetIterator();

  long getSize();

}
