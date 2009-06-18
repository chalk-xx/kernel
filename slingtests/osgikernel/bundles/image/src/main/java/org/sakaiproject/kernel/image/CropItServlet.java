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
package org.sakaiproject.kernel.image;

import net.sf.json.JSONArray;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.jcr.JCRService;
import org.sakaiproject.kernel.api.jcr.support.JCRNodeFactoryService;
import org.sakaiproject.kernel.util.PathUtils;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * This servlet will crop and cut images.
 * 
 * @scr.component immediate="true" label="%cropit.get.operation.name"
 *                description="%cropit.get.operation.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.paths" value="/system/image/cropit"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.extensions" value="json"
 */
public class CropItServlet extends SlingAllMethodsServlet {

  // private static final Logger LOGGER = LoggerFactory
  // .getLogger(CropItServlet.class);
  private static final long serialVersionUID = 7893384805719426200L;

  /** @scr.reference */
  private JCRNodeFactoryService jcrNodeFactoryService;

  protected void bindJcr(JCRNodeFactoryService jcrNodeFactoryService) {
    this.jcrNodeFactoryService = jcrNodeFactoryService;
  }

  protected void unbindJcr(JCRNodeFactoryService jcrNodeFactoryService) {
    this.jcrNodeFactoryService = null;
  }

  /** @scr.reference */
  private JCRService jcrService;

  protected void bindJcr(JCRService jcrService) {
    this.jcrService = jcrService;
  }

  protected void unbindJcr(JCRService jcrService) {
    this.jcrService = null;
  }

  /**
   * Perform the actual request. {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {

    // Get the parameters.
    int x = Integer.parseInt(request.getRequestParameter("x").toString());
    int y = Integer.parseInt(request.getRequestParameter("y").toString());
    int width = Integer.parseInt(request.getRequestParameter("width")
        .toString());
    int height = Integer.parseInt(request.getRequestParameter("height")
        .toString());
    String urlSaveIn = request.getRequestParameter("urlSaveIn").toString();
    String urlToCrop = request.getRequestParameter("urlToCrop").toString();

    // Make sure that we have correct values for the cropping.
    x = checkIntBiggerThanZero(x, 1);
    y = checkIntBiggerThanZero(y, 1);
    // We check for a width and a height.
    // If none is provided we pass along 0.
    // The Processor will use the entire image then.
    width = checkIntBiggerThanZero(width, 0);
    height = checkIntBiggerThanZero(height, 0);

    // Make sure that the path is a right path.
    urlSaveIn = PathUtils.normalizePath(urlSaveIn) + "/";

    JSONArray dimensions = JSONArray.fromObject(request.getRequestParameter(
        "dimensions").toString());

    try {
      // Get the resource at the provided path. (for /var/image/cropit)
      ResourceResolver resourceResolver = request.getResourceResolver();
      Resource resource = resourceResolver.getResource(urlToCrop);

      // Get the resource from the path we are now. (for node.cropit.json)
      // Resource resource = request.getResource();

      // Convert the resource to a node and take the session from it and add it
      // to jcrService.
      Node imgToCrop = resource.adaptTo(Node.class);

      Session s = imgToCrop.getSession();
      System.out.println("Session: " + s);
      System.out.println("Session userid: " + s.getUserID());

      jcrService.setSession(s);

      String[] crop = CropItProcessor.crop(x, y, width, height, dimensions,
          urlSaveIn, imgToCrop, jcrNodeFactoryService);

      // Send output back.
      JSONWriter output = new JSONWriter(response.getWriter());
      output.object();
      output.key("files");
      output.array();
      for (String url : crop) {
        output.value(url);
      }
      output.endArray();
      output.key("response");
      output.value("OK");
      output.endObject();

      System.out.println("Outputted: " + output.toString());
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ImageException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return;

  }

  private int checkIntBiggerThanZero(int val, int defaultVal) {
    if (val <= 0) {
      return defaultVal;
    }
    return val;
  }
}