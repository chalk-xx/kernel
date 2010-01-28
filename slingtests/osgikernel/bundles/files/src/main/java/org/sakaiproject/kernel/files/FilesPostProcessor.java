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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, enabled = true, name = "FilesPostProcessor", label = "FilesPostProcessor")
@Service(value = SlingPostProcessor.class)
public class FilesPostProcessor implements SlingPostProcessor {

  protected static final String IS_UPLOAD_PARAM = "sakai:files-is-upload";
  public static final Logger LOGGER = LoggerFactory
      .getLogger(FilesPostProcessor.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.SlingPostProcessor#process(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.List)
   */
  public void process(SlingHttpServletRequest request,
      List<Modification> changes) throws Exception {

    RequestParameter uploadParam = request.getRequestParameter(IS_UPLOAD_PARAM);
    if (uploadParam != null && "true".equalsIgnoreCase(uploadParam.getString())) {
      // This is a sakai file upload.
      // Make the uploaded file taggable and referenceable.
      // We add our mixin 'sakai:propertiesmix'.
      // This is a subtype if versionable which is a subtype of referenceable.

      String source = null;
      for (Modification m : changes) {
        if (m.getType() == ModificationType.CREATE) {
          String path = m.getSource();
          path = path.substring(path.lastIndexOf("/") + 1, path.length());
          if (path.equals("jcr:content")) {
            // The previous iteration was the actual nt:file.
            break;
          }
          source = m.getSource();
        }
      }

      if (source != null) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        // Make file referenceable/taggable.
        try {
          Node node = (Node) session.getItem(source);
          node.addMixin("sakai:propertiesmix");
          // The save will happen in the AbstractSlingPostOperation..
        } catch (RepositoryException e) {
          LOGGER.error("Unable to make an uploaded file referenceable.", e);
        }
      }
    }

  }
}
