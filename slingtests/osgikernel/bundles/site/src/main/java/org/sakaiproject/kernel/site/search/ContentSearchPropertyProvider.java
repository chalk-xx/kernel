package org.sakaiproject.kernel.site.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.query.Query;

@Component(immediate = true, name = "ContentSearchPropertyProvider", label = "ContentSearchPropertyProvider", description = "Provides general properties for the content search")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides general properties for the content search"),
    @Property(name = "sakai.search.provider", value = "Content")
})
@Service(value = SearchPropertyProvider.class)
public class ContentSearchPropertyProvider implements SearchPropertyProvider {

  private static final String SITE_PARAM = "site";

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    RequestParameter siteParam = request.getRequestParameter(SITE_PARAM);
    if (siteParam != null) {
      String site = "[@id = \"" + escapeString(siteParam.getString(), Query.XPATH) + "\"]";
      propertiesMap.put("_site", site);
    }
  }

  private String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'",
            "\\\\'").replaceAll("'", "''");
      }
    }
    return escaped;
  }
}
