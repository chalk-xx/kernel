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
package org.sakaiproject.kernel.files.search;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import java.security.AccessControlException;
import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Formats the files search results.
 * 
 * @scr.component immediate="true" label="FileSearchResultProcessor"
 *                description="Formatter for file searches"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Files"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class FileSearchResultProcessor implements SearchResultProcessor {

  public void writeNode(JSONWriter write, Node node) throws JSONException,
      RepositoryException {

    Session session = node.getSession();
    if (node.hasProperty("sling:internalRedirect")) {
      // This node points to another node.
      // Get that one.
      String path = node.getProperty("sling:internalRedirect").getString();
      if (session.itemExists(path)) {
        Node n = (Node) session.getItem(path);
        writeFileNode(write, n, session);
      }
    } else {
      writeFileNode(write, node, session);
    }
  }

  /**
   * Output the file node in json format.
   * 
   * @param write
   * @param node
   * @param session
   * @throws JSONException
   * @throws RepositoryException
   */
  private void writeFileNode(JSONWriter write, Node node, Session session)
      throws JSONException, RepositoryException {
    write.object();
    // dump all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);

    write.key("filename");
    write.value(node.getName());
    write.key("url");
    String id = node.getProperty(FilesConstants.SAKAI_ID).getString();
    write.value(FilesConstants.USER_FILESTORE + "/" + id + "/" + node.getName());

    String path = node.getPath();

    write.key("permissions");
    write.object();
    write.key("set_property");
    write.value(hasPermission(session, path, "set_property"));
    write.key("read");
    write.value(hasPermission(session, path, "read"));
    write.key("remove");
    write.value(hasPermission(session, path, "remove"));
    write.endObject();
    
    write.endObject();
  }

  /**
   * Checks if the current user has a permission on a path.
   * 
   * @param session
   * @param path
   * @param permission
   * @return
   */
  private boolean hasPermission(Session session, String path, String permission) {
    try {
      session.checkPermission(path, permission);
      return true;
    } catch (AccessControlException e) {
      return false;
    } catch (RepositoryException e) {
      return false;
    }
  }

}
