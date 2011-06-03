package org.sakaiproject.nakamura.api.search;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/**
 * Use this interface on a search result processor when you want to add
 * some JSON after the results array, i.e. additional objects
 */
public interface SearchResponseDecorator {

  void decorateSearchResponse(SlingHttpServletRequest request, JSONWriter writer)
    throws JSONException;
}
