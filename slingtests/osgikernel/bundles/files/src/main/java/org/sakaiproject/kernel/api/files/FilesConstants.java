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

import org.apache.commons.lang.time.FastDateFormat;

public interface FilesConstants {

  /**
   * Resource type for the file store. sakai/files
   */
  public static final String RT_FILE_STORE = "sakai/files";
  /**
   * The resource type for a sakai file. sakai/file
   */
  public static final String RT_SAKAI_FILE = "sakai/file";
  /**
   * The resource type for a sakai folder. sakai/folder
   */
  public static final String RT_SAKAI_FOLDER = "sakai/folder";
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
   * sakai:user
   */
  public static final String SAKAI_USER = "sakai:user";
  /**
   * sakai:id
   */
  public static final String SAKAI_ID = "sakai:id";

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
   * sakai:file
   */
  public static final String SAKAI_FILE = "sakai:file";
  /**
   * sakai:filename
   */
  public static final String SAKAI_FILENAME = "sakai:filename";
  /**
   * sakai:mimeType
   */
  public static final String SAKAI_MIMETYPE = "sakai:mimeType";
  /**
   * sakai:link
   */
  public static final String SAKAI_LINK = "sakai:link";
  /**
   * sakai:sites
   */
  public static final String SAKAI_SITES = "sakai:sites";
  /**
   * The mixin required on the node to do a tag.
   */
  public static final String REQUIRED_MIXIN = "sakai:propertiesmix";
  /**
   * FileHandlerProcessor
   */
  public static final String LINK_HANDLER = "LinkHandler";

  public static final String REG_PROCESSOR_NAMES = "sakai.files.handler";
  public static final FastDateFormat DATEFORMAT = FastDateFormat
      .getInstance("yyyy-MM-dd'T'HH:mm:ss");

}
