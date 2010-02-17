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
package org.sakaiproject.nakamura.api.discussion;

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.util.ACLUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

public class Post {

  public static final Logger LOG = LoggerFactory.getLogger(Post.class);

  private Node node;
  private List<Post> children;
  private String postId;

  public Post(Node node) {
    setNode(node);
    children = new ArrayList<Post>();
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
    try {
      if (node.hasProperty(MessageConstants.PROP_SAKAI_ID)) {
        setPostId(node.getProperty(MessageConstants.PROP_SAKAI_ID).getString());
      }
    } catch (Exception e) {
      setPostId("");
    }
  }

  public List<Post> getChildren() {
    return children;
  }

  public void setChildren(List<Post> children) {
    this.children = children;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }

  /**
   * Checks if the current user can or cannot edit this post.
   * 
   * @return
   */
  public boolean checkEdit() {
    try {
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(node
          .getSession());
      Privilege write = acm.privilegeFromName(ACLUtils.WRITE_GRANTED.substring(2));
      Privilege modProps = acm.privilegeFromName(ACLUtils.MODIFY_PROPERTIES_GRANTED
          .substring(2));
      Privilege[] privileges = { write, modProps };
      if (acm.hasPrivileges(node.getPath(), privileges)) {
        return true;
      }

      return false;
    } catch (UnsupportedRepositoryOperationException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    } catch (RepositoryException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    }

    return false;
  }

  /**
   * Checks if the current user can or cannot delete this post.
   * 
   * @return
   */
  public boolean checkDelete() {
    try {
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(node
          .getSession());
      Privilege write = acm.privilegeFromName(ACLUtils.WRITE_GRANTED.substring(2));
      Privilege modProps = acm.privilegeFromName(ACLUtils.MODIFY_PROPERTIES_GRANTED
          .substring(2));
      Privilege deleteNode = acm.privilegeFromName(ACLUtils.REMOVE_NODE_GRANTED
          .substring(2));
      Privilege deleteChildNode = acm
          .privilegeFromName(ACLUtils.REMOVE_CHILD_NODES_GRANTED.substring(2));
      Privilege[] privileges = { write, modProps, deleteNode, deleteChildNode };
      if (acm.hasPrivileges(node.getPath(), privileges)) {
        return true;
      }

      return false;
    } catch (UnsupportedRepositoryOperationException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    } catch (RepositoryException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    }

    return false;
  }

  public void outputPostAsJSON(JSONWriter writer) throws JSONException,
      RepositoryException {
    boolean canEdit = checkEdit();
    boolean canDelete = checkDelete();

    // If this post has been marked as deleted, we dont show it.
    // we do however show the children of it.
    boolean isDeleted = false;
    if (node.hasProperty(DiscussionConstants.PROP_DELETED)) {
      isDeleted = node.getProperty(DiscussionConstants.PROP_DELETED).getBoolean();
    }

    if (isDeleted && !canDelete) {
      // This post has been deleted and we dont have sufficient rights to edit, so we just
      // show the replies.
      outputChildrenAsJSON(writer);
    } else {
      writer.object();

      writer.key("post");
      writer.object();
      ExtendedJSONWriter.writeNodeContentsToWriter(writer, node);
      writer.key(MessageConstants.PROP_SAKAI_ID);
      writer.value(getPostId());

      writer.key("canEdit");
      writer.value(canEdit);
      writer.key("canDelete");
      writer.value(canDelete);

      // Show profile of editters.
      if (node.hasProperty(DiscussionConstants.PROP_EDITEDBY)) {

        String edittedBy[] = StringUtils.split(getNode().getProperty(
            DiscussionConstants.PROP_EDITEDBY).getString(), ',');

        writer.key(DiscussionConstants.PROP_EDITEDBYPROFILES);
        writer.array();
        for (int i = 0; i < edittedBy.length; i++) {
          writer.object();
          PersonalUtils.writeUserInfo(getNode().getSession(), edittedBy[i], writer,
              "editter");
          writer.endObject();
        }
        writer.endArray();
      }

      writer.key("profile");
      String profilePath = PersonalUtils.getProfilePath(getNode().getProperty(
          MessageConstants.PROP_SAKAI_FROM).getString());
      Node profileNode = JcrUtils.getFirstExistingNode(getNode().getSession(),
          profilePath);
      ExtendedJSONWriter.writeNodeToWriter(writer, profileNode);

      writer.endObject();
      writer.key("replies");

      writer.array();
      outputChildrenAsJSON(writer);
      writer.endArray();

      writer.endObject();
    }
  }

  public void outputChildrenAsJSON(JSONWriter writer) throws JSONException,
      RepositoryException {
    LOG.info("this post {} has {} children", getPostId(), getChildren().size());
    for (Post p : children) {
      p.outputPostAsJSON(writer);
    }
  }

  public boolean addPost(Node n, String postid, String replyon) {
    for (Post p : children) {
      if (p.postId.equals(replyon)) {
        // replied on this post.
        p.children.add(new Post(n));
        return true;
      } else if (p.children.size() > 0 && p.addPost(n, postid, replyon)) {
        return true;
      }
    }

    return false;
  }
}
