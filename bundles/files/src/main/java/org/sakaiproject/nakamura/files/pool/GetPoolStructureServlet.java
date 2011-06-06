package org.sakaiproject.nakamura.files.pool;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@Services(value={
    @Service(value=ServerProtectionVeto.class),
    @Service(value=DefaultServletDelegate.class)
})
@Component(immediate=true, enabled=true, metatype=true)
public class GetPoolStructureServlet extends SlingSafeMethodsServlet implements
    DefaultServletDelegate, ServerProtectionVeto {

  /**
   * 
   */
  private static final long serialVersionUID = 6091033560662167157L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(GetPoolStructureServlet.class);


  public void doDelegateGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Content content = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      String resourceId = getResourceId(request, content);
      if (resourceId != null) {
        String resourcePath = StorageClientUtils.newPath(content.getPath(), resourceId);
        Content resourceContent = contentManager.get(resourcePath);
        if (resourceContent != null) {
          if (contentManager.hasBody(resourcePath, null)) {
            LOGGER.debug("Getting Resource Path {} Has Body", resourcePath);
             StreamHelper streamHelper = new StreamHelper();
             streamHelper.stream(request, contentManager, resourceContent, null, response, resource, getServletContext());
          } else {
            LOGGER.debug("Getting Resource Path {} No Body", resourcePath);
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
    if (request != null) {
      Resource resource = request.getResource();
      if (resource != null) {
        Content content = resource.adaptTo(Content.class);
        if (content != null) {
          String contentPathInfo = getContentPathInfo(request, content);
          LOGGER.debug("Content Path Info is {} ", contentPathInfo);
          if ( contentPathInfo != null && contentPathInfo.length() > 0 ) {
            String structureId = FilesConstants.STRUCTURE_FIELD_STEM + StringUtils.split(contentPathInfo, "/", 2)[0];
            if (content.hasProperty(structureId)) {
                return content.getProperty(structureId) != null;
            }
          }
        }
      }
    }
    return false;
  }

 
  private String getContentPathInfo(SlingHttpServletRequest request, Content content) {
    RequestPathInfo requestPathInfo = request.getRequestPathInfo();
    LOGGER.debug("RequestPath Info is {} {}", requestPathInfo);
    String requestPath = requestPathInfo.getResourcePath();
    String contentPath  = content.getPath();
    LOGGER.debug("RequestPath {}  Content Path is {}", requestPath, contentPath);
    int i = requestPath.indexOf(contentPath);
    String contentPathInfo = "";
    if ( i > 0  ) {
      contentPathInfo = requestPath.substring(i+contentPath.length());
    }
    return contentPathInfo;
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
    String contentPathInfo = getContentPathInfo(srequest, content);
    if ( contentPathInfo == null || contentPathInfo.length() == 0 ) {
      return null;
    }
    String[] path = StringUtils.split(contentPathInfo, "/");
    if (!content.hasProperty(FilesConstants.STRUCTURE_FIELD_STEM + path[0])) {
      return null;
    }

    JSONObject structure = new JSONObject((String) content.getProperty(FilesConstants.STRUCTURE_FIELD_STEM
        + path[0]));
    for (int i = 1; i < path.length; i++) {
      structure = structure.getJSONObject(path[i]);
      LOGGER.debug("Got {} at {} ",structure,path[i]);
      if (structure == null) {
        return null;
      }
    }
    if ( structure.has(FilesConstants.RESOURCE_REFERENCE_FIELD)) {
      return structure.getString(FilesConstants.RESOURCE_REFERENCE_FIELD);
    } else {
      return null;
    }
  }

  public boolean willVeto(SlingHttpServletRequest srequest) {
    if (srequest != null) {
      Resource resource = srequest.getResource();
      if (resource != null) {
        if (FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceType())
            || FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceSuperType())) {
          if ( accepts(srequest) ) {
            LOGGER.debug("Will Veto this request");
            return true;
          }
        } else {
          LOGGER.debug("No Veto, No the right resource type {}", resource.getResourceType());
        }
      } else {
        LOGGER.debug("No Veto, No resource");
      }
    } else {
      LOGGER.debug("No Veto, No request");
    }
    return false;
  }

  public boolean safeToStream(SlingHttpServletRequest srequest) {
    if (srequest != null) {
      Resource resource = srequest.getResource();
      if (resource != null) {
        if (FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceType())
            || FilesConstants.POOLED_CONTENT_RT.equals(resource.getResourceSuperType())) {
          try {
            if ( accepts(srequest)) {
              Content content = resource.adaptTo(Content.class);
              LOGGER.debug("Content  is {} ",content);
              if ( content != null ) {
                ContentManager contentManager = resource.adaptTo(ContentManager.class);
                String resourceId = getResourceId(srequest, content);
                LOGGER.debug("Resource ID is {} ",resourceId);
                if (resourceId != null) {
                  return !contentManager.hasBody(
                      StorageClientUtils.newPath(content.getPath(), resourceId), null);
                } else {
                  LOGGER.debug("No Resource ID found");
                }
              }
            } else {
              LOGGER.debug("Doesnt accept, so safe to stream");
              return true;
            }
          } catch (JSONException e) {
            LOGGER.warn(e.getMessage(), e);
          } catch (StorageClientException e) {
            LOGGER.warn(e.getMessage(), e);
          } catch (AccessDeniedException e) {
            LOGGER.warn(e.getMessage(), e);
          }
          LOGGER.debug("Not Safe to stream for some reason");
          return false;
        } else {
          LOGGER.debug("Resource is not of correct type {} ", resource.getResourceType());
        }
      } else {
        LOGGER.debug("Resource is null");
      }
    }
    return true;
  }

}
