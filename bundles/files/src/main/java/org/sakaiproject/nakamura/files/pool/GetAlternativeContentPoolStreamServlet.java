/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.files.pool;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@SlingServlet(methods = { "GET" }, extensions={"*"}, resourceTypes = { "sakai/pooled-content" })
public class GetAlternativeContentPoolStreamServlet extends SlingSafeMethodsServlet
    implements OptingServlet {
  /**
   *
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GetAlternativeContentPoolStreamServlet.class);
  private static final long serialVersionUID = 6605017133790005483L;
  private static final Set<String> RESERVED_SELECTORS = new HashSet<String>();
  static {
    RESERVED_SELECTORS.add("selector-used-elsewhere");
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Content node = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      String alternativeStream = getAlternativeStream(request);
      InputStream dataStream = contentManager.getInputStream(node.getPath(), alternativeStream);

      if ( dataStream == null ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      
      Map<String, Object> properties = node.getProperties();
      long modifTime = StorageClientUtils.toLong(properties.get(StorageClientUtils.getAltField(Content.LASTMODIFIED, alternativeStream)));
      if (unmodified(request, modifTime)) {
        response.setStatus(SC_NOT_MODIFIED);
        return;
      }

      setHeaders(properties, resource, response, alternativeStream);
      setContentLength(properties, response, alternativeStream);
      IOUtils.copyLarge(dataStream, response.getOutputStream());
      dataStream.close();
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(),e);
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(),e);
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(),e);
      throw new ServletException(e.getMessage(), e);
    }
  }

  private String getAlternativeStream(SlingHttpServletRequest request) {
    RequestPathInfo rpi = request.getRequestPathInfo();
    String alternativeStream = rpi.getExtension();
    String[] selectors = rpi.getSelectors();
    if (selectors != null && selectors.length > 0) {
      alternativeStream = selectors[0];
    }
    return alternativeStream;
  }

  /**
   * Do not interfere with the default servlet's handling of streaming data, which kicks
   * in if no extension has been specified was specified in the request. (Sling servlet
   * resolution uses a servlet's declared list of "extensions" for score weighing, not for
   * filtering.)
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    RequestPathInfo rpi = request.getRequestPathInfo();
    String[] selectors = rpi.getSelectors();
    if ( selectors != null && selectors.length == 1 ) {
      if (!RESERVED_SELECTORS.contains(selectors[0])) {
        return true;
      }
    }
    return false;
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
   * @throws RepositoryException
   */
  private void setHeaders(Map<String, Object> properties, Resource resource, SlingHttpServletResponse response, String alternativeStream) {

    long modifTime = StorageClientUtils.toLong(properties.get(StorageClientUtils.getAltField(Content.LASTMODIFIED, alternativeStream)));
    if (modifTime > 0) {
      response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
    }

    String contentType = (String) properties.get(StorageClientUtils.getAltField(Content.MIMETYPE, alternativeStream));
    if (contentType == null) {
      final String ct = getServletContext().getMimeType(resource.getPath());
      if (ct != null) {
        contentType = ct;
      }
    }
    if (contentType != null) {
      response.setContentType(contentType);
    }

    String encoding = (String) properties.get(StorageClientUtils.getAltField(Content.ENCODING, alternativeStream));
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
