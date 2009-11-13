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
package org.sakaiproject.kernel.resource;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;

/**
 *
 */
@SlingServlet(paths = "/system/rp", methods = "GET")
@Reference(name = "virtualResourceType", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = VirtualResourceType.class, policy = ReferencePolicy.DYNAMIC)
public class VirtualResourceProviderServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 3426715052386292398L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(VirtualResourceProviderServlet.class);

  @Reference
  public VirtualResourceProvider virtualResourceProvider;

  public List<VirtualResourceType> virtualResourceType = new ArrayList<VirtualResourceType>();

  private Map<String, VirtualResourceType> virtualResourceTypes = new ConcurrentHashMap<String, VirtualResourceType>();


  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("test/plain");
    PrintWriter pw = response.getWriter();
    pw.println("Virtual Resource Provieder = " + virtualResourceProvider);
    pw.println("Virtual Resource Provieder Typess = "
        + Arrays.toString(virtualResourceTypes.keySet().toArray(new String[0])));
    pw.println("Virtual Resource Provieder Types Map = " + virtualResourceTypes);
    pw.println("Virtual Resource Provieder Types List = " + virtualResourceType.size());
    pw.println("Virtual Resource Provieder Types List = "
        + Arrays.toString(virtualResourceType.toArray()));
  }


  protected void bindVirtualResourceType(VirtualResourceType virtualResourceType) {
    virtualResourceTypes.put(virtualResourceType.getResourceType(), virtualResourceType);
  }

  protected void unbindVirtualResourceType(VirtualResourceType virtualResourceType) {
    virtualResourceTypes.remove(virtualResourceType.getResourceType());
  }

}
