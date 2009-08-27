package org.sakaiproject.kernel.search.processors;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="PageSearchPropertyProvider"
 *                description="Formatter for page search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="Page"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 */
public class PageSearchPropertyProvider implements SearchPropertyProvider {

  public static final String PROP_PAGE_TYPE = "sakai:type";

  public static final Logger LOG = LoggerFactory
      .getLogger(PageSearchPropertyProvider.class);

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    LOG.info("loading properties.");
    RequestParameter pathParam = request.getRequestParameter("path");
    RequestParameter[] properties = request.getRequestParameters("properties");
    RequestParameter[] values = request.getRequestParameters("values");
    RequestParameter[] operators = request.getRequestParameters("operators");

    String path = request.getResource().getPath();
    String filter = "";

    if (properties != null && values != null && operators != null
        && properties.length == values.length && values.length == operators.length) {
      for (int i = 0; i < properties.length; i++) {
        String op = operators[i].getString();
        if (op.equals(">") || op.equals("=") || op.equals("<")) {
          filter += " and @" + properties[i].getString() + operators[i].getString() + '"'
              + values[i].getString() + '"';
        }
      }
    }

    if (pathParam != null) {
      path = pathParam.getString();
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    propertiesMap.put("_filter", filter);
    propertiesMap.put("_path", ISO9075.encodePath(path));
  }

}
