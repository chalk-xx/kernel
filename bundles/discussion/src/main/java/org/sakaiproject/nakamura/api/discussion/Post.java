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

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

public class Post {

  public static final Logger LOG = LoggerFactory.getLogger(Post.class);

  private Content content;
  private List<Post> children;
  private String postId;
  private Session session;

  public Post(Content content, Session session) {
    setContent(content);
    this.session = session;
    children = new ArrayList<Post>();
  }

  public Content getContent() {
    return content;
  }

  public void setContent(Content content) {
    this.content = content;
    try {
      if (content.hasProperty(MessageConstants.PROP_SAKAI_ID)) {
        setPostId((String) content.getProperty(MessageConstants.PROP_SAKAI_ID));
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
    boolean retval = false;
    try {
      AccessControlManager accessControlManager = session.getAccessControlManager();
      accessControlManager.check(Security.ZONE_CONTENT, content.getPath(),
          Permissions.CAN_WRITE);
      retval = true;
    } catch (AccessDeniedException e) {
      retval = false;
    } catch (StorageClientException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    }

    return retval;
  }

  /**
   * Checks if the current user can or cannot delete this post.
   *
   * @return
   */
  public boolean checkDelete() {
    boolean retval = false;
    try {
      AccessControlManager accessControlManager = session.getAccessControlManager();
      accessControlManager.check(Security.ZONE_CONTENT, content.getPath(),
          Permissions.CAN_DELETE);
      retval = true;
    } catch (AccessDeniedException e) {
      retval = false;
    } catch (StorageClientException e) {
      LOG.warn("Unable to check if user has right to edit post.");
    }
    return retval;
  }

  public void outputPostAsJSON(ExtendedJSONWriter writer,
      PresenceService presenceService, BasicUserInfoService basicUserInfoService, Session session)
      throws JSONException, StorageClientException, AccessDeniedException,
      RepositoryException {
    boolean canEdit = checkEdit();
    boolean canDelete = checkDelete();

    // If this post has been marked as deleted, we dont show it.
    // we do however show the children of it.
    boolean isDeleted = false;
    if (content.hasProperty(DiscussionConstants.PROP_DELETED)) {
      isDeleted = Boolean.parseBoolean(content.getProperty(
          DiscussionConstants.PROP_DELETED).toString());
    }

    if (isDeleted && !canDelete) {
      // This post has been deleted and we dont have sufficient rights to edit, so we just
      // show the replies.
      outputChildrenAsJSON(writer, presenceService, basicUserInfoService, session);
    } else {
      writer.object();

      writer.key("post");
      writer.object();
      ExtendedJSONWriter.writeNodeContentsToWriter(writer, content);

      writer.key("canEdit");
      writer.value(canEdit);
      writer.key("canDelete");
      writer.value(canDelete);

      AuthorizableManager authMgr = session.getAuthorizableManager();
      // Show profile of editters.
      if (content.hasProperty(DiscussionConstants.PROP_EDITEDBY)) {

        String editedByProp = (String) content.getProperty(
            DiscussionConstants.PROP_EDITEDBY);
        String[] edittedBy = StringUtils.split(editedByProp, ',');

        writer.key(DiscussionConstants.PROP_EDITEDBYPROFILES);
        writer.array();
        for (int i = 0; i < edittedBy.length; i++) {
          writer.object();
          Authorizable au = authMgr.findAuthorizable(edittedBy[i]);
          ValueMap profile = new ValueMapDecorator(basicUserInfoService.getProperties(au));
          writer.valueMapInternals(profile);
          PresenceUtils.makePresenceJSON(writer, edittedBy[i], presenceService, true);
          writer.endObject();
        }
        writer.endArray();
      }

      // Show some profile info.
      writer.key("profile");
      String fromVal = (String) content.getProperty(MessageConstants.PROP_SAKAI_FROM);
      String[] senders = StringUtils.split(fromVal, ',');
      writer.array();
      for (String sender : senders) {
        writer.object();
        Authorizable au = authMgr.findAuthorizable(sender);
        ValueMap profile = new ValueMapDecorator(basicUserInfoService.getProperties(au));
        writer.valueMapInternals(profile);
        PresenceUtils.makePresenceJSON(writer, sender, presenceService, true);
        writer.endObject();
      }
      writer.endArray();
      writer.endObject();

      // All the replies on this post.
      writer.key("replies");
      writer.array();
      outputChildrenAsJSON(writer, presenceService, basicUserInfoService, session);
      writer.endArray();

      writer.endObject();
    }
  }

  public void outputChildrenAsJSON(ExtendedJSONWriter writer,
      PresenceService presenceService, BasicUserInfoService basicUserInfoService, Session session)
      throws JSONException, StorageClientException, AccessDeniedException,
      RepositoryException {
    LOG.info("this post {} has {} children", getPostId(), getChildren().size());
    for (Post p : children) {
      p.outputPostAsJSON(writer, presenceService, basicUserInfoService, session);
    }
  }

  public boolean addPost(Content c, String postid, String replyon) {
    for (Post p : children) {
      if (p.postId.equals(replyon)) {
        // replied on this post.
        p.children.add(new Post(c, this.session));
        return true;
      } else if (p.children.size() > 0 && p.addPost(c, postid, replyon)) {
        return true;
      }
    }

    return false;
  }
}
