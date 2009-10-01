package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Delete multiple resource requests and give a useful response
 * 
 * @scr.component immediate="true" label="BatchDeleteServlet"
 *                description="servlet to delete multiple resources"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Delete multiple resource requests and give a useful response."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/batch/delete"
 * @scr.property name="sling.servlet.methods" value="POST"
 */
public class BatchDeleteServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 6387824420269087079L;
  public static final String RESOURCE_PATH_PARAMETER = "resources";

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String[] requestedResources = request.getParameterValues(RESOURCE_PATH_PARAMETER);
    if (requestedResources == null || requestedResources.length == 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Must specify resources to fetch using the '" + RESOURCE_PATH_PARAMETER
              + "' parameter");
      return;
    }
    Session session = request.getResourceResolver().adaptTo(Session.class);
    ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
    try {
      write.array();
      for (String resourcePath : requestedResources) {
        write.object();
        write.key("path");
        write.value(resourcePath);
        write.key("succes");
        try {
          removeResource(resourcePath, session);
          write.value(200);
        } catch (AccessDeniedException e) {
          write.value(401);
        } catch (PathNotFoundException e) {
          write.value(404);
        } catch (RepositoryException e) {
          write.value(500);
        }
        write.endObject();
      }
      write.endArray();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to parse JSON");
    }
  }

  /**
   * Removes a resource
   * 
   * @param resourcePath
   * @param session
   * @throws RepositoryException
   */
  private void removeResource(String resourcePath, Session session)
      throws AccessDeniedException, PathNotFoundException, RepositoryException {
    try {
      if (!session.itemExists(resourcePath)) {
        throw new PathNotFoundException();
      }
      Item i = session.getItem(resourcePath);
      i.remove();
      session.save();
    } catch (AccessDeniedException e) {
      throw e;
    }
  }

}
