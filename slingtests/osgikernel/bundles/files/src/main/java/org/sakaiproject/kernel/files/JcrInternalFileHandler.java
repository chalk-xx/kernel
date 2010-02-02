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
package org.sakaiproject.kernel.files;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.sakaiproject.kernel.api.files.LinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Handles files that are linked to a jcrinternal resource.
 * 
 */

@Component(immediate = true, label = "JcrInternalFileHandler")
@Service(value = LinkHandler.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.files.handler", value = "jcrinternal") })
public class JcrInternalFileHandler implements LinkHandler {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(JcrInternalFileHandler.class);

  public void handleFile(SlingHttpServletRequest request,
      SlingHttpServletResponse response, String to) throws ServletException, IOException {
    Session session = request.getResourceResolver().adaptTo(Session.class);

    String path = null;
    try {
      // Check if the to value is a UUID
      UUID uuid = UUID.fromString(to);
      Node node = session.getNodeByUUID(uuid.toString());
      path = node.getPath();
    } catch (Exception e) {
      // We assume a path was specified.
      path = to;
    }

    response.sendRedirect(path);
  }
}