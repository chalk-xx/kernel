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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import com.google.common.collect.ImmutableSet;

import org.sakaiproject.nakamura.api.lite.content.Content;

import java.util.Set;

public interface FilesConstants {
  /**
   * The resource type for a sakai link. sakai/link
   */
  public static final String RT_SAKAI_LINK = "sakai/link";
  /**
   * The resource type for a sakai tag. sakai/tag
   */
  public static final String RT_SAKAI_TAG = "sakai/tag";
  /**
   * The path to the filestore for users.
   */
  public static final String USER_FILESTORE = "/_user/files";

  /**
   * sakai:tags
   */
  public static final String SAKAI_TAGS = "sakai:tags";
  /**
   * sakai:tag-uuid
   */
  public static final String SAKAI_TAG_UUIDS = "sakai:tag-uuid";
  /**
   * sakai:tag-name - Intended to identify the name of a tag
   */
  public static final String SAKAI_TAG_NAME = "sakai:tag-name";
  /**
   * sakai:description - description of file as given by the user
   */
  public static final String SAKAI_DESCRIPTION = "sakai:description";
  /**
   * sakai:file
   */
  public static final String SAKAI_FILE = "sakai:file";
  /**
   * sakai:link
   */
  public static final String SAKAI_LINK = "sakai:link";
  /**
   * The mixin required on the node to do a tag.
   */
  public static final String REQUIRED_MIXIN = "sakai:propertiesmix";
  /**
   * FileHandlerProcessor
   */
  public static final String LINK_HANDLER = "LinkHandler";

  public static final String REG_PROCESSOR_NAMES = "sakai.files.handler";

  /**
   * The OSGi topic for tagging a file.
   */
  public static final String TOPIC_FILES_TAG = "org/sakaiproject/nakamura/files/tag";

  /**
   * The OSGi topic for linking a file.
   */
  public static final String TOPIC_FILES_LINK = "org/sakaiproject/nakamura/files/link";

  /**
   * The sling:resourceType for pooled content.
   */
  public static final String POOLED_CONTENT_RT = "sakai/pooled-content";
  /**
   * The jcr:primaryType for pooled content.
   */
  public static final String POOLED_CONTENT_NT = "sakai:pooled-content";
  /**
   * The jcr:primaryType for the pooled content members nodetype.
   */
  public static final String POOLED_CONTENT_USER_RT = "sakai/pooled-content-user";

  /**
   * The name of the node that contains the viewers and and managers of the pooled content.
   */
  public static final String POOLED_CONTENT_MEMBERS_NODE = "/members";
  /**
   * The name for the managers property on the pooled content members node.
   */
  public static final String POOLED_CONTENT_USER_MANAGER = "sakai:pooled-content-manager";
  /**
   * The name for the viewers property on the pooled content members node.
   */
  public static final String POOLED_CONTENT_USER_VIEWER = "sakai:pooled-content-viewer";
  /**
   * The name of the property that holds the filename.
   */
  public static final String POOLED_CONTENT_FILENAME = "sakai:pooled-content-file-name";
  /**
   * The name of the property that holds the custom mimetype.
   */
  public static final String POOLED_CONTENT_CUSTOM_MIMETYPE = "sakai:custom-mimetype";

  /**
   * Property on the file node indicating who the content was created for.
   */
  public static final String POOLED_CONTENT_CREATED_FOR = "sakai:pool-content-created-for";

  /**
   * Selector for feed of related content which is accessible by any logged-in user.
   */
  public static final String POOLED_CONTENT_RELATED_SELECTOR = "related";
  /**
   * Selector for feed of related content which is publicly accessible.
   */
  public static final String POOLED_CONTENT_PUBLIC_RELATED_SELECTOR = "relatedpublic";

  /**
   * Property which stores the UX access scheme for the node.
   */
  public static final String ACCESS_SCHEME_PROPERTY = "sakai:permissions";
  /**
   * The access scheme which lets any logged-in user see the item.
   */
  public static final String LOGGED_IN_ACCESS_SCHEME = "everyone";
  /**
   * The access scheme which makes the item visible even to sessions which have not logged in.
   */
  public static final String PUBLIC_ACCESS_SCHEME = "public";

  public static final String POOLED_NEEDS_PROCESSING = "sakai:needsprocessing";

  /** Property of when the content was last modified */
  public static final String LAST_MODIFIED = Content.LASTMODIFIED_FIELD;

  /** Property of who modified the content last */
  public static final String LAST_MODIFIED_BY = Content.LASTMODIFIED_BY_FIELD;

  /** Property of when the content was created */
  public static final String CREATED = Content.CREATED_FIELD;

  /** Property of who created the content */
  public static final String CREATED_BY = Content.CREATED_BY_FIELD;

  /** Property of where this content is used*/
  public static final String LINK_PATHS = "linkpaths";

  /** Property stem for structure properties in content pool items */
  public static final String STRUCTURE_FIELD_STEM = "structure";

  /**
   * Resource ID referneces in structures.
   */
  public static final String RESOURCE_REFERENCE_FIELD = "_ref";
  
  
  public static final Set<String> RESERVED_POOL_KEYS = ImmutableSet.of(
      SLING_RESOURCE_TYPE_PROPERTY, POOLED_CONTENT_CREATED_FOR,
      POOLED_CONTENT_USER_MANAGER, POOLED_NEEDS_PROCESSING,
      Content.MIMETYPE_FIELD);

String SAKAI_TAG_COUNT = "sakai:tag-count";}
