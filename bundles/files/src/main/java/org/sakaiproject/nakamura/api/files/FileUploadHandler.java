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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface FileUploadHandler {
  /**
   * This method is called when a file is uploaded via the
   * CreateContentPoolServlet.  It is called after the file has been added to
   * the repository, but has the opportunity to add or replace properties by
   * returning them as a Map.
   *
   * @param poolId
   *          The path of the content object for the file (a unique identifier).
   *
   * @param contentProperties
   *          An immutable map of the content object's properties (see sparsemapcontent's org.sakaiproject.nakamura.lite.content.InternalContent for the names of common properties)
   *
   * @param fileInputStream
   *          A FileInputStream on the uploaded content, set to position zero.
   *
   * @param userId
   *          The login name of the client performing the file upload (as per
   *          request.getRemoteUser())
   *
   * @param isNew
   *          True if the uploaded file is new content.  False if it replaces an existing node.
   *
   * @return A map of properties that will be added to the newly-created content object.
   *
   **/
  void handleFile(String poolId,
                  InputStream fileInputStream,
                  String userId,
                  boolean isNew)
    throws IOException;
}