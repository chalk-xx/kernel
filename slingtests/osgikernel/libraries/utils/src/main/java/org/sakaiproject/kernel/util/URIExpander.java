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
package org.sakaiproject.kernel.util;

import org.apache.sling.jcr.resource.JcrResourceConstants;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class URIExpander {

  /**
   * This will expand a path. By default, any stores are expanded with PathUtils.
   * 
   * @param session
   *          The current session.
   * @param path
   *          The path to expand.
   * @return The expanded path.
   * @throws RepositoryException
   */
  public static String expandStorePath(Session session, String path)
      throws RepositoryException {
    // Get the first existing item.
    
    if (session.itemExists(path)) {
      return path;
    }
    
    String absPath = path;

    List<String> types = new ArrayList<String>();
    types.add("sakai/files");
    types.add("sakai/personalPrivate");
    types.add("sakai/personalPublic");
    types.add("sakai/groupPrivate");
    types.add("sakai/groupPublic");
    types.add("sakai/contactstore");
    types.add("sakai/messagestore");

    String[] parts = StringUtils.split(path, '/');
    String p = "";
    for(int i = 0; i < parts.length;i++) {
      p += "/" + parts[i];
      if (session.itemExists(p)) {
        Node node = (Node) session.getItem(p);
        if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          String rt = node.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
          if (types.contains(rt)) {
            p += PathUtils.getHashedPath((i < parts.length) ? parts[i+1] : "", 4);
            if (p.endsWith("/")) {
              p = p.substring(0, p.length() - 1);
            }
            i++;
          }
        }
      }
    }
    
    return p;
  }
}
