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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.util.PathUtils;
import org.sakaiproject.kernel.util.StringUtils;
import org.sakaiproject.kernel.util.URIExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet will crop and cut images.
 */
@SlingServlet(paths = "/var/image/cropit", methods = { "POST" })
@Properties(value = { @Property(name = "service.description", value = "Crops an image."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class CropItServlet extends SlingAllMethodsServlet {

  private static final Logger logger = LoggerFactory.getLogger(CropItServlet.class);
  private static final long serialVersionUID = 7893384805719426200L;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    RequestParameter imgParam = request.getRequestParameter("img");
    RequestParameter saveParam = request.getRequestParameter("save");
    RequestParameter xParam = request.getRequestParameter("x");
    RequestParameter yParam = request.getRequestParameter("y");
    RequestParameter widthParam = request.getRequestParameter("width");
    RequestParameter heightParam = request.getRequestParameter("height");
    RequestParameter dimensionsParam = request.getRequestParameter("dimensions");

    if (imgParam == null || saveParam == null || xParam == null || yParam == null
        || widthParam == null || heightParam == null || dimensionsParam == null) {
      response
          .sendError(HttpServletResponse.SC_BAD_REQUEST,
              "The following parameters are required: img, save, x, y, width, height, dimensions");
      return;
    }

    try {
      // Grab the session
      ResourceResolver resourceResolver = request.getResourceResolver();
      Session session = resourceResolver.adaptTo(Session.class);

      String img = imgParam.getString();
      String save = saveParam.getString();
      int x = Integer.parseInt(xParam.getString());
      int y = Integer.parseInt(yParam.getString());
      int width = Integer.parseInt(widthParam.getString());
      int height = Integer.parseInt(heightParam.getString());
      String[] dimensionsList = StringUtils.split(dimensionsParam.getString(), ';');
      List<Dimension> dimensions = new ArrayList<Dimension>();
      for (String s : dimensionsList) {
        Dimension d = new Dimension();
        String[] size = StringUtils.split(s, 'x');
        int diWidth = Integer.parseInt(size[0]);
        int diHeight = Integer.parseInt(size[1]);

        diWidth = checkIntBiggerThanZero(diWidth, 0);
        diHeight = checkIntBiggerThanZero(diHeight, 0);

        d.setSize(diWidth, diHeight);
        dimensions.add(d);
      }
      
      // Make sure we have correct values.
      img = URIExpander.expandStorePath(session, img);
      save = URIExpander.expandStorePath(session, save);

      x = checkIntBiggerThanZero(x, 0);
      y = checkIntBiggerThanZero(y, 0);
      width = checkIntBiggerThanZero(width, 0);
      height = checkIntBiggerThanZero(height, 0);

      // Make sure the save path is correct.
      save = PathUtils.normalizePath(save) + "/";

      String[] crop = CropItProcessor.crop(session, x, y, width, height, dimensions, img,
          save);

      JSONWriter output = new JSONWriter(response.getWriter());
      output.object();
      output.key("files");
      output.array();
      for (String url : crop) {
        output.value(url);
      }
      output.endArray();
      output.endObject();

    } catch (ArrayIndexOutOfBoundsException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The dimensions have to be specified in a widthxheight;widthxheight fashion.");
      return;
    } catch (NumberFormatException e) {
      response
          .sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              "The following parameters have to be integers: x, y, width, height. (Dimensions has to be of the form widthxheight;widthxheight");
      return;
    } catch (ImageException e) {
      // Something went wrong..
      logger.warn("ImageException e: " + e.getMessage());
      e.printStackTrace();
      response.sendError(e.getCode(), e.getMessage());
    } catch (JSONException e) {
      response.sendError(500, "Unable to output JSON.");
    } catch (RepositoryException e) {
      logger.warn("ReposityoryException: " + e.getMessage());
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private int checkIntBiggerThanZero(int val, int defaultVal) {
    if (val < 0) {
      return defaultVal;
    }
    return val;
  }
}
