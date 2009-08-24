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
package org.sakaiproject.kernel.discussion;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionException;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.discussion.DiscussionUtils;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.util.ACLUtils;
import org.sakaiproject.kernel.util.DateUtils;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * Manager for the discussions.
 * 
 * @scr.component immediate="true" label="Sakai Discussion Manager"
 *                description="Service for doing operations with discussions." name
 *                ="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionManagerImpl implements DiscussionManager {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionManagerImpl.class);

  public NodeIterator getDiscussionPosts(Resource store, List<String[]> sorts) {
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
          + "//*[@sling:resourceType='" + DiscussionConstants.SAKAI_DISCUSSION_POST
          + "']" + sortOn;

      LOG.info("Getting discussion posts with: {}", queryString);

      QueryManager queryManager = storeNode.getSession().getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      it = result.getNodes();
    } catch (RepositoryException e) {
      LOG.warn(e.getMessage());
      e.printStackTrace();
    }
    return it;
  }

  public Node createInitialPost(Map<String, String[]> requestProperties, Resource store)
      throws DiscussionException {
    Node post = null;
    try {

      // If this user is an anonymous user we do some checks.
      Node storeNode = (Node) store.adaptTo(Node.class);
      if (storeNode.getSession().getUserID().equals(UserConstants.ANON_USERID)) {
        allowAnonymousPost(requestProperties, store);
      }

      post = createPost(requestProperties, store);
      post.setProperty(DiscussionConstants.PROP_INITIAL_POST, true);

      post.getSession().save();
    } catch (RepositoryException e) {
      LOG.warn("Could not create post: {} " + e.getMessage());
      e.printStackTrace();
      throw new DiscussionException(500, "Unable to save post to JCR.");
    }

    return post;
  }

  public Node reply(Map<String, String[]> requestProperties, Resource post)
      throws DiscussionException {
    Node postNode = null;
    // Find the store this post belongs to.
    Resource store = findStore(post);
    Node replyOnPost = (Node) post.adaptTo(Node.class);

    try {

      // If this user is an anonymous user we do some checks.
      if (replyOnPost.getSession().getUserID().equals(UserConstants.ANON_USERID)) {
        allowAnonymousPost(requestProperties, store);
      }
      postNode = createPost(requestProperties, store);
      postNode.setProperty(DiscussionConstants.PROP_INITIAL_POST, false);
      postNode.setProperty(DiscussionConstants.PROP_REPLY_ON, replyOnPost.getName());

      postNode.getSession().save();
    } catch (RepositoryException e) {
      LOG.warn("Could not create post: {} " + e.getMessage());
      e.printStackTrace();
      throw new DiscussionException(500, "Unable to save post to JCR.");
    }

    return postNode;
  }

  public Node delete(Resource post) throws DiscussionException {
    Node postNode = (Node) post.adaptTo(Node.class);
    try {
      postNode.setProperty(DiscussionConstants.PROP_DELETED, true);
      postNode.getSession().save();
    } catch (Exception ex) {
      LOG.warn("unable to delete post: {}", ex.getMessage());
      ex.printStackTrace();
      throw new DiscussionException(500, "Unable to delete post.");
    }
    return null;
  }

  public Node edit(Map<String, String[]> requestProperties, Resource post)
      throws DiscussionException {
    Node postNode = null;
    try {
      postNode = (Node) post.adaptTo(Node.class);
      // Get the list of edits.
      Value[] newEditedby = null;
      Value v = ValueFactoryImpl.getInstance().createValue(
          postNode.getSession().getUserID() + "|" + DateUtils.rfc3339());
      if (postNode.hasProperty(DiscussionConstants.PROP_EDITEDBY)) {
        Value[] editedBy = postNode.getProperty(DiscussionConstants.PROP_EDITEDBY)
            .getValues();
        newEditedby = new Value[editedBy.length + 1];
        for (int i = 0; i < editedBy.length; i++) {
          newEditedby[i] = editedBy[i];
        }
        // Add new edit.
        newEditedby[editedBy.length] = v;
      } else {
        newEditedby = new Value[1];
        newEditedby[0] = v;
      }

      writePropertiesToNode(requestProperties, postNode);

      postNode.setProperty(DiscussionConstants.PROP_EDITEDBY, newEditedby);

      postNode.save();

    } catch (Exception ex) {
      LOG.warn("Unable to edit post {}", ex.getMessage());
      ex.printStackTrace();
      throw new DiscussionException(500, ex.getMessage());
    }
    return postNode;
  }

  /**
   * Checks if there are any anonymous settings on the store node. If they are they are
   * checked against the request.
   * 
   * @param requestProperties
   * @param store
   */
  protected void allowAnonymousPost(Map<String, String[]> requestProperties,
      Resource store) throws DiscussionException {

    try {
      Node storeNode = (Node) store.adaptTo(Node.class);
      // Default behavior is to allow anonymous comments.
      if (storeNode.hasProperty(DiscussionConstants.PROP_ALLOWANONYMOUS)) {
        boolean allowAnonymous = storeNode.getProperty(
            DiscussionConstants.PROP_ALLOWANONYMOUS).getBoolean();
        if (!allowAnonymous) {
          // The user is not allowed here..
          throw new DiscussionException(401, "User must be logged in to place a comment.");
        }
      } else {
        if (storeNode.hasProperty(DiscussionConstants.PROP_ANON_FORCE_NAME)) {
          String[] name = requestProperties.get(DiscussionConstants.PROP_ANON_NAME);
          if (name == null) {
            throw new DiscussionException(400, "Anonymous comments must provide a name.");
          }
        }
        if (storeNode.hasProperty(DiscussionConstants.PROP_ANON_FORCE_EMAIL)) {
          String[] email = requestProperties.get(DiscussionConstants.PROP_ANON_EMAIL);
          if (email == null) {
            throw new DiscussionException(400,
                "Anonymous comments must provide an email address.");
          }
          // TODO Add email validation?
        }
      }
    } catch (PathNotFoundException e) {
      LOG.warn("Problem checking anonymous users {}", e.getMessage());
      e.printStackTrace();
      throw new DiscussionException(500, "Unable to check anonymous posts.");
    } catch (ValueFormatException e) {
      LOG.warn("Problem checking anonymous users {}", e.getMessage());
      e.printStackTrace();
      throw new DiscussionException(500, "Unable to check anonymous posts.");
    } catch (RepositoryException e) {
      LOG.warn("Problem checking anonymous users {}", e.getMessage());
      e.printStackTrace();
      throw new DiscussionException(500, "Unable to check anonymous posts.");
    }
  }

  /**
   * Takes a post and looks for the store.
   * 
   * @param post
   * @return
   * @throws DiscussionException
   */
  protected Resource findStore(Resource post) throws DiscussionException {
    Node postNode = (Node) post.adaptTo(Node.class);
    String storePath = "";
    try {
      while (!postNode.equals("/")) {

        if (postNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          if (postNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) != null
              && postNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
                  .getString().equals(DiscussionConstants.SAKAI_DISCUSSION_STORE)) {
            storePath = postNode.getPath();
            break;
          }
        }

        postNode = postNode.getParent();
      }
    } catch (Exception ex) {
      LOG.warn("Error occured trying to get the store: {}", ex.getMessage());
      ex.printStackTrace();
      throw new DiscussionException(500, "Error occured trying to get the store.");
    }
    if (storePath.equals("/")) {
      throw new DiscussionException(403, "Could not locate store.");
    }

    Resource store = post.getResourceResolver().getResource(storePath);

    return store;

  }

  /**
   * Creates a post.
   * 
   * @param requestProperties
   * @param store
   * @return
   * @throws DiscussionException
   */
  protected Node createPost(Map<String, String[]> requestProperties, Resource store)
      throws DiscussionException {
    Node post = null;
    Session session = store.getResourceResolver().adaptTo(Session.class);
    String storePath = store.getPath();
    String postid = generatePostId();
    String pathToComment = DiscussionUtils.getFullPostPath(storePath, postid);

    try {
      post = JcrUtils.deepGetOrCreateNode(session, pathToComment);

      // Set the correct properties.
      // Only this user can edit the message.
      Authorizable authorizable = AccessControlUtil.getUserManager(session)
          .getAuthorizable(session.getUserID());
      ACLUtils.addEntry(post.getPath(), authorizable, session, ACLUtils.WRITE_GRANTED,
          ACLUtils.MODIFY_PROPERTIES_GRANTED, ACLUtils.MODIFY_ACL_DENIED,
          ACLUtils.REMOVE_NODE_DENIED);

      // TODO Make it so that nobody else can edit/delete this node except the
      // site group.

      // Write properties to the node.
      if (post.isNew()) {

        writePropertiesToNode(requestProperties, post);

        // Set some additional properties.
        post.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            DiscussionConstants.SAKAI_DISCUSSION_POST);
        post.setProperty(DiscussionConstants.PROP_FROM, session.getUserID());
        post.setProperty(DiscussionConstants.PROP_POST_ID, postid);
      }

      session.save();

    } catch (RepositoryException e) {
      throw new DiscussionException(500, "Unable to save post to JCR.");
    }
    return post;
  }

  protected void writePropertiesToNode(Map<String, String[]> requestProperties, Node n)
      throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException {
    for (Entry<String, String[]> param : requestProperties.entrySet()) {
      String[] values = param.getValue();
      if (values.length == 1) {
        n.setProperty(param.getKey(), values[0]);
      } else {
        n.setProperty(param.getKey(), values);
      }
    }
  }

  /**
   * Generates a unique id for a post based on the current thread and time and taking a
   * sha1hash of it.
   * 
   * @return
   * @throws DiscussionException
   */
  protected String generatePostId() throws DiscussionException {
    String cid = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      return org.sakaiproject.kernel.util.StringUtils.sha1Hash(cid);
    } catch (Exception ex) {
      LOG.warn("Unable to create hash for post.");
      throw new DiscussionException(500, "Unable to create hash.");
    }
  }

}
