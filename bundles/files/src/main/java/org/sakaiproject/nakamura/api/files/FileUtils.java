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
package org.sakaiproject.nakamura.api.files;

import static org.sakaiproject.nakamura.api.files.FilesConstants.REQUIRED_MIXIN;
import static org.sakaiproject.nakamura.api.files.FilesConstants.RT_SAKAI_LINK;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_LINK;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAGS;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.DateUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Some utility function regarding file management.
 */
public class FileUtils {

  public static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  /**
   * Create a link to a file. There is no need to call a session.save, the change is
   * persistent.
   *
   * @param fileNode
   *          The node that represents the file. This node has to be retrieved via the
   *          normal user his {@link Session session}. If the userID equals
   *          {@link UserConstants.ANON_USERID} an AccessDeniedException will be thrown.
   * @param linkPath
   *          The absolute path in JCR where the link should be placed.
   * @param slingRepository
   *          The {@link SlingRepository} to use to login as an administrative.
   * @return The newly created node.
   * @throws AccessDeniedException
   *           When the user is anonymous.
   * @throws RepositoryException
   *           Something else went wrong.
   */
  public static boolean createLink(Node fileNode, String linkPath,
      SlingRepository slingRepository) throws AccessDeniedException, RepositoryException {
    Session session = fileNode.getSession();
    String userId = session.getUserID();
    if (UserConstants.ANON_USERID.equals(userId)) {
      throw new AccessDeniedException();
    }

    boolean hasMixin = JcrUtils.hasMixin(fileNode, REQUIRED_MIXIN)
        && fileNode.canAddMixin(REQUIRED_MIXIN);
    // If the fileNode doesn't have the required referenceable mixin, we need to set it.
    if (!hasMixin) {
      // The required mixin is not on the node.
      // Set it.
      Session adminSession = null;
      try {
        adminSession = slingRepository.loginAdministrative(null);

        // Grab the node via the adminSession
        String path = fileNode.getPath();
        Node adminFileNode = (Node) adminSession.getItem(path);
        if (!hasMixin) {
          adminFileNode.addMixin(REQUIRED_MIXIN);
        }

        if (adminSession.hasPendingChanges()) {
          adminSession.save();
        }
      } finally {
        adminSession.logout();
      }
    }

    // Now that the file is referenceable, it has a uuid.
    // Use it for the link.
    // Grab the (updated) node via the user's session id.
    fileNode = (Node) session.getItem(fileNode.getPath());

    // Create the link
    Node linkNode = JcrUtils.deepGetOrCreateNode(session, linkPath);
    if (!"sling:Folder".equals(linkNode.getPrimaryNodeType().getName())) {
      // sling folder allows single and multiple properties, no need for the mixin.
      if (linkNode.canAddMixin(REQUIRED_MIXIN)) {
        linkNode.addMixin(REQUIRED_MIXIN);
      }
    }
    linkNode
        .setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, RT_SAKAI_LINK);
    linkNode.setProperty(SAKAI_LINK, fileNode.getIdentifier());

    // Save link.
    if (session.hasPendingChanges()) {
      session.save();
    }

    return true;
  }

  public static boolean createLink(Content content, String link,
      org.sakaiproject.nakamura.api.lite.Session session) throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException, StorageClientException {
    String userId = session.getUserId();
    if (User.ANON_USER.equals(userId)) {
      throw new org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException(
          Security.ZONE_CONTENT, link, "Cant create a link", userId);
    }
    ContentManager contentManager = session.getContentManager();
    Content linkNode = contentManager.get(link);
    if (linkNode == null) {
      linkNode = new Content(link, ImmutableMap.of(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          (Object) RT_SAKAI_LINK, SAKAI_LINK,
          content.getPath()));
    } else {
      linkNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          RT_SAKAI_LINK);
      linkNode.setProperty(SAKAI_LINK, content.getPath());
    }
    contentManager.update(linkNode);
    return true;
  }

  /**
   * Writes all the properties of a sakai/file node. Also checks what the permissions are
   * for a session and where the links are.<br/>
   * Same as calling {@link #writeFileNode(Node, Session, JSONWriter, 0)}
   *
   * @param node
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeFileNode(Node node, Session session, JSONWriter write)
      throws JSONException, RepositoryException {
    writeFileNode(node, session, write, 0);
  }

  public static void writeFileNode(Content content,
      org.sakaiproject.nakamura.api.lite.Session session, JSONWriter write)
      throws JSONException, StorageClientException {
    writeFileNode(content, session, write, 0);
  }

  /**
   * Writes all the properties of a sakai/file node. Also checks what the permissions are
   * for a session and where the links are.
   *
   * @param node
   * @param write
   * @param objectInProgress
   *          Whether object creation is in progress. If false, object is started and
   *          ended in this method call.
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeFileNode(Node node, Session session, JSONWriter write,
      int maxDepth) throws JSONException, RepositoryException {

    write.object();

    // dump all the properties.
    ExtendedJSONWriter.writeNodeTreeToWriter(write, node, true, maxDepth);
    // The permissions for this session.
    writePermissions(node, session, write);

    if (node.hasNode(JcrConstants.JCR_CONTENT)) {
      Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
      write.key(JcrConstants.JCR_LASTMODIFIED);
      Calendar cal = contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
      write.value(DateUtils.iso8601(cal));
      write.key(JcrConstants.JCR_MIMETYPE);
      write.value(contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString());

      if (contentNode.hasProperty(JcrConstants.JCR_DATA)) {
        write.key(JcrConstants.JCR_DATA);
        write.value(contentNode.getProperty(JcrConstants.JCR_DATA).getLength());
      }
    }

    write.endObject();
  }

  public static void writeFileNode(Content content,
      org.sakaiproject.nakamura.api.lite.Session session, JSONWriter write, int maxDepth)
      throws JSONException, StorageClientException {
    write.object();

    // dump all the properties.
    ExtendedJSONWriter.writeContentTreeToWriter(write, content, true, maxDepth);
    // The permissions for this session.
    writePermissions(content, session, write);

    write.key(JcrConstants.JCR_LASTMODIFIED);
    Calendar cal = new GregorianCalendar();
    cal.setTimeInMillis(StorageClientUtils.toLong(content.getProperty(Content.LASTMODIFIED_FIELD)));
    write.value(DateUtils.iso8601(cal));
    write.key(JcrConstants.JCR_MIMETYPE);
    write.value(content.getProperty(Content.MIMETYPE_FIELD));
    write.key(JcrConstants.JCR_DATA);
    write.value(StorageClientUtils.toLong(content.getProperty(Content.LENGTH_FIELD)));
    write.endObject();
  }

  /**
   * Writes all the properties for a linked node.
   *
   * @param node
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeLinkNode(Node node, Session session, JSONWriter write)
      throws JSONException, RepositoryException {
    write.object();
    // Write all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    // permissions
    writePermissions(node, session, write);

    // Write the actual file.
    if (node.hasProperty(SAKAI_LINK)) {
      String uuid = node.getProperty(SAKAI_LINK).getString();
      write.key("file");
      try {
        Node fileNode = session.getNodeByIdentifier(uuid);
        writeFileNode(fileNode, session, write);
      } catch (ItemNotFoundException e) {
        write.value(false);
      }
    }

    write.endObject();
  }

  public static void writeLinkNode(Content content,
      org.sakaiproject.nakamura.api.lite.Session session, JSONWriter writer)
      throws StorageClientException, JSONException {
    ContentManager contentManager = session.getContentManager();

    writer.object();

    // Write all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(writer, content);

    // permissions
    writePermissions(content, session, writer);

    // Write the actual file.
    if (content.hasProperty(SAKAI_LINK)) {
      String linkPath = (String) content.getProperty(SAKAI_LINK);
      writer.key("file");
      try {
        Content fileNode = contentManager.get(linkPath);
        writeFileNode(fileNode, session, writer);
      } catch (org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException e) {
        writer.value(false);
      }
    }

    writer.endObject();
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

  private static void writePermissions(Content content,
      org.sakaiproject.nakamura.api.lite.Session session, JSONWriter writer)
      throws StorageClientException, JSONException {
    AccessControlManager acm = session.getAccessControlManager();
    String path = content.getPath();

    writer.key("permissions");
    writer.object();
    writer.key("set_property");
    // TODO does CAN_WRITE == set_property -CFH : yes, ieb
    // TODO: make this a bit more efficient, checking permissions one by one is going to rely on
    //       caching to make it efficient. It would be better to get the permissions bitmap and then
    //       check it to see what has been set. That might require a niew methods in the AccessControl
    //       manager API.
    writer.value(hasPermission(acm, path, Permissions.CAN_WRITE));
    writer.key("read");
    writer.value(hasPermission(acm, path, Permissions.CAN_READ));
    writer.key("remove");
    writer.value(hasPermission(acm, path, Permissions.CAN_DELETE));
    writer.endObject();
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

  private static boolean hasPermission(AccessControlManager acm, String path,
      Permission permission) {
    try {
      acm.check(Security.ZONE_CONTENT, path, permission);
      return true;
    } catch (org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException e) {
      return false;
    } catch (StorageClientException e) {
      return false;
    }
  }

  /**
   * Check if a node is a proper sakai tag.
   *
   * @param node
   *          The node to check if it is a tag.
   * @return true if the node is a tag, false if it is not.
   * @throws RepositoryException
   */
  public static boolean isTag(Node node) throws RepositoryException {
    if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
        && FilesConstants.RT_SAKAI_TAG.equals(node.getProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
      return true;
    }
    return false;
  }

  /**
   * Add's a tag on a node. If the tag has a name defined in the {@link Property property}
   * sakai:tag-name it will be added in the fileNode as well.
   *
   * @param adminSession
   *          The session that can be used to modify the fileNode.
   * @param fileNode
   *          The node that needs to be tagged.
   * @param tagNode
   *          The node that represents the tag.
   */
  public static boolean addTag(Session adminSession, Node fileNode, Node tagNode)
      throws RepositoryException {
    if (fileNode == null) {
      throw new RuntimeException(
          "Cant tag non existant nodes, sorry, both must exist prior to tagging. File:"
              + fileNode);
    }
    // Grab the node via the adminSession
    String path = fileNode.getPath();
    fileNode = (Node) adminSession.getItem(path);

    // Check if the mixin is on the node.
    // This is nescecary for nt:file nodes.

    return addTag(adminSession, fileNode, getTags(tagNode));
  }

  public static boolean addTag(ContentManager contentManager, Content contentNode,
      Node tagNode)
      throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      StorageClientException, RepositoryException {
    if (contentNode == null) {
      throw new RuntimeException(
          "Cant tag non existant nodes, sorry, both must exist prior to tagging. File:"
              + contentNode);
    }
    return addTag(contentManager, contentNode, getTags(tagNode));
  }
  
  public static boolean addTag(ContentManager contentManager, Content contentNode,
      Content tagNode)
      throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      StorageClientException, RepositoryException {
    if (contentNode == null) {
      throw new RuntimeException(
          "Cant tag non existant nodes, sorry, both must exist prior to tagging. File:"
              + contentNode);
    }
    return addTag(contentManager, contentNode, getTags(tagNode));
  }

  private static boolean addTag(Session adminSession, Node fileNode, String[] tags)
      throws RepositoryException {
    boolean added = false;
    if (!JcrUtils.hasMixin(fileNode, REQUIRED_MIXIN)) {
      if (fileNode.canAddMixin(REQUIRED_MIXIN)) {
        fileNode.addMixin(REQUIRED_MIXIN);
      }
    }
    if (JcrUtils.addUniqueValue(adminSession, fileNode, SAKAI_TAG_UUIDS, tags[0],
        PropertyType.STRING)) {
      added = true;
    }
    if (JcrUtils.addUniqueValue(adminSession, fileNode, SAKAI_TAGS, tags[1],
        PropertyType.STRING)) {
      added = true;
    }
    return added;
  }

  private static String[] getTags(Node tagNode) throws RepositoryException {
    String tagUuid = tagNode.getIdentifier();
    String tagName = tagNode.getName();
    if (tagNode.hasProperty(SAKAI_TAGS)) {
      tagName = tagNode.getProperty(SAKAI_TAGS).getString();
    }
    return new String[] { tagUuid, tagName };
  }
  
  private static String[] getTags(Content tagNode) {
    String tagUuid = (String) tagNode.getProperty(Content.UUID_FIELD);
    String tagName = "";
    if (tagNode.hasProperty(SAKAI_TAGS)) {
      tagName = (String) tagNode.getProperty(SAKAI_TAGS);
    }
    return new String[] { tagUuid, tagName };
  }

  private static boolean addTag(ContentManager contentManager, Content content,
      String[] tags)
      throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      StorageClientException {
    boolean sendEvent = false;
    Map<String, Object> properties = content.getProperties();
    Set<String> uuidSet = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[]) properties
        .get(SAKAI_TAG_UUIDS)));
    if (!uuidSet.contains(tags[0])) {
      uuidSet.add(tags[0]);
      content.setProperty(SAKAI_TAG_UUIDS,
          uuidSet.toArray(new String[uuidSet.size()]));
      sendEvent = true;
    }
    Set<String> nameSet = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[]) properties
        .get(SAKAI_TAGS)));
    if (!nameSet.contains(tags[1])) {
      nameSet.add(tags[1]);
      content.setProperty(SAKAI_TAGS,
          nameSet.toArray(new String[nameSet.size()]));
      sendEvent = true;
    }

    if (sendEvent) {
      contentManager.update(content);
      return true;
    }
    return false;
  }

  /**
   * Delete a tag from a node.
   *
   * @param adminSession
   * @param fileNode
   * @param tagNode
   * @throws RepositoryException
   */
  public static void deleteTag(Session adminSession, Node fileNode, Node tagNode)
      throws RepositoryException {
    if (tagNode == null || fileNode == null) {
      throw new RuntimeException("Can't delete tag from non existent nodes. File:"
          + fileNode + " Node To Tag:" + tagNode);
    }
    // Grab the node via the adminSession
    String path = fileNode.getPath();
    fileNode = (Node) adminSession.getItem(path);

    // Add the reference from the tag to the node.
    deleteTag(adminSession, fileNode, getTags(tagNode));
  }

  /**
   * Delete a tag from a node.
   *
   * @param adminSession
   * @param fileNode
   * @param tagNode
   * @throws RepositoryException
   * @throws StorageClientException
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   */
  public static void deleteTag(ContentManager contentManager, Content contentNode,
      Node tagNode) throws RepositoryException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      StorageClientException {
    if (contentNode == null || contentNode == null) {
      throw new RuntimeException("Can't delete tag from non existent nodes. File:"
          + contentNode + " Node To Tag:" + tagNode);
    }
    // Add the reference from the tag to the node.
    deleteTag(contentManager, contentNode, getTags(tagNode));
  }

  private static void deleteTag(Session adminSession, Node fileNode, String[] tags)
      throws RepositoryException {
    JcrUtils.deleteValue(adminSession, fileNode, SAKAI_TAG_UUIDS, tags[0]);
    JcrUtils.deleteValue(adminSession, fileNode, SAKAI_TAGS, tags[1]);
  }

  private static boolean deleteTag(ContentManager contentManager, Content content,
      String[] tags)
      throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException,
      StorageClientException {
    boolean updated = false;
    Map<String, Object> properties = content.getProperties();
    Set<String> uuidSet = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[]) properties
        .get(SAKAI_TAG_UUIDS)));
    if (uuidSet.contains(tags[0])) {
      uuidSet.remove(tags[0]);
      content.setProperty(SAKAI_TAG_UUIDS,
          uuidSet.toArray(new String[uuidSet.size()]));
      updated = true;
    }
    Set<String> nameSet = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[]) properties
        .get(SAKAI_TAGS)));
    if (nameSet.contains(tags[1])) {
      nameSet.remove(tags[1]);
      content.setProperty(SAKAI_TAGS,
          nameSet.toArray(new String[nameSet.size()]));
      updated = true;
    }
    if (updated) {
      contentManager.update(content);
      return true;
    }
    return false;
  }

  /**
   * Resolves a Node given one of three possible passed parameters: 1) A fully qualified
   * path to a Node (e.g. "/foo/bar/baz"), 2) a Node's UUID, or 3) the PoolId from a
   * ContentPool.
   *
   * @param pathOrIdentifier
   *          One of three possible parameters: 1) A fully qualified path to a Node (e.g.
   *          "/foo/bar/baz"), 2) a Node's UUID, or 3) the PoolId from a ContentPool.
   * @param resourceResolver
   * @return If the Node cannot be resolved, <code>null</code> will be returned.
   * @throws IllegalArgumentException
   */
  public static Node resolveNode(final String pathOrIdentifier,
      final ResourceResolver resourceResolver) {
    if (pathOrIdentifier == null || "".equals(pathOrIdentifier)) {
      throw new IllegalArgumentException("Passed argument was null or empty");
    }
    if (resourceResolver == null) {
      throw new IllegalArgumentException("Resource resolver cannot be null");
    }
    Node node = null;
    try {
      if (pathOrIdentifier.startsWith("/")) { // it is a path specification
        node = resourceResolver.resolve(pathOrIdentifier).adaptTo(Node.class);
      } else {
        // Not a full resource path, so try the two flavors of ID.
        Session session = resourceResolver.adaptTo(Session.class);
        // First, assume we have a UUID and try to resolve
        try {
          node = session.getNodeByIdentifier(pathOrIdentifier);
        } catch (RepositoryException e) {
          log.debug("Swallowed exception; i.e. normal operation: {}",
              e.getLocalizedMessage(), e);
        }
        if (node == null) {
          log.warn("Unable to Tag Content Pool at this time, tried {} ", pathOrIdentifier);
          // must not have been a UUID; resolve via poolId
          // final String poolPath = CreateContentPoolServlet.hash(pathOrIdentifier);
          // node = session.getNode(poolPath);
        }
      }
    } catch (Throwable e) {
      log.error(e.getLocalizedMessage(), e);
    }
    return node;
  }

}
