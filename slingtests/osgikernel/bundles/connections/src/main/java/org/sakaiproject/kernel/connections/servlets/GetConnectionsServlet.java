package org.sakaiproject.kernel.connections.servlets;

import static org.sakaiproject.kernel.api.search.SearchConstants.JSON_RESULTS;
import static org.sakaiproject.kernel.api.search.SearchConstants.TOTAL;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This handles the GET requests to list connections
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/contactstore"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 */
public class GetConnectionsServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 444L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GetConnectionsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String requesterUserId = request.getRemoteUser();
    Node contactNode = request.getResource().adaptTo(Node.class);
    try {
      QueryManager queryManager = contactNode.getSession().getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("//*[@sling:resourceType=\"sakai/contactusercontact\" and @jcr:createdBy=\"" + requesterUserId + "\"]", Query.XPATH);
      QueryResult result = query.execute();
  
      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.object();
      NodeIterator resultNodes = result.getNodes();
      write.key(TOTAL);
      long total = resultNodes.getSize();
      write.value(total);
      write.key(JSON_RESULTS);
      write.array();
      output(write, resultNodes);
      write.endArray();
      write.endObject();
      return;
    } catch (RepositoryException e) {
      LOGGER.error("Repository exception looking up contacts {}", e);
    } catch (JSONException e) {
      LOGGER.error("Repository exception rendering contact nodes {}", e);
    }
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to render contacts");
  }

  private void output(ExtendedJSONWriter write, NodeIterator resultNodes) throws JSONException, RepositoryException {
    while (resultNodes.hasNext()) {
      write.node(resultNodes.nextNode());
    }
  }

}
