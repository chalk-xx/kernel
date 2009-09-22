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
package org.sakaiproject.kernel.files.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.files.FileHandler;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 * Points the request to the actual file.
 * 
 * @scr.component metatype="no" immediate="true" label="FileLinkServlet"
 *                description="Links nodes to files"
 * @scr.property name="service.description" value="Links nodes to files"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/link"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 * @scr.reference name="FileHandler"
 *                interface="org.sakaiproject.kernel.api.files.FileHandler"
 *                cardinality="0..n" policy="dynamic"
 */
public class FileLinkServlet extends SlingAllMethodsServlet {

  public static final Logger LOGGER = LoggerFactory.getLogger(FileLinkServlet.class);
  private static final long serialVersionUID = -1536743371265952323L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Resource resource = request.getResource();
    Node node = (Node) resource.adaptTo(Node.class);

    try {
      if (node.hasProperty(FilesConstants.SAKAI_LINK)) {
        String link = node.getProperty(FilesConstants.SAKAI_LINK).getString();

        String[] linkProps = StringUtils.split(link, ':');
        if (linkProps.length == 2) {

          FileHandler handler = fileHandlerTracker.getProcessorByName(linkProps[0]);
          if (handler != null) {
            handler.handleFile(request, response, linkProps[1]);
          }
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Unable to handle linked file.");
      e.printStackTrace();
      response.sendError(500, "Unable to handle linked file.");
    }
  }
  

  //
  // Needed to bind all the file handlers out there to this servlet.
  //
  private FileHandlerTracker fileHandlerTracker = new FileHandlerTracker();

  protected void bindFileHandler(ServiceReference serviceReference) {
    fileHandlerTracker.bindFileHandler(serviceReference);
  }

  protected void unbindFileHandler(ServiceReference serviceReference) {
    fileHandlerTracker.unbindFileHandler(serviceReference);
  }

  protected void activate(ComponentContext componentContext) {
    fileHandlerTracker.setComponentContext(componentContext);
  }
  
}
