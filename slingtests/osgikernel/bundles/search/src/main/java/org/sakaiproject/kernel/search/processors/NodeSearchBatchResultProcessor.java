package org.sakaiproject.kernel.search.processors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;


@Component(immediate = true, label = "NodeSearchBatchResultProcessor", description = "Formatter for batch search results.")
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "Node") })
@Service(value = SearchBatchResultProcessor.class)
public class NodeSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      RowIterator iterator, long start, long end) throws JSONException,
      RepositoryException {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    iterator.skip(start);

    for (long i = start; i < end && iterator.hasNext(); i++) {
      Row row = iterator.nextRow();
      String path = row.getValue("jcr:path").getString();
      Node node = (Node) session.getItem(path);
      ExtendedJSONWriter.writeNodeToWriter(write, node);
    }

  }

}
