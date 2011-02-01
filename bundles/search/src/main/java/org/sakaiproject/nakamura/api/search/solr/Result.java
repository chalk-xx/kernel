package org.sakaiproject.nakamura.api.search.solr;

import java.util.Collection;
import java.util.Map;

public interface Result {

  String getPath();

  Map<String, Collection<Object>> getProperties();

  Object getFirstValue(String name);

}
