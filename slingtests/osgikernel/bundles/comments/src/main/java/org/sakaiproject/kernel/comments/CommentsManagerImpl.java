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

package org.sakaiproject.kernel.comments;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.comments.CommentsConstants;
import org.sakaiproject.kernel.api.comments.CommentsException;
import org.sakaiproject.kernel.api.comments.CommentsManager;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.util.ACLUtils;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for the comments.
 * 
 * @scr.component immediate="true" label="Sakai Comments Manager"
 *                description="Service for doing operations with comments."
 *                name="org.sakaiproject.kernel.api.comments.CommentsManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.comments.CommentsManager"
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 *                bind="bindSiteService" unbind="unbindSiteService"
 * 
 */
public class CommentsManagerImpl implements CommentsManager {

  public static final Logger LOG = LoggerFactory
      .getLogger(CommentsManagerImpl.class);

  // References
  private SiteService siteService;

  public void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

  public Node createComment(Map<String, String[]> requestProperties,
      Resource store) throws CommentsException {

    Node comment = null;

    Session session = store.getResourceResolver().adaptTo(Session.class);
    String storePath = store.getPath();
    String cid = generateCommentId();
    String pathToComment = CommentsUtils.getCommentsPathById(storePath, cid);

    try {
      comment = JcrUtils.deepGetOrCreateNode(session, pathToComment);

      // Set the correct properties.
      // Only this user can edit the message.
      Authorizable authorizable = AccessControlUtil.getUserManager(session)
          .getAuthorizable(session.getUserID());
      ACLUtils.addEntry(comment.getPath(), authorizable, session,
          ACLUtils.WRITE_GRANTED, ACLUtils.MODIFY_PROPERTIES_GRANTED,
          ACLUtils.MODIFY_ACL_DENIED);

      // TODO Make it so that nobody else can edit/delete this node except the
      // site group.

      // Write properties to the node.
      if (comment.isNew()) {
        for (Entry<String, String[]> param : requestProperties.entrySet()) {
          String[] values = param.getValue();
          if (values.length == 1) {
            comment.setProperty(param.getKey(), values[0]);
          } else {
            comment.setProperty(param.getKey(), values);
          }
        }

        // Set some additional properties.
        comment.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            CommentsConstants.SAKAI_COMMENT);
        comment.setProperty(CommentsConstants.PROP_FROM, session.getUserID());
        comment.setProperty(CommentsConstants.PROP_COMMENTID, cid);
      }

      session.save();

    } catch (RepositoryException e) {
      throw new CommentsException(500, "Unable to save comment in JCR.");
    }

    return comment;
  }

  /**
   * Generates a unique id for a comment based on the current thread and time
   * and taking a sha1hash of it.
   * 
   * @return
   * @throws CommentsException
   */
  private String generateCommentId() throws CommentsException {
    String cid = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      return org.sakaiproject.kernel.util.StringUtils.sha1Hash(cid);
    } catch (Exception ex) {
      throw new CommentsException(500, "Unable to create hash.");
    }
  }

  public NodeIterator getComments(Resource store, List<String[]> sorts)
      throws CommentsException {
    // TODO Auto-generated method stub

    Node storeNode = (Node) store.adaptTo(Node.class);
    NodeIterator it = null;
    String queryString;

    String sortOn = "";
    if (sorts != null && sorts.size() != 0) {
      sortOn = " order by ";
      for (String[] s : sorts) {
        sortOn += "@" + s[0] + " " + s[1] + " ";
      }
    }

    try {
      queryString = "/" + ISO9075.encodePath(storeNode.getPath())
          + " //*[@sling:resourceType='sakai/comment']" + sortOn;

      LOG.info("Getting comments with: {}", queryString);

      QueryManager queryManager = storeNode.getSession().getWorkspace()
          .getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      it = result.getNodes();
    } catch (RepositoryException e) {
      LOG.warn(e.getMessage());
      e.printStackTrace();
      throw new CommentsException(400, "Could not retrieve comments.");
    }

    return it;

  }

  private boolean isCollaboratorOnSite(Node store, String userId) {
    try {
      // Find the site store.
      Node n = store;
      while (!n.getPath().equals("/")) {
        if (siteService.isSite((Item) n)) {
          break;
        }
        n = n.getParent();
      }
      if (n.getPath() == "/")
        return false;

      // Check if the user is part of the collaborator group for this site.
      // TODO: Do this on a better and cleaner way!
      String sitename = n.getProperty("id").getString();
      String groupname = "g-" + sitename + "-collaborators";
      UserManager um = AccessControlUtil.getUserManager(n.getSession());
      Authorizable siteGroupCollabs = um.getAuthorizable(groupname);
      if (siteGroupCollabs instanceof Group) {
        Group siteGroup = (Group) siteGroupCollabs;
        Authorizable user = um.getAuthorizable(userId);
        if (siteGroup.isMember(user)) {
          return true;
        }
      }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return false;
  }

  public void outputCommentsAsJSON(NodeIterator it, ExtendedJSONWriter writer,
      ResourceResolver resourceResolver, int start, int items,
      String currentUser, Node store) {
    try {
      writer.array();

      int i = 0;
      boolean canEdit = false;
      while (it.hasNext()) {
        canEdit = false;
        Node n = it.nextNode();
        // No need to go to the end of the list.
        if (i > start + items) {
          break;
        }

        // Paging
        if (i >= start && i < start + items) {
          writer.object();

          // Write entire node to output
          ExtendedJSONWriter.writeNodeContentsToWriter(writer, n);

          // Show some extra properties.
          if (n.hasProperty(CommentsConstants.PROP_FROM)) {
            String user = n.getProperty(CommentsConstants.PROP_FROM)
                .getString();

            // Get the profile for this user.
            LOG.info("Getting profile information for '{}'", user);
            Resource resource = resourceResolver.resolve(PersonalUtils
                .getProfilePath(user));
            ValueMap profileMap = resource.adaptTo(ValueMap.class);
            if (profileMap != null) {
              writer.key("profile");
              writer.valueMap(profileMap);
            }

            if (currentUser.equals(user) && !currentUser.equals(UserConstants.ANON_USERID)) {
              canEdit = true;
            } else {
              if (isCollaboratorOnSite(store, currentUser)) {
                canEdit = true;
              }
            }
          }

          // Wether or not the current user can edit this post.
          writer.key(CommentsConstants.PROP_CANEDIT);
          writer.value(canEdit);

          writer.endObject();
        }
        i++;
      }

      writer.endArray();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ValueFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
