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
package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.AbstractSparsePostOperation;
import org.sakaiproject.nakamura.api.resource.lite.LiteJsonImporter;
import org.sakaiproject.nakamura.api.resource.lite.SparseNonExistingResource;
import org.sakaiproject.nakamura.resource.lite.servlet.post.SparseCreateServlet;

import java.io.IOException;
import java.util.List;

public class CreateTreeOperation extends AbstractSparsePostOperation {

  private static final long serialVersionUID = 9207596135556346980L;
  public static final String TREE_PARAM = "tree";
  public static final String DELETE_PARAM = "delete";

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes, String contentPath)
      throws StorageClientException, AccessDeniedException, IOException {
    // Check parameters
    RequestParameter treeParam = request.getRequestParameter(TREE_PARAM);
    RequestParameter deleteParam = request.getRequestParameter(DELETE_PARAM);
    JSONObject json = null;

    // If there is no tree parameter we ignore the entire thing
    if (treeParam == null) {
      throw new StorageClientException("No " + TREE_PARAM + " parameter found.");
    }

    // Get the json object.
    try {
      json = new JSONObject(treeParam.getString());
    } catch (JSONException e) {
      throw new StorageClientException("Invalid JSON tree structure");
    }

    Resource resource = request.getResource();
    String path = null;
    {
      if ( resource instanceof SparseNonExistingResource ) {
        path = ((SparseNonExistingResource) resource).getTargetContentPath();
      } else {
        Content content = resource.adaptTo(Content.class);
        if (content != null) {
          path = content.getPath();
        } else {
          // for some reason, if the operation posts to a path that does not exist and the
          // operation is create tree the SparseCreateServlet.doPost() operation is
          // bypassed. This is an ugly fix that works wound that problem. I suspect
          // somethings up in Sling.
          path = (String) request
              .getAttribute(SparseCreateServlet.CONTENT_TARGET_PATH_ATTRIBUTE);
        }
      }
    }
    if (path == null) {
      throw new StorageClientException("No Path suppleid to create json tree at");
    }

    // Check if we want to delete the entire tree before creating a new node.
    if (deleteParam != null && "1".equals(deleteParam.getString())) {
      contentManager.delete(path);
    }

    // Start creating the tree.
    try {
      LiteJsonImporter simpleJSONImporter = new LiteJsonImporter();
      simpleJSONImporter.importContent(contentManager, json, path, false, false, false);
    } catch (JSONException e) {
      throw new StorageClientException(e.getMessage(), e);
    }

  }


}
