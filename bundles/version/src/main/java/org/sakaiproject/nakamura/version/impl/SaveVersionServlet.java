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
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.resource.AllMethodsHandlingServlet;
import org.sakaiproject.nakamura.api.resource.ServletResourceHandler;

/**
 * Saves the current version of the JCR node identified by the Resource and checks out a
 * new writeable version.
 */

@ServiceDocumentation(name = "Save a version Servlet", description = "Saves a new version of a resource", shortDescription = "List versions of a resource", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = {
    "sling/servlet/default", "selector save" }, selectors = @ServiceSelector(name = "save", description = "Saves  the current version of a resource creating a new version."), extensions = @ServiceExtension(name = "json", description = "A json tree containing the name of the saved version.")), methods = @ServiceMethod(name = "POST", description = {
    "Lists previous versions of a resource. The url is of the form "
        + "http://host/resource.save.json ",
    "Example<br>" + "<pre>curl http://localhost:8080/sresource/resource.save.json</pre>" }, response = {
    @ServiceResponse(code = 200, description = "Success a body is returned containing a json ove the name of the version saved"),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "POST", selectors = "save", extensions = "json")
@References(value = { @Reference(referenceInterface = ServletResourceHandler.class, name = "resourceHandlers", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, target = "(handling.servlet=SaveVersionServlet)", bind = "bindServletResourceHandler", unbind = "unbindServletResourceHandler") })
public class SaveVersionServlet extends AllMethodsHandlingServlet {

  
  
  /**
   * 
   */
  private static final long serialVersionUID = 6294923392943921667L;

  @Override
  protected void bindServletResourceHandler(ServletResourceHandler handler) {
    super.bindServletResourceHandler(handler);
  }

  @Override
  protected void unbindServletResourceHandler(ServletResourceHandler handler) {
    super.unbindServletResourceHandler(handler);
  }

}
