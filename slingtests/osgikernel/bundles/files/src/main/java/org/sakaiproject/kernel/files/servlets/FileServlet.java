package org.sakaiproject.kernel.files.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Points the request to the actual file.
 * 
 * @scr.component metatype="no" immediate="true" label="FilesReference"
 *                description="Links nodes to files"
 * @scr.property name="service.description" value="Links nodes to files"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/file"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 */
public class FileServlet extends SlingAllMethodsServlet {

  public static final Logger LOGGER = LoggerFactory.getLogger(FileServlet.class);

  /**
   * 
   */
  private static final long serialVersionUID = -1536743371265952323L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Get the filenode.
    Resource baseResource = request.getResource();
    String path = baseResource.getPath();
    path = PathUtils.normalizePath(path);
    String filename = "";
    Node node = (Node) baseResource.adaptTo(Node.class);
    try {
      if (node.hasProperty(FilesConstants.SAKAI_FILENAME)) {
        // This is a sakai/file inside the store.
        filename = node.getProperty(FilesConstants.SAKAI_FILENAME).getString();
        path += "/" + filename;
      } else if (node.hasProperty(FilesConstants.SAKAI_LINK)) {
        // This is a sakai/file outside the store so it is probably linked to one in a
        // store.

        // Get the id to the file and retrieve the actual path.
        String id = node.getProperty(FilesConstants.SAKAI_LINK).getString();
        Session session = node.getSession();
        path = FileUtils.getActualFilePath(id, session);

        if (path == null) {
          response.sendError(HttpServletResponse.SC_CONFLICT, "Unable to download file.");
          return;
        }
      }

      else {
        response.sendError(HttpServletResponse.SC_CONFLICT, "Unable to download file.");
        return;
      }
    } catch (RepositoryException e) {
      response.sendError(500, "Unable to download file.");
      return;
    }
    LOGGER.info("Pointing request {} to the real file {}", baseResource.getPath(), path);

    // Send request to download the file.
    baseResource.getResourceMetadata().setResolutionPath("");
    baseResource.getResourceMetadata().setResolutionPathInfo(path);

    final String finalPath = path;
    final ResourceMetadata rm = baseResource.getResourceMetadata();

    // Wrap the request so it points to the message we just created.
    ResourceWrapper wrapper = new ResourceWrapper(request.getResource()) {
      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
       */
      @Override
      public String getPath() {
        return finalPath;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
       */
      @Override
      public String getResourceType() {
        return "sling/servlet/default";
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceMetadata()
       */
      @Override
      public ResourceMetadata getResourceMetadata() {
        return rm;
      }

    };

    RequestDispatcherOptions options = new RequestDispatcherOptions();
    options.setForceResourceType("sling:File");
    SlingHttpServletResponseWrapper wrappedResponse = new SlingHttpServletResponseWrapper(
        response) {
      ServletOutputStream servletOutputStream = new ServletOutputStream() {

        @Override
        public void write(int b) throws IOException {
        }
      };
      PrintWriter pw = new PrintWriter(servletOutputStream);

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#flushBuffer()
       */
      @Override
      public void flushBuffer() throws IOException {
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getOutputStream()
       */
      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getWriter()
       */
      @Override
      public PrintWriter getWriter() throws IOException {
        return pw;
      }
    };
    options.setReplaceSelectors("");
    request.getRequestDispatcher(wrapper, options).forward(request, wrappedResponse);
    if (!response.isCommitted()) {
      response.reset();
    }

  }
}
