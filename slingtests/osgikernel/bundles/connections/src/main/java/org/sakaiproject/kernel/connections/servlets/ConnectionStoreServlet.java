package org.sakaiproject.kernel.connections.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.connections.ConnectionUtils;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.StringUtils;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/contactstore" }, methods = { "GET",
    "POST" })
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for connection stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class ConnectionStoreServlet extends AbstractVirtualPathServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Reference
  protected VirtualResourceProvider virtualResourceProvider;

  @Override
  protected String getTargetPath(Resource baseResource,
      SlingHttpServletRequest request, SlingHttpServletResponse response,
      String realPath, String virtualPath) {
    String path = realPath;
    String user = request.getRemoteUser(); // current user
    if (user == null || UserConstants.ANON_USERID.equals(user)) {
      // cannot proceed if the user is not logged in
      try {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "User must be logged in to access connections");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {

      // example: /_user/contacts/simong/user1.json
      String[] users = StringUtils.split(virtualPath, '/');
      if (users.length == 2) {
        path = ConnectionUtils.getConnectionPath(users[0], users[1], "");
      }
    }
    return path;
  }

  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return virtualResourceProvider;
  }

}
