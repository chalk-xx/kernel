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

public interface FilesConstants {

  /**
   * Resource type for the file store.
   */
  public static final String RT_FILE_STORE = "sakai/files";
  /**
   * The resource type for a sakai file.
   */
  public static final String RT_SAKAI_FILE = "sakai/file";
    
  /**
   * The path to the filestore for users.
   */
  public static final String USER_FILESTORE = "/_user/files/store";

  public static final String SAKAI_USER = "sakai:user";

  public static final String SAKAI_ID = "sakai:id";
  public static final String SAKAI_TAGS = "sakai:tags";

  
}
