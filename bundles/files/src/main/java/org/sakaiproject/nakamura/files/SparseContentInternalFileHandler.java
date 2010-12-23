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
package org.sakaiproject.nakamura.files;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.sakaiproject.nakamura.api.files.LinkHandler;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 *
 */
@Component(immediate = true, label = "JcrInternalFileHandler")
@Service(value = LinkHandler.class)
@Properties({ @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.files.handler", value = "sparseinternal") })
public class SparseContentInternalFileHandler implements LinkHandler {
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.files.LinkHandler#handleFile(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse, java.lang.String)
   */
  public void handleFile(SlingHttpServletRequest request,
      SlingHttpServletResponse response, String to) throws ServletException, IOException {
    // JcrInternalFileHandler converted a node ID to a path then redirected.
    // TODO check that it is safe to assume "to" is the path since sparse content is path based -CFH
    response.sendRedirect(to);
  }

}
