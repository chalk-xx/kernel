/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sakaiproject.nakamura.resource.lite.servlet.operations;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

/**
 * The <code>ModifyOperation</code> class implements the default operation called by the
 * Sling default POST servlet if no operation is requested by the client. This operation
 * is able to create and/or modify content.
 */
public class ModifyOperation extends AbstractCreateOperation {

  private final DateParser dateParser;

  /**
   * handler that deals with file upload
   */
  private final SparseFileUploadHandler uploadHandler;

  public ModifyOperation(NodeNameGenerator defaultNodeNameGenerator,
      DateParser dateParser, ServletContext servletContext) {
    super(defaultNodeNameGenerator);
    this.dateParser = dateParser;
    this.uploadHandler = new SparseFileUploadHandler(servletContext);
  }

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes)  {

    Map<String, RequestProperty> reqProperties = collectContent(request, response);

    VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);


    for (RequestProperty property : reqProperties.values()) {
      if (property.hasRepositoryMoveSource()) {
        String from = property.getRepositorySource();
        String to = property.getPath();
        contentManager.move(from, to);
        changes.add(Modification.onMoved(from, to));
        property.setDelete(false);
      } else if (property.hasRepositoryCopySource()) {
        String from = property.getRepositorySource();
        String to = property.getPath();
        contentManager.copy(from, to);
        changes.add(Modification.onCopied(from, to));
        property.setDelete(false);
      } else if ( property.isDelete()) {
        contentManager.delete(property.getPath());
        changes.add(Modification.onDeleted(property.getPath()));
      }
    }

    
    SparsePropertyValueHandler propHandler = new SparsePropertyValueHandler(dateParser, changes);

    Map<String, Content> contentMap = Maps.newHashMap();
    for (RequestProperty prop : reqProperties.values()) {
      if (prop.hasValues()) {
        String path = prop.getParentPath();
        Content content = contentMap.get(path);
        if ( content == null  ) {
          content = contentManager.get(path);
          if ( content == null ) {
            content = new Content(path, null);
          }
          contentMap.put(path, content);
        }
        // skip jcr special properties
        if (prop.getName().equals("jcr:primaryType")
            || prop.getName().equals("jcr:mixinTypes")) {
          continue;
        }
        if (prop.isFileUpload()) {
          uploadHandler.setFile(content, contentManager, prop, changes);
        } else {
          propHandler.setProperty(content, prop);
        }
      }
    }

    for ( Content c : contentMap.values()) {
      contentManager.update(c);
    }
  }

  @Override
  protected String getItemPath(SlingHttpServletRequest request) {

    // calculate the paths
    StringBuffer rootPathBuf = new StringBuffer();
    String suffix;
    Resource currentResource = request.getResource();
    if (ResourceUtil.isSyntheticResource(currentResource)) {

      // no resource, treat the missing resource path as suffix
      suffix = currentResource.getPath();

    } else {

      // resource for part of the path, use request suffix
      suffix = request.getRequestPathInfo().getSuffix();

      // and preset the path buffer with the resource path
      rootPathBuf.append(currentResource.getPath());

    }

    // check for extensions or create suffix in the suffix
    boolean doGenerateName = false;
    if (suffix != null) {

      // cut off any selectors/extension from the suffix
      int dotPos = suffix.indexOf('.');
      if ((dotPos > 0) && (!(currentResource instanceof NonExistingResource))) {
        suffix = suffix.substring(0, dotPos);
      }

      // and check whether it is a create request (trailing /)
      if (suffix.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
        suffix = suffix.substring(0, suffix.length()
            - SlingPostConstants.DEFAULT_CREATE_SUFFIX.length());
        doGenerateName = true;

        // or with the star suffix /*
      } else if (suffix.endsWith(SlingPostConstants.STAR_CREATE_SUFFIX)) {
        suffix = suffix.substring(0, suffix.length()
            - SlingPostConstants.STAR_CREATE_SUFFIX.length());
        doGenerateName = true;
      }

      // append the remains of the suffix to the path buffer
      rootPathBuf.append(suffix);

    }

    String path = rootPathBuf.toString();

    if (doGenerateName) {
      try {
        path = generateName(request, path);
      } catch (RepositoryException re) {
        throw new SlingException("Failed to generate name", re);
      }
    }

    return path;
  }


}
