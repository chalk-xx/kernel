package org.sakaiproject.kernel.files.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.files.search.FileSearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Dumps all the files of a sakai/folder.
 * 
 * @scr.component metatype="no" immediate="true" label="FilesReference"
 *                description="Links nodes to files"
 * @scr.property name="service.description" value="Dumps all the files of a sakai/folder."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/folder"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 * @scr.property name="sling.servlet.selectors" values.0="files"
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 */
public class FilesFolderServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 2602398397981186645L;
  private static final Logger LOGGER = LoggerFactory.getLogger(FilesFolderServlet.class);

  private SiteService siteService;

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }
  
  
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Node node = (Node) request.getResource().adaptTo(Node.class);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    String path = "";

    // Get all the children of this node.
    try {

      if (LOGGER.isDebugEnabled()) {
        path = node.getPath();
      }
      JSONWriter write = new JSONWriter(response.getWriter());
      FileSearchResultProcessor processor = new FileSearchResultProcessor();
      processor.bindSiteService(siteService);
      NodeIterator it = node.getNodes();

      write.array();
      while (it.hasNext()) {
        Node child = it.nextNode();
        // File node
        if (child.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          String resourceType = child.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
          if (resourceType.equals(FilesConstants.RT_SAKAI_LINK)) {
        	  FileUtils.writeLinkNode(child, session, write, siteService);
          }
          // Folder node
          else if (resourceType.equals(FilesConstants.RT_SAKAI_FOLDER)){
            write.object();
            ExtendedJSONWriter.writeNodeContentsToWriter(write, child);
            write.key("path");
            write.value(child.getPath());
            write.key("name");
            write.value(child.getName());
            write.endObject();
          }
        }
      }
      write.endArray();

    } catch (RepositoryException e) {
      LOGGER.warn("Unable to list all the file/folders for {}", path);
      e.printStackTrace();
      response.sendError(500, "Unable to list all the file/folders");
    } catch (JSONException e) {
      LOGGER.warn("Unable to list all the file/folders for {}", path);
      e.printStackTrace();
      response.sendError(500, "Unable to list all the file/folders");
    }

  }
}
