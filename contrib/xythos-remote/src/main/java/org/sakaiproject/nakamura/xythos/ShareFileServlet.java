package org.sakaiproject.nakamura.xythos;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import edu.nyu.XythosRemote;

@SlingServlet(resourceTypes = { "sakai/xythos-share" }, methods = { "POST" }, generateComponent = true, generateService = true)
public class ShareFileServlet extends SlingAllMethodsServlet {
  
  /**
   * 
   */
  private static final long serialVersionUID = 3794459685138851653L;
  @Reference
  private XythosRemote xythos;
  
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException,
      IOException {
    try {
      String currentUserId = request.getResource().adaptTo(Node.class).getSession().getUserID();
      String resource = request.getParameter("resource");
      String groupId = request.getParameter("groupid");
      
      boolean success = xythos.shareFileWithGroup(groupId, resource, currentUserId);
      String status = success ? "success" : "failed";
      response.setContentType("application/json");
      PrintWriter out = response.getWriter();
      out.println("{\"status\":\""+status+"\"}");
      out.flush();
    } catch (RepositoryException e) {
      response.sendError(500, e.getMessage());
    }
  }

}
