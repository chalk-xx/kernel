package org.sakaiproject.nakamura.files.pool;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StreamHelper {

  public void stream(HttpServletRequest request, ContentManager contentManager, Content node, String alternativeStream, HttpServletResponse response, Resource resource, ServletContext servletContext) throws IOException, StorageClientException, AccessDeniedException {
    InputStream dataStream = contentManager.getInputStream(node.getPath(), alternativeStream);

    if ( dataStream == null ) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    
    Map<String, Object> properties = node.getProperties();
    long modifTime = StorageClientUtils.toLong(properties.get(StorageClientUtils.getAltField(Content.LASTMODIFIED_FIELD, alternativeStream)));
    if (unmodified(request, modifTime)) {
      response.setStatus(SC_NOT_MODIFIED);
      return;
    }

    setHeaders(properties, resource, response, alternativeStream, servletContext);
    setContentLength(properties, response, alternativeStream);
    IOUtils.copyLarge(dataStream, response.getOutputStream());
    dataStream.close();
  }

  /**
   * Returns <code>true</code> if the request has a <code>If-Modified-Since</code> header
   * whose date value is later than the last modification time given as
   * <code>modifTime</code>.
   *
   * @param request
   *          The <code>ComponentRequest</code> checked for the
   *          <code>If-Modified-Since</code> header.
   * @param modifTime
   *          The last modification time to compare the header to.
   * @return <code>true</code> if the <code>modifTime</code> is less than or equal to the
   *         time of the <code>If-Modified-Since</code> header.
   */
  private boolean unmodified(HttpServletRequest request, long modifTime) {
    if (modifTime > 0) {
      long modTime = modifTime / 1000; // seconds
      long ims = request.getDateHeader(HEADER_IF_MODIFIED_SINCE) / 1000;
      return modTime <= ims;
    }

    // we have no modification time value, assume modified
    return false;
  }

  /**
   * @param resource
   * @param request
   * @param response
   * @param servletContext 
   * @throws RepositoryException
   */
  private void setHeaders(Map<String, Object> properties, Resource resource, HttpServletResponse response, String alternativeStream, ServletContext servletContext) {

    long modifTime = StorageClientUtils.toLong(properties.get(StorageClientUtils.getAltField(Content.LASTMODIFIED_FIELD, alternativeStream)));
    if (modifTime > 0) {
      response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
    }

    String contentType = (String) properties.get(StorageClientUtils.getAltField(Content.MIMETYPE_FIELD, alternativeStream));
    if (contentType == null) {
      final String ct = servletContext.getMimeType(resource.getPath());
      if (ct != null) {
        contentType = ct;
      }
    }
    if (contentType != null) {
      response.setContentType(contentType);
    }

    String encoding = (String) properties.get(StorageClientUtils.getAltField(Content.ENCODING_FIELD, alternativeStream));
    if (encoding != null) {
      response.setCharacterEncoding(encoding);
    }
  }

  /**
   * Set the <code>Content-Length</code> header to the give value. If the length is larger
   * than <code>Integer.MAX_VALUE</code> it is converted to a string and the
   * <code>setHeader(String, String)</code> method is called instead of the
   * <code>setContentLength(int)</code> method.
   *
   * @param response
   *          The response on which to set the <code>Content-Length</code> header.
   * @param length
   *          The content length to be set. If this value is equal to or less than zero,
   *          the header is not set.
   */
  private void setContentLength(Map<String,Object> properties, HttpServletResponse response, String alternativeStream) {
    long length = StorageClientUtils.toLong(properties.get(StorageClientUtils.getAltField(Content.LENGTH_FIELD, alternativeStream)));
    if (length > 0) {
      if (length < Integer.MAX_VALUE) {
        response.setContentLength((int) length);
      } else {
        response.setHeader("Content-Length", String.valueOf(length));
      }
    }
  }

}
