package org.sakaiproject.kernel.connections.servlets;

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.CONNECTION_OPERATION;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.connections.ConnectionConstants;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * This handles POST requests for requesting connections
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/contactstore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 */
public class DefaultPostServlet extends AbstractPostConnectionServlet {

  private static final long serialVersionUID = 555L;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String requestPath = request.getPathInfo();
    Resource resource = request.getResource();
    System.out.println("Resource: " + resource);
    int lastSlash = requestPath.lastIndexOf('/');
    if (lastSlash > -1 && lastSlash < requestPath.length() - 1) {
      String tail = requestPath.substring(lastSlash + 1);
      String head = requestPath.substring(0, lastSlash);
      String[] components = tail.split("\\.");
      if (components.length == 3) {
        request.setAttribute(CONNECTION_OPERATION, ConnectionConstants.ConnectionOperations
            .lookup(components[1]));
        request.getRequestDispatcher(request.getResource()).forward(
            new RequestWrapper(request, head, components[0], new ResourceWrapper(head,
                components[0], resource)), response);
        return;
      }
    }
    super.doPost(request, response);
  }

}
