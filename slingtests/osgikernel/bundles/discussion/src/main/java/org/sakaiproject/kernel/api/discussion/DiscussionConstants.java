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

public interface DiscussionConstants {

  public static final String SAKAI_DISCUSSION_STORE = "sakai/discussionstore";
  
  public static final String SAKAI_DISCUSSION_POST = "sakai/discussionpost";
  
  public static final String PROP_REPLY_ON = "sakai:replyon";
  
  public static final String PROP_POST_ID = "sakai:postid";
  
  public static final String PROP_INITIAL_POST = "sakai:initialpost";
  
  public static final String PROP_FROM = "sakai:from";
  
  public static final String PROP_DELETED = "sakai:deleted";
  
  public static final String PROP_EDITEDBY = "sakai:editedby";
  
  public static final String PROP_ALLOWANONYMOUS = "sakai:allowanonymous";
  /**
   * The property that determines if an email should be sent when someone leaves a post.
   */
  public static final String PROP_NOTIFICATION = "sakai:notification";
  
  /**
   * If the sakai:allowanonymous property is set to true. this can be set to to true to force a name.
   */
 public static final String PROP_ANON_FORCE_NAME = "sakai:forcename";
 
 /**
  * The value for a name if it is an anonymous post.
  */
 public static final String PROP_ANON_NAME = "sakai:name";
 
 /**
  * If the sakai:allowanonymous property is set to true. this can be set to to true to force an email address.
  */
 public static final String PROP_ANON_FORCE_EMAIL = "sakai:forcemail";
 
 /**
  * The value for a name if it is an anonymous post.
  */
 public static final String PROP_ANON_EMAIL = "sakai:email";
 
/**
 * The viewmode for getting the posts.
 */
  public static final String PARAM_VIEWMODE = "viewmode";
  
  /**
   * Start parameter to fetch the comments.
   */
  public static final String PARAM_START = "start";
  
  /**
   * Sort on..
   */
  public static final String PARAM_SORT = "sort";
  
  /**
   * Number of comments to fetch
   */
  public static final String PARAM_ITEMS = "items";

}
