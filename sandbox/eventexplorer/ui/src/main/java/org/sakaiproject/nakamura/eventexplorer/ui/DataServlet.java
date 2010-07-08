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
package org.sakaiproject.nakamura.eventexplorer.ui;

import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.eventexplorer.api.cassandra.CassandraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service(value = Servlet.class)
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.description", value = "Event data servlet."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class DataServlet extends HttpServlet implements Servlet {

  private static final long serialVersionUID = -7279082919880845845L;
  @Reference
  protected transient CassandraService cassandraService;
  private Client client;

  private static final Logger LOGGER = LoggerFactory.getLogger(DataServlet.class);

  protected void activate(ComponentContext context) {
    client = cassandraService.getClient();
    LOGGER.warn("got cassandra client.");
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    int len = Integer.parseInt(request.getParameter("len"));
    String[] val = request.getParameterValues("val");
    String[] ar = request.getParameterValues("ar");
    User user = new User(ar, val, len);
  }

}
