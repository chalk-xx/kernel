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
package org.sakaiproject.kernel.personal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * 
 */
public abstract class AbstractPersonalServlet extends SlingAllMethodsServlet {


  
  /**
   *
   */
  private static final long serialVersionUID = -7892996718559864951L;
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPersonalServlet.class);
  

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("Processing {}",request.getRequestURI());
    hashRequest(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOGGER.info("Processing {}",request.getRequestURI());
    hashRequest(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("Processing {}",request.getRequestURI());
    hashRequest(request, response);
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("Processing {}",request.getRequestURI());
    hashRequest(request, response);
  }


  /**
   * @param request
   * @param response
   * @throws ServletException 
   * @throws IOException 
   */
  protected abstract void hashRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException, ServletException;

  
  


}
