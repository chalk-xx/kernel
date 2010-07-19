package org.sakaiproject.nakamura.xythos.servlet;


import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.xythos.XythosRepository;

import java.io.IOException;

import javax.servlet.ServletException;

@SlingServlet(resourceTypes = { "sakai/xythos-members" }, methods = { "POST" }, generateComponent = true, generateService = true)
public class AddMembersServlet extends SlingAllMethodsServlet {
  
  /**
   * 
   */
  private static final long serialVersionUID = -7938660763579949853L;
  @Reference
  private XythosRepository xythos;
  
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException,
      IOException {
      String member = request.getParameter(":member[]");
      String siteId = request.getParameter("siteid");
      
      xythos.toggleMember(siteId, member);
  }

}
