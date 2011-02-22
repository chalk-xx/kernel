package org.sakaiproject.nakamura.profile.servlet;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProfileConstants;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, resourceTypes = { ProfileConstants.GROUP_PROFILE_RT,
    ProfileConstants.USER_PROFILE_RT })
public class ProfileUpdateServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -600556329959608324L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ProfileUpdateServlet.class);

  @Reference
  private ProfileService profileService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String content = request.getParameter(":content");
      if (content == null || content.length() == 0) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            ":content parameter is missing");
        return;
      }
      JSONObject json = new JSONObject(content);
      
      Resource resource = request.getResource();
      Content targetContent = resource.adaptTo(Content.class);
      Session session = resource.adaptTo(Session.class);
      
      LOGGER.info("Got profile update {} ", json);
      profileService.update(session, targetContent.getPath(), json);
      

      response.setStatus(200);
      response.getWriter().write("Ok");
    } catch (JSONException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
      return;
    }

  }

}
