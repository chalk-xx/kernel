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
package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.kernel.util.URIExpander;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service
 * @scr.property name="sling.post.operation" value="addProperty"
 * @scr.reference name="URIExpander" interface="org.sakaiproject.kernel.util.URIExpander"
 */
public class AddPropertyOperation extends AbstractPropertyOperationModifier {
  
  private URIExpander uriExpander;

  protected void bindURIExpander(URIExpander uriExpander) {
    this.uriExpander = uriExpander;
  }

  protected void unbindURIExpander(URIExpander uriExpander) {
    this.uriExpander = null;
  }
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    doModify(request, response, changes, uriExpander);
  }

}
