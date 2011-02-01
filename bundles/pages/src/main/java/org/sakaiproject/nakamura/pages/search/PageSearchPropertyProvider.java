package org.sakaiproject.nakamura.pages.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides properties to process the search
 *
 */
@Component(label = "PageSearchPropertyProvider", description = "Formatter for page search results.")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "Page")})
public class PageSearchPropertyProvider implements SolrSearchPropertyProvider {

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
          filter += " AND " + properties[i].getString() + operators[i].getString() + '"'
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

    propertiesMap.put("_filter", ClientUtils.escapeQueryChars(filter));
    propertiesMap.put("_path", ClientUtils.escapeQueryChars(path));
  }

}
