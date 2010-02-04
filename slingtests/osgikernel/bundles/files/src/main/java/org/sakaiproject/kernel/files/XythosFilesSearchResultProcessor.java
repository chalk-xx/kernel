package org.sakaiproject.kernel.files;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.XythosUtils;
import org.sakaiproject.kernel.api.search.AbstractSearchResultSet;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchException;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchResultSet;

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
	  Session session = request.getResourceResolver().adaptTo(Session.class);
    RowIterator xythosResults = XythosUtils.searchFiles(query, session.getUserID());
    return new AbstractSearchResultSet(xythosResults, (int) xythosResults.getSize());
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
		  Aggregator aggregator, Row row) throws JSONException, RepositoryException {
	  Session session = request.getResourceResolver().adaptTo(Session.class);
	  
	  String remotePath = row.getValue("jcr:path").getString();
	  String[] pathStems = remotePath.split("/");
	  if (pathStems.length > 2 && pathStems[2].equals("trash")) {
		  // we simply ignore files that are in the Xythos trash
		  return;
	  }
	  String fileName = remotePath.substring(remotePath.lastIndexOf("/")+1);
	  
	  String mimeType = request.getSession().getServletContext().getMimeType(fileName);
	  
	  
	  write.object();
	  
	  write.key("sakai:filename");
	  write.value(fileName);
	  
	  write.key("sakai:remoteurl");
	  write.value("http://localhost:9090" + remotePath);
	  
	  write.key("sling:resourceType");
	  write.value("sakai/file");
	  
	  write.key("sakai:mimeType");
	  write.value(mimeType);
	  
	  write.key("sakai:user");
	  write.value(session.getUserID());
	  
	  write.endObject();
  }

}
