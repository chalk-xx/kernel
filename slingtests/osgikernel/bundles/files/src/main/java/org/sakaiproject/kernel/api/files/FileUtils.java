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

import org.sakaiproject.kernel.util.PathUtils;
import org.sakaiproject.kernel.util.StringUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class FileUtils {

  /**
   * Returns a Base62 representation of a number.
   * 
   * @param nr
   * @return
   */
  private static String encodeBase66(long nr) {
    if (nr < 0) { nr *= -1; }
    String tempVal = nr == 0 ? "0" : "";
    String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_.+";
    int remainder = 0;
    while (nr > 0) {
      remainder = (int) (nr % 66);
      nr = nr / 66;
      tempVal += baseDigits.charAt(remainder);
    }
    return tempVal;
  }

  /**
   * Returns a Base62 number.
   * 
   * @return
   */
  public static String generateID() {

    try {
      long id = (long) (Thread.currentThread().getId() + System.currentTimeMillis() + Math
          .random() * 1000000000);
      String hash = StringUtils.sha1Hash(String.valueOf(id));
      return encodeBase66(hash.hashCode());
    } catch (Exception e) {
      System.out.println("failed");
      throw new RuntimeException(e);
    }
  }

  public static String getHashedPath(String id) {
    return PathUtils.toInternalHashedPath(FilesConstants.USER_FILESTORE, id, "");
  }

  public static String getDownloadPath(String id) {
    return FilesConstants.USER_FILESTORE + "/" + id;
  }

  public static String getActualFilePath(String id, Session session) {
    String fileName = "";
    String path = null;

    String downloadPath = getHashedPath(id);
    try {
      if (session.itemExists(downloadPath)) {
        Node downloadNode = (Node) session.getItem(downloadPath);
        if (downloadNode.hasProperty(FilesConstants.SAKAI_FILENAME)) {
          fileName = downloadNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();
          path = downloadPath + "/" + fileName;
        }
      }
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return path;
  }

}
