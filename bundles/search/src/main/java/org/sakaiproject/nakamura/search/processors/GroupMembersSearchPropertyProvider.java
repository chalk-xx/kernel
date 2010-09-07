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
package org.sakaiproject.nakamura.search.processors;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
@Component
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "GroupMembers")
})
public class GroupMembersSearchPropertyProvider implements SearchPropertyProvider {
  private static final Logger logger = LoggerFactory
      .getLogger(GroupMembersSearchPropertyProvider.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);

      // get the request group name
      String groupName = request.getParameter("group");
      if (groupName == null) {
        throw new IllegalArgumentException("Must provide group to search within.");
      }

      // get the authorizable associated to the requested group name
      Group group = (Group) um.getAuthorizable(groupName);
      if (group == null) {
        throw new IllegalArgumentException("Unable to find group [" + groupName + "]");
      }

      Iterator<Authorizable> members = group.getDeclaredMembers();
      ArrayList<String> memberIds = new ArrayList<String>();
      while (members.hasNext()) {
        memberIds.add(members.next().getID());
      }

      // get the managers group for the request group and its members to the list
      Group managerGroup = null;
      if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
        Value[] values = group.getProperty(UserConstants.PROP_MANAGERS_GROUP);
        if ((values != null) && (values.length == 1)) {
          String managerGroupId = values[0].getString();
          managerGroup = (Group) um.getAuthorizable(managerGroupId);
          if (managerGroup != null) {
            members = managerGroup.getDeclaredMembers();
            while (members.hasNext()) {
              memberIds.add(members.next().getID());
            }
          } else {
            logger.warn("Unable to find manager's group [" + managerGroupId + "]");
          }
        }
      }

      if (memberIds.size() > 900) {
        // more than the threshold; pass along for post processing
        request.setAttribute("memberIds", memberIds);
      } else {
        // update the query to filter before writing nodes
        propertiesMap.put("_groupQuery",
            "and (rep:userId='" + StringUtils.join(memberIds, "' or rep:userId='") + "')");
      }
    } catch (RepositoryException e) {
      logger.error(e.getMessage(), e);
    }
  }
}
