package org.sakaiproject.nakamura.world;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.LiteJsonImporter;
import org.sakaiproject.nakamura.api.templates.IDGenerator;
import org.sakaiproject.nakamura.api.templates.TemplateIDGenerator;
import org.sakaiproject.nakamura.api.templates.TemplateService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@Component(immediate = false, metatype = true, enabled = true)
@SlingServlet(resourceTypes = { "sakai/world" }, selectors = { "associate",
    "disassociate" }, methods = { "POST" }, generateComponent = false)
public class WorldAssociateServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 77119111837101599L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(WorldAssociateServlet.class);

  @Reference
  private TemplateService templateService;

  @Reference
  protected ClusterTrackingService clusterTrackingService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String au = request.getParameter("authorizable");
      String role = request.getParameter("role");
      @SuppressWarnings("unchecked")
      Map<String, Object> params = request.getParameterMap();
      if (au == null || au.length() == 0 || role == null || role.length() == 0) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "role and authorizable ID must be specified ");
        return;
      }
      if (role.contains("..")) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid role specified ");
        return;
      }

      Resource resource = request.getResource();
      Content worldContent = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);

      Content roleDefinition = contentManager.get(StorageClientUtils.newPath(
          worldContent.getPath(), "_meta/_role_templates/" + role));
      if (roleDefinition == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Role does not exist");
        return;
      }

      javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(
          javax.jcr.Session.class);
      Session session = StorageClientUtils.adaptToSession(jcrSession);
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      AccessControlManager accessControlManager = session.getAccessControlManager();

      Authorizable authorizable = authorizableManager.findAuthorizable(au);
      if (authorizable == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Authorizable does not exist");
        return;
      }

      String selector = request.getRequestPathInfo().getSelectorString();
      String template = null;
      if (selector.contains("associate")) {
        template = (String) roleDefinition.getProperty("_associate");
      } else if (selector.contains("disassociate")) {
        template = (String) roleDefinition.getProperty("_disassociate");
      }
      if (template == null || template.length() == 0) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Role association/disassociation template is invalid");
        return;
      }

      // process the template
      params.put(TemplateService.ID_GENERATOR, new TemplateIDGenerator(new IDGenerator() {

        public String nextId() {
          return clusterTrackingService.getClusterUniqueId();
        }
      }));
      String associatedJson = templateService.evaluateTemplate(params, template);
      JSONObject jsonAssociate = new JSONObject(associatedJson);

      LiteJsonImporter jsonImporter = new LiteJsonImporter();
      jsonImporter.importContent(contentManager, jsonAssociate, worldContent.getPath(),
          true, true, false, accessControlManager);

      response.sendError(HttpServletResponse.SC_OK);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Map<String, Object> responseMap = ImmutableMap.of("world",
          (Object) worldContent.getPath(), "role", role, "authorizable", au);
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      ExtendedJSONWriter.writeValueMap(jsonWriter, responseMap);

    } catch (AccessDeniedException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
      return;
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      throw new ServletException("Template is invalid json once processed "
          + e.getMessage(), e);
    }
  }
}
