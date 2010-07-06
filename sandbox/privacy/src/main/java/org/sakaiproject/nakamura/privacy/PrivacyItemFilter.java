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

package org.sakaiproject.nakamura.privacy;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.webdav.api.SlingItemFilterHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;


@Component(immediate = true)
@Service(value = SlingItemFilterHelper.class)
public class PrivacyItemFilter implements SlingItemFilterHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyItemFilter.class);

  public boolean isFilteredItem(String name, Session session) {
    try {
      if (name.startsWith("/_user")) {
        PrincipalManager pm = AccessControlUtil.getPrincipalManager(session);
        Principal p = pm.getPrincipal(session.getUserID());
        if (p instanceof ItemBasedPrincipal) {
          ItemBasedPrincipal ibp = (ItemBasedPrincipal) p;
          String userPath = "/_user"
              + ibp.getPath().substring(
                  "/rep:security/rep:authorizables/rep:users".length());
          if (name.startsWith(userPath) || userPath.startsWith(name)) {
            LOGGER.debug("Not Filtering User Path Item {}  for {} user path [{}] ",
                new Object[] { name, session.getUserID(), userPath });
            return false;
          } else {
            LOGGER.debug("Filtering User Path Item {} for {} user path [{}] ",
                new Object[] { name, session.getUserID(), userPath });
            return true;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    LOGGER.debug("Not Filtering Item {} for {} ", name, session.getUserID());
    return false;
  }

  public boolean isFilteredItem(Item item) {
    try {
      return isFilteredItem(item.getPath(), item.getSession());
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return false;
  }

}
