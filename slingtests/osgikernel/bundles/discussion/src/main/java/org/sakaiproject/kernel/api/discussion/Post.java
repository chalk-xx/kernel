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
package org.sakaiproject.kernel.api.discussion;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      if (node.hasProperty(DiscussionConstants.PROP_POST_ID)) {
        setPostId(node.getProperty(DiscussionConstants.PROP_POST_ID).getString());
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

  public void outputPostAsJSON(ExtendedJSONWriter writer, String currentUser,
      boolean isSitecollaborator) throws JSONException, RepositoryException {
    LOG.info("Outputting post with ID: " + getPostId());

    boolean canEdit = false, canDelete = false;
    // Site collabs can do anything..
    if (isSitecollaborator) {
      canEdit = true;
      canDelete = true;
    } else if (!currentUser.equals(UserConstants.ANON_USERID)
        && currentUser
            .equals(node.getProperty(DiscussionConstants.PROP_FROM).getString())) {
      // if this is the user his own post, he can do whatever he likes..
      canEdit = true;
      canDelete = true;
    }

    // If this post has been marked as deleted, we dont show it.
    // we do however show the children of it.
    boolean isDeleted = false;
    if (node.hasProperty(DiscussionConstants.PROP_DELETED)) {
      isDeleted = node.getProperty(DiscussionConstants.PROP_DELETED).getBoolean();
    }

    if (isDeleted && !canDelete) {
      // This post has been deleted and we dont have sufficient rights to edit, so we just show the replies.
      outputChildrenAsJSON(writer, currentUser, isSitecollaborator); 
    }
    else {
      writer.object();

      writer.key("post");
      writer.object();
      ExtendedJSONWriter.writeNodeContentsToWriter(writer, node);
      writer.key(DiscussionConstants.PROP_POST_ID);
      writer.value(getPostId());

      writer.key("canEdit");
      writer.value(canEdit);
      writer.key("canDelete");
      writer.value(canDelete);

      // List of all the people who have edited this post.
      if (node.hasProperty(DiscussionConstants.PROP_EDITEDBY)) {
        Value[] editedBy = getNode().getProperty(DiscussionConstants.PROP_EDITEDBY)
            .getValues();
        writer.key("editters");
        writer.array();
        for (int i = 0; i < editedBy.length; i++) {
          String[] s = StringUtils.split(editedBy[i].getString(), '|');
          writer.object();

          writer.key("profile");
          String profilePath = PersonalUtils.getProfilePath(getNode().getProperty(
              DiscussionConstants.PROP_FROM).getString());
          Node profileNode = JcrUtils.getFirstExistingNode(getNode().getSession(),
              profilePath);
          writer.node(profileNode);
          writer.key("date");
          writer.value(s[1]);

          writer.endObject();
        }
        writer.endArray();
      }

      writer.key("profile");
      String profilePath = PersonalUtils.getProfilePath(getNode().getProperty(
          DiscussionConstants.PROP_FROM).getString());
      Node profileNode = JcrUtils.getFirstExistingNode(getNode().getSession(),
          profilePath);
      writer.node(profileNode);

      writer.endObject();
      writer.key("replies");

      writer.array();
      outputChildrenAsJSON(writer, currentUser, isSitecollaborator);
      writer.endArray();

      writer.endObject();
    }
  }

  public void outputChildrenAsJSON(ExtendedJSONWriter writer, String currentUser,
      boolean isSitecollaborator) throws JSONException, RepositoryException {
    LOG.info("this post {} has {} children", getPostId(), getChildren().size());
    for (Post p : children) {
      p.outputPostAsJSON(writer, currentUser, isSitecollaborator);
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
