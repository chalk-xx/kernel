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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.sling.api.resource.Resource;

public interface DiscussionManager {

  public NodeIterator getDiscussionPosts(Resource store, List<String[]> sorts);

  /**
   * Create an initial post.
   * @param requestProperties All the request parameters.
   * @param store the store where you want to add an initial post under.
   * @return The newly created node.
   */
  public Node createInitialPost(Map<String, String[]> requestProperties, Resource store) throws DiscussionException;

  /**
   * Add a reply to the store.
   * @param requestProperties All the request parameters.
   * @param post The resource that resembles the post to reply on.
   * @return The newly created node.
   */
  public Node reply(Map<String, String[]> requestProperties, Resource post) throws DiscussionException;
  

  /**
   * Edits a post by setting all the requestProperties and adds the current userid to the array of editedby properties.
   * @param requestProperties All the request parameters.
   * @param post The resource that resembles the post to edit.
   * @return The edited node.
   */
  public Node edit(Map<String, String[]> requestProperties, Resource post) throws DiscussionException;
  
  /**
   * Delete a post. Note: Don't really delete it. Just mark it with a property.
   * @param post The resource that resembles the post to delete.
   * @return Returns the 'deleted' post.
   */
  public Node delete(Resource post) throws DiscussionException;
}
