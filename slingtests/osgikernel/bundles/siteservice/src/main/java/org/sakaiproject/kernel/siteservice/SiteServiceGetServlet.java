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
package org.sakaiproject.kernel.siteservice;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 * 
 * @scr.component immediate="true" label="SiteServiceGetServlet"
 *                description="Get servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Renders sites"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="html"
 */
public class SiteServiceGetServlet extends SlingAllMethodsServlet {
  
  public static final String SITE_RESOURCE_TYPE = "sakai:site";
  public static final String DEFAULT_SITE = "/sites/default.html";
  
  private static final Logger LOG = LoggerFactory.getLogger(SiteServiceGetServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOG.info("Got get to SiteServiceGetServlet");
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null)
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
      return;
    }
    try {
      String templatePath = DEFAULT_SITE;
      if (site.hasProperty("sakai:site-template"))
      {
        templatePath = site.getProperty("sakai:site-template").getString();
      }
      Resource siteTemplate = request.getResourceResolver().getResource(templatePath);
      IOUtils.stream(siteTemplate.adaptTo(InputStream.class), response.getOutputStream());
      LOG.info("Streamed site template");
      return;
    } catch (ValueFormatException e) {
      LOG.error("Unable to read template value", e);
    } catch (PathNotFoundException e) {
      LOG.error("Unable to read template value", e);
    } catch (RepositoryException e) {
      LOG.error("Unable to read template value", e);
    }
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to render template");
    return;
  }

}
