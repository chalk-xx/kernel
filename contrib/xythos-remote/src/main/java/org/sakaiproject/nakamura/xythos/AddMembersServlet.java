package org.sakaiproject.nakamura.xythos;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import edu.nyu.XythosRemote;

@SlingServlet(resourceTypes = { "sakai/xythos-members" }, methods = { "POST" }, generateComponent = true, generateService = true)
public class AddMembersServlet extends SlingAllMethodsServlet {
  
  /**
   * 
   */
  private static final long serialVersionUID = -7938660763579949853L;
  @Reference
  private XythosRemote xythos;
  
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException,
      IOException {
      String member = request.getParameter(":member");
      String siteId = request.getParameter("siteid");
      siteId = siteId.replaceAll("\\/sites\\/", "\\/alex3\\/");
      
      xythos.toggleMember(siteId, member);
  }

}
