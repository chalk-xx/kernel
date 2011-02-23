package org.sakaiproject.nakamura.profile.servlet;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProfileConstants;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.resource.JSONResponse;
import org.sakaiproject.nakamura.api.resource.lite.ResourceModifyOperation;
import org.sakaiproject.nakamura.api.resource.lite.SparsePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, resourceTypes = { ProfileConstants.GROUP_PROFILE_RT,
    ProfileConstants.USER_PROFILE_RT })
public class ProfileUpdateServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -600556329959608324L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ProfileUpdateServlet.class);

  @Reference
  private ProfileService profileService;
  private ResourceModifyOperation modifyOperation;

  @Override
  public void init() {
    // default operation: create/modify
    modifyOperation = new ResourceModifyOperation(getServletContext());
  }
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String operation = request.getParameter(":operation");
      if ( "import".equals(operation)) {
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
      } else {
        // prepare the response
        HtmlResponse htmlResponse = new JSONResponse();
        htmlResponse.setReferer(request.getHeader("referer"));
        modifyOperation.run(request, htmlResponse, new SparsePostProcessor[]{});
        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
          String redirect = getRedirectUrl(request, htmlResponse);
          if (redirect != null) {
            response.sendRedirect(redirect);
            return;
          }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(response, isSetStatus(request));
      }
    } catch (JSONException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
      return;
    }

  }

  /**
   * compute redirect URL (SLING-126)
   *
   * @param ctx
   *          the post processor
   * @return the redirect location or <code>null</code>
   */
  protected String getRedirectUrl(HttpServletRequest request, HtmlResponse ctx) {
    // redirect param has priority (but see below, magic star)
    String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
    if (result != null && ctx.getPath() != null) {

      // redirect to created/modified Resource
      int star = result.indexOf('*');
      if (star >= 0) {
        StringBuffer buf = new StringBuffer();

        // anything before the star
        if (star > 0) {
          buf.append(result.substring(0, star));
        }

        // append the name of the manipulated node
        buf.append(ResourceUtil.getName(ctx.getPath()));

        // anything after the star
        if (star < result.length() - 1) {
          buf.append(result.substring(star + 1));
        }

        // use the created path as the redirect result
        result = buf.toString();

      } else if (result.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
        // if the redirect has a trailing slash, append modified node
        // name
        result = result.concat(ResourceUtil.getName(ctx.getPath()));
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will redirect to " + result);
      }
    }
    return result;
  }

  protected boolean isSetStatus(SlingHttpServletRequest request) {
    String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
    if (statusParam == null) {
      LOGGER.debug("getStatusMode: Parameter {} not set, assuming standard status code",
          SlingPostConstants.RP_STATUS);
      return true;
    }

    if (SlingPostConstants.STATUS_VALUE_BROWSER.equals(statusParam)) {
      LOGGER.debug("getStatusMode: Parameter {} asks for user-friendly status code",
          SlingPostConstants.RP_STATUS);
      return false;
    }

    if (SlingPostConstants.STATUS_VALUE_STANDARD.equals(statusParam)) {
      LOGGER.debug("getStatusMode: Parameter {} asks for standard status code",
          SlingPostConstants.RP_STATUS);
      return true;
    }

    LOGGER.debug(
        "getStatusMode: Parameter {} set to unknown value {}, assuming standard status code",
        SlingPostConstants.RP_STATUS);
    return true;
  }

}
