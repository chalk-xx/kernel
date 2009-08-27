package org.sakaiproject.kernel.discussion.searchresults;

import java.util.Map;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="DiscussionInitialPostPropertyProvider"
 *                description="Formatter for initial posts of discussion search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="DiscussionInitialPost"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 */
public class DiscussionInitialPostPropertyProvider implements SearchPropertyProvider {

  public void loadUserProperties(SlingHttpServletRequest request, Map<String, String> propertiesMap) {
    // Make sure we don't go trough the entire repository..
    String path = request.getResource().getPath();
    
    RequestParameter pathParam = request.getRequestParameter("path");
    if (pathParam != null) {
      path = pathParam.getString();
    }
    
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    
    propertiesMap.put("_path", ISO9075.encodePath(path));
  }

}
