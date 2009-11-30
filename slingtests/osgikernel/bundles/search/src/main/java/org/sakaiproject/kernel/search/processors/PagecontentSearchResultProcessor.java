package org.sakaiproject.kernel.search.processors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component(immediate = true, label = "PagecontentSearchResultProcessor", name = "PagecontentSearchResultProcessor")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Formatter for pagecontent resourcetypes. This will actually dump the data for the above page instead the pagecontent one."),
    @Property(name = "sakai.search.resourcetype", value = "sakai/pagecontent"),
    @Property(name = "sakai.search.processor", value = "Pagecontent") })
@Service(value = SearchResultProcessor.class)
public class PagecontentSearchResultProcessor implements SearchResultProcessor {

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Node node,
      String excerpt) throws JSONException, RepositoryException {
    Node parentNode = node.getParent();
    if (parentNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      String type = parentNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
      if (type.equals("sakai/page")) {
        PageSearchResultProcessor proc = new PageSearchResultProcessor();
        proc.writeNode(request, write, parentNode, excerpt);
        return;
      }
    }

    ExtendedJSONWriter.writeNodeToWriter(write, node);
  }

}
