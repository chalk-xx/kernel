/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.templates.servlet;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.ComponentContext;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.sakaiproject.nakamura.api.templates.TemplateNodeSource;
import org.sakaiproject.nakamura.templates.velocity.JcrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.velocity.app.VelocityEngine;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/template" }, generateComponent = true, generateService = true, methods = { "GET" })
public class FulfillTemplateServlet extends SlingSafeMethodsServlet implements TemplateNodeSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FulfillTemplateServlet.class);
  private ThreadLocal<Node> boundNode = new ThreadLocal<Node>();

  private VelocityEngine velocityEngine;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Resource requestResource = request.getResource();
      Node templateNode = requestResource.adaptTo(Node.class);
      String templateText = "";
      if (templateNode != null && templateNode.hasProperty("sakai:template")) {
        templateText = templateNode.getProperty("sakai:template").getString();
        boundNode.set(templateNode);
      } else {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Requested path does not contain a Sakai template");
        return;
      }
      // Find all velocity replacement variable(s) in the templateText,
      // copy any equivalent keys from the input Map, to a new Map that
      // can be process by Velocity. In the new Map, the Map value field
      // has been changed from RequestParameter[] to String.
      Map<String, String> inputContext = new HashMap<String, String>();
      int startPosition = templateText.indexOf("${");
      while(startPosition > -1) {
        int endPosition = templateText.indexOf("}", startPosition);
        if (endPosition > -1) {
          String key = templateText.substring(startPosition + 2, endPosition);
          Object value = request.getRequestParameter(key);
          if (value instanceof RequestParameter[]) {
            // now change input value object from RequestParameter[] to String
            // and add to inputContext Map.
            RequestParameter[] requestParameters = (RequestParameter[]) value;
            inputContext.put(key, requestParameters[0].getString());
          } else {
            // KERN-1346 regression; see KERN-1409
            inputContext.put(key, value.toString());
          }
          // look for the next velocity replacement variable
          startPosition = templateText.indexOf("${", endPosition);
        } else {
          break;
        }
      }

      if (templateNode.hasProperty("sakai:content-type")) {
        response.setContentType(templateNode.getProperty("sakai:content-type").getString());
      }

      VelocityContext context = new VelocityContext(inputContext);
      // combine template with parameter map
      Reader template = new StringReader(templateText);
      StringWriter templateWriter = new StringWriter();
      velocityEngine.evaluate(context, templateWriter, "templateprocessing", template);
      // return the result
      PrintWriter w = response.getWriter();
      w.append(templateWriter.toString());
      w.flush();
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
    }

  }

  protected void activate(ComponentContext ctx) throws Exception {
    velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new LogChute() {
      public void init(RuntimeServices runtimeServices) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      public void log(int i, String s) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      public void log(int i, String s, Throwable throwable) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      public boolean isLevelEnabled(int i) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }
    });

    velocityEngine.setProperty(VelocityEngine.RESOURCE_LOADER, "jcr");
    velocityEngine.setProperty("jcr.resource.loader.class", JcrResourceLoader.class.getName());
    ExtendedProperties configuration = new ExtendedProperties();
    configuration.addProperty("jcr.resource.loader.resourceSource", this);
    velocityEngine.setExtendedProperties(configuration);
    try {
      velocityEngine.init();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public Node getNode() {
    return boundNode.get();
  }
}
