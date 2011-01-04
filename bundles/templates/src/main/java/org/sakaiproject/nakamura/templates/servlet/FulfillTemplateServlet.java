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

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.osgi.service.component.ComponentContext;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.sakaiproject.nakamura.templates.FooTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.velocity.app.VelocityEngine;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(paths = { "/system/template" }, generateComponent = true, generateService = true, methods = { "GET" })
public class FulfillTemplateServlet extends SlingSafeMethodsServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(FulfillTemplateServlet.class);

  private VelocityEngine velocityEngine;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get template document
    // Find all velocity replacement variable(s) in the endpointURL,
        // copy any equivalent keys from the input Map, to a new Map that
        // can be process by Velocity. In the new Map, the Map value field
        // has been changed from RequestParameter[] to String.

        Map<String, String> inputContext = new HashMap<String, String>();
        String endpointURL = sampleTemplate();
        int startPosition = endpointURL.indexOf("${");
        while(startPosition > -1) {
          int endPosition = endpointURL.indexOf("}", startPosition);
          if (endPosition > -1) {
            String key = endpointURL.substring(startPosition + 2, endPosition);
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
            startPosition = endpointURL.indexOf("${", endPosition);
          } else {
            break;
          }
        }

        VelocityContext context = new VelocityContext(inputContext);
//    Session session = request.getResourceResolver().adaptTo(Session.class);
      RequestParameter templatePathParam = request.getRequestParameter("templatePath");
      if (templatePathParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This request must have a 'templatePath' parameter.");
        return;
      }

      if ("".equals(templatePathParam)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The 'templatePath' parameter must not be blank.");
        return;
      }
    // combine template with parameter map
    Reader template = new StringReader(sampleTemplate());
    StringWriter templateWriter = new StringWriter();
    velocityEngine.evaluate(context, templateWriter, "templateprocessing", template);
    // return the result
    PrintWriter w = response.getWriter();
    w.append(templateWriter.toString());
    w.flush();

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

    velocityEngine.setProperty(VelocityEngine.RESOURCE_LOADER, "foo");
    velocityEngine.setProperty("foo.resource.loader.class", FooTemplateLoader.class.getName());
    ExtendedProperties configuration = new ExtendedProperties();
    configuration.addProperty("foo.resource.loader.resourceSource", this);
    velocityEngine.setExtendedProperties(configuration);
    try {
      velocityEngine.init();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

}
