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
package org.sakaiproject.nakamura.calendar;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.CalendarService;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Session;
import javax.servlet.ServletException;

@SlingServlet(generateComponent = true, generateService = true, paths = { "/system/calendar/store" }, methods = { "GET" })
public class CalendarTestServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1083359817389813778L;
  @Reference
  protected transient CalendarService calendarService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    System.err.println("Trying store");
    String r = "basic.ics";
    RequestParameter rParam = request.getRequestParameter("r");
    if (rParam != null) {
      r = rParam.getString();
    }
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(r);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    RequestParameter p = request.getRequestParameter("p");
    String path = p.getString();

    try {
      calendarService.store(in, session, path);
      System.err.println("Store succeeded");
    } catch (CalendarException e) {
      e.printStackTrace();
      response.sendError(e.getCode(), e.getMessage());
    }

  }

}
