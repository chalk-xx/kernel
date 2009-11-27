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
package org.sakaiproject.kernel.api.files;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.ACLUtils;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

// TODO: Javadoc
public class FileUtils {

  public static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  /**
   * Save a file.
   * 
   * @param session
   * @param path
   * @param id
   * @param file
   * @param contentType
   * @param slingRepository
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  public static Node saveFile(Session session, String path, String id,
      RequestParameter file, String contentType, SlingRepository slingRepository)
      throws RepositoryException, IOException {

    // Get the nescecary parameters
    InputStream is = file.getInputStream();
    String fileName = file.getFileName();

    if (fileName != null && !fileName.equals("")) {
      // Clean the filename.

      log.info("Trying to save file {} to {} for user {}", new Object[] { fileName, path,
          session.getUserID() });

      // Create or get the file.
      Node fileNode = JcrUtils.deepGetOrCreateNode(session, path, JcrConstants.NT_FILE);

      Node content = null;
      // If this is a new node then we have to add certain things.
      if (fileNode.isNew()) {
        // Make sure we can reference this node.
        if (fileNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
          fileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }
        fileNode.addMixin("sakai:propertiesmix");
        fileNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            FilesConstants.RT_SAKAI_FILE);
        fileNode.setProperty(FilesConstants.SAKAI_ID, id);

        // Create the content node.
        content = fileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        content.setProperty(JcrConstants.JCR_DATA, is);
        content.setProperty(JcrConstants.JCR_MIMETYPE, contentType);
        content.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());

        log.info(Calendar.getInstance().toString());
        
        if (session.hasPendingChanges()) {
          session.save();
        }

        // TODO: Check that this isnt a fix for the Owner dynamic principal not working.
        // Make sure that the user can edit it later on.
        Session adminSession = null;
        try {

          adminSession = slingRepository.loginAdministrative(null);
          Authorizable authorizable = AccessControlUtil.getUserManager(adminSession)
              .getAuthorizable(session.getUserID());

          ACLUtils.addEntry(path, authorizable, adminSession, "g:" + Privilege.JCR_ALL);

          if (adminSession.hasPendingChanges()) {
            adminSession.save();
          }
        } finally {
          if (adminSession != null)
            adminSession.logout();
        }

      } else {
        // This is not a new node, so we should already have a content node.
        // Just in case.. catch it
        try {
          content = fileNode.getNode(JcrConstants.JCR_CONTENT);
        } catch (PathNotFoundException pnfe) {
          content = fileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        }

        content.setProperty(JcrConstants.JCR_DATA, is);
        content.setProperty(JcrConstants.JCR_MIMETYPE, contentType);
        content.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());

      }

      // Set the person who last modified it.s
      fileNode.setProperty(FilesConstants.SAKAI_USER, session.getUserID());

      fileNode.setProperty("sakai:filename", fileName);

      if (session.hasPendingChanges()) {
        session.save();
      }
      return fileNode;
    }
    return null;
  }

  /**
   * Create a link to a file.
   * 
   * @param session
   *          The current session.
   * @param fileNode
   *          The node for the file we are linking to.
   * @param linkPath
   *          The absolute path were the node should be created that will contain the
   *          link.
   * @throws RepositoryException
   */
  public static String createLink(Session session, Node fileNode, String linkPath,
      String sitePath, SlingRepository slingRepository) throws RepositoryException {
    String fileUUID = fileNode.getUUID();
    Node linkNode = JcrUtils.deepGetOrCreateNode(session, linkPath);
    // linkNode.addMixin("sakai:propertiesmix");
    linkNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        FilesConstants.RT_SAKAI_LINK);
    String fileName = fileNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();
    linkNode.setProperty(FilesConstants.SAKAI_FILENAME, fileName);
    String uri = FileUtils.getDownloadPath(fileNode);
    linkNode.setProperty(FilesConstants.SAKAI_LINK, "jcrinternal:" + uri);
    linkNode.setProperty("jcr:reference", fileUUID);

    // Make sure we can reference this node.
    if (linkNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
      linkNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
    }
    // Save the linkNode.
    if (session.hasPendingChanges()) {
      session.save();
    }

    Session adminSession = null;
    try {
      // Login as admin.
      // This way we can set an ACL on the fileNode even if it is read only.
      adminSession = slingRepository.loginAdministrative(null);

      // Get the node trough the admin session.
      Node adminFileNode = adminSession.getNodeByUUID(fileUUID);

      addValue(adminFileNode, "jcr:reference", linkNode.getUUID());
      addValue(adminFileNode, "sakai:sites", sitePath);
      addValue(adminFileNode, "sakai:linkpaths", linkPath);

      // Save the reference.
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
    } finally {
      if (adminSession != null)
        adminSession.logout();
    }

    return linkNode.getPath();

  }

  /**
   * Add a value to to a multi-valued property.
   * 
   * @param adminFileNode
   * @param property
   * @param value
   * @throws RepositoryException
   */
  private static void addValue(Node adminFileNode, String property, String value)
      throws RepositoryException {
    // Add a reference on the fileNode.
    Value[] references = JcrUtils.getValues(adminFileNode, property);
    if (references.length == 0) {
      Value[] vals = { ValueHelper.convert(value, PropertyType.STRING, ValueFactoryImpl
          .getInstance()) };
      adminFileNode.setProperty(property, vals);
    } else {
      Value[] newReferences = new Value[references.length + 1];
      for (int i = 0; i < references.length; i++) {
        newReferences[i] = references[i];
      }
      newReferences[references.length] = ValueHelper.convert(value, PropertyType.STRING,
          ValueFactoryImpl.getInstance());
      adminFileNode.setProperty(property, newReferences);
    }
  }

  public static String getHashedPath(String store, String id) {
    return PathUtils.toInternalHashedPath(store, id, "");
  }

  /**
   * Get the download path.
   * 
   * @param store
   * @param id
   * @return
   */
  public static String getDownloadPath(String store, String id) {
    return store + "/" + id;
  }

  /**
   * Looks at a sakai/file node and returns the download path for it.
   * 
   * @param node
   * @return
   * @throws RepositoryException
   */
  public static String getDownloadPath(Node node) throws RepositoryException {
    Session session = node.getSession();
    String path = node.getPath();
    String store = findStore(path, session);

    if (node.hasProperty(FilesConstants.SAKAI_ID)) {
      String id = node.getProperty(FilesConstants.SAKAI_ID).getString();
      return getDownloadPath(store, id);
    }

    return path;
  }

  /**
   * Looks at a path and returns the store (or null if none is found)
   * 
   * @param path
   * @param session
   * @return
   * @throws RepositoryException
   */
  public static String findStore(String path, Session session) throws RepositoryException {

    if (session.itemExists(path)) {
      Node node = (Node) session.getItem(path);
      while (!node.getPath().equals("/")) {
        if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
                .getString().equals(FilesConstants.RT_FILE_STORE)) {
          return node.getPath();
        }

        node = node.getParent();

      }
    }

    return null;
  }

  /**
   * Writes all the properties of a sakai/file node. Also checks what the permissions are
   * for a session and where the links are.
   * 
   * @param node
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeFileNode(Node node, Session session, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    write.object();
    // dump all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    // The permissions for this session.
    writePermissions(node, session, write);

    // The download path to this file.
    write.key("path");
    write.value(FileUtils.getDownloadPath(node));

    if (node.hasNode(JcrConstants.JCR_CONTENT)) {
      Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
      write.key(JcrConstants.JCR_LASTMODIFIED);
      Calendar cal = contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
      write.value(FilesConstants.DATEFORMAT.format(cal));
      write.key(FilesConstants.SAKAI_MIMETYPE);
      write.value(contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString());

      if (contentNode.hasProperty(JcrConstants.JCR_DATA)) {
        write.key("filesize");
        write.value(contentNode.getProperty(JcrConstants.JCR_DATA).getLength());
      }
    }

    // Get all the sites where this file is referenced.
    getSites(node, write, siteService);

    write.endObject();
  }

  /**
   * Writes all the properties for a linked node.
   * 
   * @param node
   * @param write
   * @param siteService
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeLinkNode(Node node, Session session, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    write.object();
    // Write all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    // The name of this file.
    write.key("name");
    write.value(node.getName());
    // Download path.
    write.key("path");
    write.value(node.getPath());
    // permissions
    writePermissions(node, session, write);

    // Write the actual file.
    if (node.hasProperty("jcr:reference")) {
      String uuid = node.getProperty("jcr:reference").getString();
      write.key("file");
      try {
        Node fileNode = session.getNodeByUUID(uuid);
        writeFileNode(fileNode, session, write, siteService);
      } catch (ItemNotFoundException e) {
        write.value(false);
      }
    }

    write.endObject();
  }

  /**
   * Gives the permissions for this user.
   * 
   * @param node
   * @param session
   * @param write
   * @throws RepositoryException
   * @throws JSONException
   */
  private static void writePermissions(Node node, Session session, JSONWriter write)
      throws RepositoryException, JSONException {
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
  }

  /**
   * Checks if the current user has a permission on a path.
   * 
   * @param session
   * @param path
   * @param permission
   * @return
   */
  private static boolean hasPermission(Session session, String path, String permission) {
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
  private static void getSites(Node node, JSONWriter write, SiteService siteService)
      throws RepositoryException, JSONException {

    write.key("usedIn");
    write.object();
    write.key("sites");
    write.array();

    Value[] sites = JcrUtils.getValues(node, "sakai:sites");
    Session session = node.getSession();

    int total = 0;
    try {
      List<String> handledSites = new ArrayList<String>();
      for (Value v : sites) {
        String path = v.getString();
        if (!handledSites.contains(path)) {
          handledSites.add(path);
          Node siteNode = (Node) session.getItem(v.getString());

          AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
          Privilege read = acm.privilegeFromName(Privilege.JCR_READ);
          Privilege[] privs = new Privilege[] { read };
          boolean hasAccess = acm.hasPrivileges(path, privs);
          if (siteService.isSite(siteNode) && hasAccess) {
            writeSiteInfo(siteNode, write, siteService);
            total++;
          }
        }
      }
    } catch (Exception e) {
      // We ignore every exception it has when looking up sites.
      // it is dirty ..
      log.info("Catched exception when looking up used sites for a file.");
      e.printStackTrace();
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
  private static void writeSiteInfo(Node siteNode, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(siteNode)));
    write.key("path");
    write.value(siteNode.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, siteNode);
    write.endObject();
  }
}
