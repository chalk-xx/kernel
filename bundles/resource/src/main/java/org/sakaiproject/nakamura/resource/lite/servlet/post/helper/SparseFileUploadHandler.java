/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.resource.lite.servlet.post.helper;

import com.google.common.collect.ImmutableMap;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.RequestProperty;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

/**
 * Handles file uploads.
 * <p/>
 * 
 * Simple example: <xmp> <form action="/home/admin" method="POST"
 * enctype="multipart/form-data"> <input type="file" name="./portrait" /> </form> </xmp>
 * 
 * this will create a nt:file node below "/home/admin" if the node type of "admin" is
 * (derived from) nt:folder, a nt:resource node otherwise.
 * <p/>
 * 
 * Filename example: <xmp> <form action="/home/admin" method="POST"
 * enctype="multipart/form-data"> <input type="file" name="./*" /> </form> </xmp>
 * 
 * same as above, but uses the filename of the uploaded file as name for the new node.
 * <p/>
 * 
 * Type hint example: <xmp> <form action="/home/admin" method="POST"
 * enctype="multipart/form-data"> <input type="file" name="./portrait" /> <input
 * type="hidden" name="./portrait@TypeHint" value="my:file" /> </form> </xmp>
 * 
 * this will create a new node with the type my:file below admin. if the hinted type
 * extends from nt:file an intermediate file node is created otherwise directly a resource
 * node.
 */
public class SparseFileUploadHandler {

  // nodetype name string constants
  public static final String NT_FOLDER = "nt:folder";
  public static final String NT_FILE = "nt:file";
  public static final String NT_RESOURCE = "nt:resource";
  public static final String NT_UNSTRUCTURED = "nt:unstructured";

  // item name string constants
  public static final String JCR_CONTENT = "jcr:content";
  public static final String JCR_LASTMODIFIED = "jcr:lastModified";
  public static final String JCR_MIMETYPE = "jcr:mimeType";
  public static final String JCR_ENCODING = "jcr:encoding";
  public static final String JCR_DATA = "jcr:data";

  /**
   * The servlet context.
   */
  private final ServletContext servletContext;

  /**
   * Constructs file upload handler
   * 
   * @param servletCtx
   *          the post processor
   */
  public SparseFileUploadHandler(ServletContext servletCtx) {
    this.servletContext = servletCtx;
  }

  /**
   * Uses the file(s) in the request parameter for creation of new nodes. if the parent
   * node is a nt:folder a new nt:file is created. otherwise just a nt:resource. if the
   * <code>name</code> is '*', the filename of the uploaded file is used.
   * 
   * @param parent
   *          the parent node
   * @param prop
   *          the assembled property info
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   * @throws IOException 
   * @throws RepositoryException
   *           if an error occurs
   */
  public void setFile(Content parent, ContentManager contentManager,
      RequestProperty prop, List<Modification> changes) throws AccessDeniedException, StorageClientException, IOException {
    RequestParameter[] values = prop.getValues();
    for (RequestParameter requestParameter : values) {
      RequestParameter value = requestParameter;

      // ignore if a plain form field or empty
      if (value.isFormField() || value.getSize() <= 0) {
        continue;
      }

      // get node name
      String name = prop.getName();
      if (name.equals("*")) {
        name = value.getFileName();
        // strip of possible path (some browsers include the entire path)
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.substring(name.lastIndexOf('\\') + 1);
      }
      name = Text.escapeIllegalJcrChars(name);

      if (parent.isNew()) {
        contentManager.update(parent);
      }

      // get content type
      String contentType = value.getContentType();
      if (contentType != null) {
        int idx = contentType.indexOf(';');
        if (idx > 0) {
          contentType = contentType.substring(0, idx);
        }
      }

      if (contentType == null || contentType.equals("application/octet-stream")) {
        // try to find a better content type
        contentType = this.servletContext.getMimeType(value.getFileName());
        if (contentType == null || contentType.equals("application/octet-stream")) {
          contentType = "application/octet-stream";
        }
      }

      String contentPath = StorageClientUtils.newPath(parent.getPath(), name);
      Content fileNode = contentManager.get(contentPath);
      if (fileNode == null) {
        fileNode = new Content(contentPath, ImmutableMap.of(Content.MIMETYPE_FIELD,(Object)contentType));
      } else {
        fileNode.setProperty(Content.MIMETYPE_FIELD, contentType);
      }

      contentManager.update(fileNode);

      contentManager.writeBody(fileNode.getPath(), value.getInputStream());
      changes.add(Modification.onModified(fileNode.getPath()));
    }
  }
}