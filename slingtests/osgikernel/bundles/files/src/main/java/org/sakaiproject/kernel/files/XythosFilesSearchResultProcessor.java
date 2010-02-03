package org.sakaiproject.kernel.files;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.XythosUtils;
import org.sakaiproject.kernel.api.search.AbstractSearchResultSet;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchException;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchResultSet;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="XythosFilesSearchResultProcessor"
 *                description="Returns search results from Xythos"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="XythosFiles"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class XythosFilesSearchResultProcessor implements SearchResultProcessor {

  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    RowIterator xythosResults = XythosUtils.searchFiles(query);
    return new AbstractSearchResultSet(xythosResults, (int) xythosResults.getSize());
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
	  Session session = request.getResourceResolver().adaptTo(Session.class);

	    write.array();
	    write.endArray();
  }

}
