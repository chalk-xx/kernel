package org.sakaiproject.nakamura.files.pool;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionVeto;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@Services(value={
    @Service(value=Servlet.class),
    @Service(value=ServerProtectionVeto.class)
})
@Component(immediate=true, enabled=true, metatype=true)
@SlingServlet(resourceTypes = { "sakai/pooled-content" }, methods = { "GET" }, generateService=false, generateComponent=false)
public class GetPoolStructureServlet extends SlingSafeMethodsServlet implements
    OptingServlet, ServerProtectionVeto {

  /**
   * 
   */
  private static final long serialVersionUID = 6091033560662167157L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(GetPoolStructureServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Content content = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      String resourceId = getResourceId(request, content);
      String resourcePath = StorageClientUtils.newPath(content.getPath(), resourceId);
      if (resourceId != null) {
        Content resourceContent = contentManager.get(resourcePath);
        if (resourceContent != null) {
          if (contentManager.hasBody(resourcePath, null)) {
             StreamHelper streamHelper = new StreamHelper();
             streamHelper.stream(request, contentManager, resourceContent, null, response, resource, getServletContext());
          } else {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
            ExtendedJSONWriter.writeContentTreeToWriter(writer, resourceContent, false, 0);
          }
        } else {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
      }
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(),e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(),e);
    } catch (AccessDeniedException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  public boolean accepts(SlingHttpServletRequest request) {
    Resource resource = request.getResource();
    Content content = resource.adaptTo(Content.class);
    String suffix = request.getRequestPathInfo().getSuffix();
    String structureId = "structure" + StringUtils.split(suffix, "/", 1)[0];
    if (content.hasProperty(structureId)) {
      return content.getProperty(structureId) != null;
    }
    return false;
  }

 
  /**
   * Get the resource ID from the request
   * @param srequest
   * @param content
   * @return
   * @throws JSONException
   */
  private String getResourceId(SlingHttpServletRequest srequest, Content content)
      throws JSONException {
    String suffix = srequest.getRequestPathInfo().getSuffix();
    String[] path = StringUtils.split(suffix, "/");
    if (!content.hasProperty("structure" + path[0])) {
      return null;
    }

    JSONObject structure = new JSONObject((String) content.getProperty("structure"
        + path[0]));
    for (int i = 1; i < path.length; i++) {
      structure = structure.getJSONObject(path[i]);
      if (structure == null) {
        return null;
      }
    }
    return structure.getString("_ref");
  }

  public boolean willVeto(SlingHttpServletRequest srequest) {
    if (srequest != null) {
      Resource resource = srequest.adaptTo(Resource.class);
      if (resource != null
          && (FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceType()) || FilesConstants.POOLED_CONTENT_RT
              .equals(resource.getResourceSuperType()))) {
        return true;
      }
    }
    return false;
  }

  public boolean veto(SlingHttpServletRequest srequest) {
    Resource resource = srequest.adaptTo(Resource.class);
    if (FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceType())
        || FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceSuperType())) {
      try {
        if (FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceType())
            || FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceSuperType())) {

          ContentManager contentManager = resource.adaptTo(ContentManager.class);
          Content content = resource.adaptTo(Content.class);
          String resourceId = getResourceId(srequest, content);
          if (resourceId != null) {
            return !contentManager.hasBody(
                StorageClientUtils.newPath(content.getPath(), resourceId), null);
          }
        }
      } catch (JSONException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (StorageClientException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        LOGGER.warn(e.getMessage(), e);
      }
      return false;
    }
    return true;
  }

}
