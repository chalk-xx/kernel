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
package org.sakaiproject.kernel.batch;

import org.apache.sling.commons.json.JSONException;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/*
 * 
 * component immediate="true" label="MultiGetFilter"
 *                description="Filter to return multiple resources"
 * property name="service.description"
 *               value="Bundles multiple resource requests into a single response."
 * property name="service.vendor" value="The Sakai Foundation"
 * reference name="ConfigurationPrinter" cardinality="0..n" policy="dynamic"
 *                interface="org.apache.felix.webconsole.ConfigurationPrinter"
 *                bind="bindConfigurationPrinter" unbind="unbindConfigurationPrinter"
 * reference name="HttpService" interface="org.osgi.service.http.HttpService"
 *                bind="bindHttpService" unbind="unbindHttpService"
 */

public class MultiGetFilter implements Filter {

  /**
   *
   */
  private static final long serialVersionUID = 9159034894038200948L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MultiGetFilter.class);

  private static final String RESOURCE_PATH_PARAMETER = "r";
  private static final String IS_BATCH_PARAMETER = "isBatch";
  private static final String SLING_MAIN_SERVLET_NAME = "org.apache.sling.engine.impl.SlingMainServlet";
  private static final String[] URL_PATTERN = new String[] { "/_batch*" };

  //private WebContainer webContainer;

  private HttpService httpService;

  private HttpContext httpContext;

  private BundleContext bundleContext;

  /*
  
  // private ServiceTracker tracker;

  // We try to get the SlingMainServlet because that is the HttpContext where everything
  // is registered with.
  // It is exposed as a configurationPrinter so we try to get it via that way.
  protected void bindConfigurationPrinter(
      ConfigurationPrinter configurationPrinter) {
    System.err.println(configurationPrinter.getClass().getName());
    if (configurationPrinter.getClass().getName().equals(
        SLING_MAIN_SERVLET_NAME)) {

      System.err.println("Got the sling main servlet.");

      this.httpContext = (HttpContext) configurationPrinter;

      if (this.bundleContext != null && this.httpService != null
          && this.httpContext != null) {
        registerFilter();
      }

    }
  }

  protected void unbindConfigurationPrinter(
      ConfigurationPrinter configurationPrinter) {
    if (configurationPrinter.getClass().getName().equals(
        SLING_MAIN_SERVLET_NAME)) {
      this.httpContext = null;
    }
  }

  protected void bindHttpService(HttpService httpService) {
    this.httpService = httpService;
    System.err.println("Got the httpService.");

    if (this.bundleContext != null && this.httpService != null
        && this.httpContext != null) {
      registerFilter();
    }
  }

  protected void unbindHttpService(HttpService httpService) {
    this.httpService = null;
  }

  private void registerFilter() {
    System.err.println("1");

    webContainer = (WebContainer) httpService;

    System.err.println("2");

    if (httpContext == null) {
      System.err.println("httpContext is null");
    } else {
      System.err.println("httpContext is not null");
    }

    System.err.println("3");
    webContainer.registerFilter(this, URL_PATTERN, null, null, httpContext);

    System.err.println("4");
  }

  protected void activate(ComponentContext componentContext) {
    bundleContext = componentContext.getBundleContext();
    System.err.println("Got the bundleContext.");
    if (this.bundleContext != null && this.httpService != null
        && this.httpContext != null) {
      registerFilter();
    }

  }

  public void deactivate(ComponentContext componentContext) {
    webContainer.unregisterFilter(this);
  }
  */

  public void destroy() {

  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    if ("true".equals(request.getParameter(IS_BATCH_PARAMETER))) {

      String[] requestedResources = request
          .getParameterValues(RESOURCE_PATH_PARAMETER);
      if (requestedResources != null && requestedResources.length != 0) {

        response.setContentType("application/json; charset=UTF-8");

        ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
        try {
          write.array();
          for (String resourcePath : requestedResources) {

            outputResource(request, response, write, resourcePath);
          }
          write.endArray();
        } catch (JSONException e) {
          LOGGER.error("Error encoding output as JSON", e);
        }

      } else {
        chain.doFilter(request, response);
      }

    } else {
      chain.doFilter(request, response);
    }
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    // TODO Auto-generated method stub

  }

  private void outputResource(ServletRequest request, ServletResponse response,
      ExtendedJSONWriter write, String resourcePath) throws JSONException {

    try {

      RequestData requestData = new RequestData();
      requestData.setMethod("GET");
      requestData.setUrl(resourcePath);
      requestData.setParameters(new Hashtable<String, String[]>());

      RequestWrapper requestWrapper = new RequestWrapper(
          (HttpServletRequest) request);
      requestWrapper.setRequestData(requestData);
      ResponseWrapper responseWrapper = new ResponseWrapper(
          (HttpServletResponse) response);

      request.getRequestDispatcher(resourcePath).include(requestWrapper,
          responseWrapper);

      outputResponseAsJSON(responseWrapper, write, resourcePath);

    } catch (ServletException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void outputResponseAsJSON(ResponseWrapper responseWrapper,
      ExtendedJSONWriter write, String resourcePath) throws JSONException {
    try {
      write.object();
      write.key("path");
      write.value(resourcePath);
      write.key("status");
      write.value(responseWrapper.getResponseStatus());
      write.key("headers");
      write.object();

      Dictionary<String, String> headers = responseWrapper.getResponseHeaders();
      Enumeration<String> keys = headers.keys();
      while (keys.hasMoreElements()) {
        String k = keys.nextElement();
        write.key(k);
        write.value(headers.get(k));
      }

      write.endObject();
      write.key("data");
      String s = responseWrapper.getDataAsString();
      write.value(s);
      LOGGER.info(s);
      write.endObject();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

  }

}