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

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
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
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 */
public class FileSearchResultProcessor implements SearchResultProcessor {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(FileSearchResultProcessor.class);
  private SiteService siteService;

  public void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

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
    String path = node.getPath();

    write.key("name");
    write.value(node.getName());

    write.key("permissions");
    write.object();
    write.key("set_property");
    write.value(hasPermission(session, path, "set_property"));
    write.key("read");
    write.value(hasPermission(session, path, "read"));
    write.key("remove");
    write.value(hasPermission(session, path, "remove"));
    write.endObject();

    String type = node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
        .getString();

    // If it is a file node we provide some extra properties.
    if (FilesConstants.RT_SAKAI_FILE.equals(type)) {
      // proper file
      // Sites where this file is used in.
      getSites(node, write);

      write.key("path");
      write.value(FileUtils.getDownloadPath(node));

    } else if (FilesConstants.RT_SAKAI_LINK.equals(type)) {
      // This is a linked file.
      write.key("path");
      write.value(node.getPath());
    } else if (FilesConstants.RT_SAKAI_FOLDER.equals(type)) {
      write.key("path");
      write.value(node.getPath());
    }

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

  /**
   * Gets all the sites where this file is used and parses the info for it.
   * 
   * @param node
   * @param write
   * @throws RepositoryException
   * @throws JSONException
   */
  private void getSites(Node node, JSONWriter write) throws RepositoryException,
      JSONException {

    write.key("usedIn");
    write.object();
    write.key("sites");
    write.array();
    PropertyIterator pi = node.getReferences();
    int total = 0;
    while (pi.hasNext()) {
      Property p = pi.nextProperty();
      Node parent = p.getParent(); // Get the node for this property.
      LOGGER.info(parent.getPath());
      
      // If it is a site service then we print it out.
      if (siteService.isSite(parent)) {
        writeSiteInfo(parent, write);
        total++;
      }
    }
    write.endArray();
    write.key("total");
    write.value(total);
    write.endObject();
  }

  /**
   * Parses the info for a site.
   * 
   * @param siteNode
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  private void writeSiteInfo(Node siteNode, JSONWriter write) throws JSONException,
      RepositoryException {
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(siteNode)));
    write.key("path");
    write.value(siteNode.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, siteNode);
    write.endObject();
  }

}
