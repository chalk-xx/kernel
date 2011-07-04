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
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.resource.SafeMethodsHandlingServlet;
import org.sakaiproject.nakamura.api.resource.SafeServletResourceHandler;

/**
 * Gets a version
 */

@ServiceDocumentation(name = "List Versions Servlet", okForVersion = "0.11",
  description = "Lists versions of a resource in json format",
  shortDescription = "List versions of a resource",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE,
      bindings = { "sling/servlet/default" },
      selectors = @ServiceSelector(name = "versions", description = "Retrieves a paged list of versions for the resource"),
      extensions = @ServiceExtension(name = "json", description = "A list over versions in json format"))
  },
  methods = @ServiceMethod(name = "GET",
    description = {
      "Lists previous versions of a resource. The url is of the form "
          + "http://host/resource.versions.json ",
      "Example<br>"
          + "<pre>curl http://localhost:8080/p/hziUdUqgaa.versions.json</pre>"
    },
    parameters = {
      @ServiceParameter(name = "items", description = "The number of items per page"),
      @ServiceParameter(name = "page", description = "The page to of items to return")
    },
    response = {
      @ServiceResponse(code = 200, description = "Success a body is returned containing a json tree"),
      @ServiceResponse(code = 404, description = "Resource was not found."),
      @ServiceResponse(code = 500, description = "Failure with HTML explanation.")
    }))
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "GET", selectors = "versions", extensions = "json")
@References(value = { @Reference(referenceInterface = SafeServletResourceHandler.class, name = "resourceHandlers", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, target = "(handling.servlet=ListVersionsServlet)", bind = "bindServletResourceHandler", unbind = "unbindServletResourceHandler") })
public class ListVersionsServlet extends SafeMethodsHandlingServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -8625094021909795537L;

  @Override
  protected void bindServletResourceHandler(SafeServletResourceHandler handler) {
    super.bindServletResourceHandler(handler);
  }

  @Override
  protected void unbindServletResourceHandler(SafeServletResourceHandler handler) {
    super.unbindServletResourceHandler(handler);
  }
}
