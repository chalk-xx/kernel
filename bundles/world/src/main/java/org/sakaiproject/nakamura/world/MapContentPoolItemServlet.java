package org.sakaiproject.nakamura.world;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sparse/Content" }, selectors = { "map-pool" }, methods = { "POST" })
public class MapContentPoolItemServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 5558859864475232589L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MapContentPoolItemServlet.class);

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Content content = request.adaptTo(Content.class);
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
          .adaptTo(javax.jcr.Session.class));
      AccessControlManager accessControlManager = session.getAccessControlManager();
      ContentManager contentManager = session.getContentManager();
      String targetContentPath = request.getParameter("src");
      String[] granted = request.getParameterValues("grant");
      String[] denied = request.getParameterValues("deny");

      int grantedBitmap = buildPermissionBitmap(granted);
      int deniedBitmap = buildPermissionBitmap(denied);
      String aclID = Integer.toHexString(grantedBitmap) + "_"
          + Integer.toHexString(deniedBitmap);
      String tokenPrincipal = AccessControlManager.DYNAMIC_PRINCIPAL_STEM + aclID;

      accessControlManager.setAcl(
          Security.ZONE_CONTENT,
          targetContentPath,
          new AclModification[] {
              new AclModification(AclModification.grantKey(tokenPrincipal),
                  grantedBitmap, Operation.OP_REPLACE),
              new AclModification(AclModification.denyKey(tokenPrincipal), deniedBitmap,
                  Operation.OP_REPLACE), });

      String tokenPath = StorageClientUtils.newPath(content.getPath(), tokenPrincipal);
      Content token = null;
      if (contentManager.exists(tokenPath)) {
        token = contentManager.get(tokenPath);
      } else {
        token = new Content(tokenPath, null);
      }
      accessControlManager.signContentToken(token, targetContentPath);
      contentManager.update(token);

      response.sendError(HttpServletResponse.SC_OK);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Map<String, Object> responseMap = ImmutableMap.of("tokenPrincipalId", (Object) tokenPrincipal);
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      ExtendedJSONWriter.writeValueMap(jsonWriter, responseMap);


    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    }
  }

  private int buildPermissionBitmap(String[] granted) {
    int bitmap = 0;
    for (String permission : granted) {
      bitmap = bitmap | Permissions.parse(permission.toLowerCase()).getPermission();
    }
    return bitmap;
  }

}
