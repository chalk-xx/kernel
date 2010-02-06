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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.PathUtils;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class DiscussionUtils {
  public static String getFullPostPath(String store, String postid) {
    return PathUtils.toInternalHashedPath(store, postid, "");
}

  public static boolean isSiteCollaborator(String currentUser, Resource store, SiteService siteService) {
    
      // Find the site store.
    try {
      Node n = (Node) store.adaptTo(Node.class);
        while (!n.getPath().equals("/")) {
          if (siteService.isSite((Item) n)) {
            break;
          }
          n = n.getParent();
        }
      if (n.getPath() == "/")
        return false;

      // Check if the user is part of the collaborator group for this site.
      // TODO: Do this on a better and cleaner way!
      
      // Get the collaborators group for this site.
      String sitename = n.getProperty("id").getString();
      String groupname = "g-" + sitename + "-collaborators";
      UserManager um = AccessControlUtil.getUserManager(n.getSession());
      Authorizable siteGroupCollabs = um.getAuthorizable(groupname);
      if (siteGroupCollabs instanceof Group) {
        Group siteGroup = (Group) siteGroupCollabs;
        Authorizable user = um.getAuthorizable(currentUser);
        if (siteGroup.isMember(user)) {
          return true;
        }
      }
    } catch (ItemNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (AccessDeniedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return false;
  }
}
