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
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceResponse;

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
@ServiceDocumentation(name = "Virtual Resource Provider Servlet", 
    description = "Lists the available resource providers in text form.",
    shortDescription="List all the resource providers in the system.",
    bindings = @ServiceBinding(type = BindingType.PATH, 
        bindings = "/system/rp"
    ), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {
                 "Lists the available resource providers responding with text/plain ",
                 "<pre>" +
                 "curl http://localhost:8080/system/rp\n" +
                 "Virtual Resource Provieder = org.sakaiproject.kernel.resource.VirtualResourceProviderImpl@45e7c2\n"+
                 "Virtual Resource Provieder Typess = [sakai/personalPrivate, sakai/contactstore, sakai/messagestore, " +
                 "sakai/groupPublic, sakai/files, sakai/personalPublic]\n"+
                 "Virtual Resource Provieder Types Map = {sakai/personalPrivate=org.sakaiproject.kernel.personal.resource." +
                 "PersonalResourceTypeProvider@27f9d5, sakai/contactstore=org.sakaiproject.kernel.connections.resource." +
                 "ConnectionResourceTypeProvider@833478, sakai/messagestore=org.sakaiproject.kernel.message.resource." +
                 "MessageResourceTypeProvider@e9e345, sakai/groupPublic=org.sakaiproject.kernel.personal.resource." +
                 "GroupPublicResourceTypeProvider@47d6de, sakai/files=org.sakaiproject.kernel.files.resource." +
                 "MessageResourceTypeProvider@6b8fa1, sakai/personalPublic=org.sakaiproject.kernel.personal.resource." +
                 "PublicResourceTypeProvider@a2c812}\n"+
                 "Virtual Resource Provieder Types List = 0\n"+
                 "Virtual Resource Provieder Types List = []\n"+
                 "</pre>"
         },
        response = {
            @ServiceResponse(code=200,description="On sucess a response simular to above."),
            @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class VirtualResourceProviderServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 3426715052386292398L;


  @Reference
  public transient VirtualResourceProvider virtualResourceProvider;

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
