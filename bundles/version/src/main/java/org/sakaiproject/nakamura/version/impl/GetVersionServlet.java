/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.version.impl;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.resource.SafeMethodsHandlingServlet;
import org.sakaiproject.nakamura.api.resource.SafeServletResourceHandler;

/**
 * Outputs a version
 */
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "GET", selectors = "version")
@ServiceDocumentation(name = "Get Version Servlet", okForVersion = "0.11",
  description = "Gets a previous version of a resource",
  shortDescription = "Get a version of a resource",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE,
      bindings = { "sling/servlet/default" },
      selectors = @ServiceSelector(name = "version", description = "Retrieves a named version of a resource, the version specified in the URL"),
      extensions = @ServiceExtension(name = "*", description = "All selectors available in SLing (json, html, xml)"))
  },
  methods = @ServiceMethod(name = "GET",
    description = {
      "Gets a previous version of a resource. The url is of the form "
          + "http://localhost:8080/someresource.version.,versionnumber,.json "
          + " where versionnumber is the version number of version to be retrieved. Note that the , "
          + "at the start and end of versionnumber"
          + " delimit the version number. Once the version of the node requested has been extracted the request "
          + " is processed as for other Sling requests ",
      "Example<br>"
          + "<pre>curl http://localhost:8080/p/hziUdUqgaa.version.,1.1,/airplane-stamp.png</pre>"
    },
    response = {
      @ServiceResponse(code = 200, description = "Success a body is returned"),
      @ServiceResponse(code = 400, description = "If the version name is not known."),
      @ServiceResponse(code = 404, description = "Resource was not found."),
      @ServiceResponse(code = 500, description = "Failure with HTML explanation.")
    }))
@References(value = { @Reference(referenceInterface = SafeServletResourceHandler.class, name = "resourceHandlers", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, target = "(handling.servlet=GetVersionServlet)", bind = "bindServletResourceHandler", unbind = "unbindServletResourceHandler") })
public class GetVersionServlet extends SafeMethodsHandlingServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 2174722854412826398L;

  @Override
  protected void bindServletResourceHandler(SafeServletResourceHandler handler) {
    super.bindServletResourceHandler(handler);
  }

  @Override
  protected void unbindServletResourceHandler(SafeServletResourceHandler handler) {
    super.unbindServletResourceHandler(handler);
  }

}
