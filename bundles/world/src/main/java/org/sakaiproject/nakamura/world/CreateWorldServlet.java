package org.sakaiproject.nakamura.world;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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
import java.io.InputStream;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@Component(immediate = true, metatype = true, enabled = true)
@SlingServlet(paths = { "/system/world/create" }, methods = { "POST" }, generateComponent = false)
public class CreateWorldServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 529333168619884684L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateWorldServlet.class);

  @Reference
  private TemplateService templateService;

  @Reference
  protected ClusterTrackingService clusterTrackingService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String template = request.getParameter("template");
      String at = request.getParameter("at");
      @SuppressWarnings("unchecked")
      Map<String, Object> params = request.getParameterMap();

      javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(
          javax.jcr.Session.class);
      Session session = StorageClientUtils.adaptToSession(jcrSession);
      ContentManager contentManager = session.getContentManager();
      AccessControlManager accessControlManager = session.getAccessControlManager();

      String templateContent = getTemplateContent(jcrSession, template, response);
      if (templateContent == null) {
        return;
      }
      // we now have the template, try and create the base node.

      if (contentManager.exists(at)) {
        response.sendError(HttpServletResponse.SC_CONFLICT, "World already exists");
        return;
      }

      Content content = new Content(at, ImmutableMap.of(
          SlingConstants.PROPERTY_RESOURCE_TYPE, (Object) "sakai/world"));
      contentManager.update(content);

      // process the template
      params.put(TemplateService.ID_GENERATOR, new TemplateIDGenerator(new IDGenerator() {

        public String nextId() {
          return clusterTrackingService.getClusterUniqueId();
        }
      }));
      String toLoad = templateService.evaluateTemplate(params, templateContent);
      JSONObject toLoadJson = new JSONObject(toLoad);
      LiteJsonImporter importer = new LiteJsonImporter();
      importer.importContent(contentManager, toLoadJson, at, false, false, false,
          accessControlManager);

      response.sendError(HttpServletResponse.SC_CREATED);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Map<String, Object> responseMap = ImmutableMap.of("newworld", (Object) at);
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      ExtendedJSONWriter.writeValueMap(jsonWriter, responseMap);

    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
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

  private String getTemplateContent(javax.jcr.Session jcrSession, String template,
      HttpServletResponse response) throws IOException, RepositoryException {

    if (template == null || template.length() == 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "template parameter is required, path to the template resource");
      return null;
    }
    if (template.indexOf("..") >= 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "template does not exist");
      return null;
    }

    String templatePath = "/var/world/templates" + template;

    if (!jcrSession.itemExists(templatePath)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "template does not exist");
      return null;
    }
    Node n = jcrSession.getNode(templatePath);

    String templateContent = null;
    if ( n.hasNode(JcrConstants.JCR_CONTENT)) {
      Node contentNode = n.getNode(JcrConstants.JCR_CONTENT);
      InputStream in = contentNode.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
      templateContent = IOUtils.toString(in, "UTF-8");
    } else if (n.hasProperty("template")) {
      templateContent = n.getProperty("template").getString();
    }

    if (templateContent == null || templateContent.length() == 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "template is empty");
      return null;
    }
    return templateContent;
  }

}
