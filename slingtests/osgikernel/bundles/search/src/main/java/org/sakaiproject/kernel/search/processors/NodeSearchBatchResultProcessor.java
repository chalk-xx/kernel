package org.sakaiproject.kernel.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Formats node search results for batch queries
 * 
 * @scr.component immediate="true" label="NodeSearchBatchResultProcessor"
 *                description="Formatter for batch search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.batchprocessor" value="Node"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchBatchResultProcessor"
 */
public class NodeSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public void writeNodeIterator(JSONWriter write, NodeIterator nodeIterator, long start,
      long end) throws JSONException, RepositoryException {

    while (nodeIterator.hasNext()) {
      ExtendedJSONWriter.writeNodeToWriter(write, nodeIterator.nextNode());
    }

  }

}
