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
package org.sakaiproject.nakamura.meservice;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.TreeMap;

@ServiceDocumentation(
  name = "My Groups Servlet", okForVersion = "0.11",
  shortDescription = "Gets the groups where the current user is a member",
  description = "Gets the groups where the current user is a member",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE, bindings = { "system/me/groups" })
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = {"Get the groups this user is a member of",
          "curl -u suzy:suzy http://localhost:8080/system/me/groups.tidy.json" +
          "<pre>{\n" +
            "    \"items\": 25,\n" +
            "    \"results\": [{\n" +
            "        \"sakai:category\": null,\n" +
            "        \"sakai:group-description\": null,\n" +
            "        \"sakai:group-title\": \"math101 (Students)\",\n" +
            "        \"lastModified\": 1309561938702,\n" +
            "        \"homePath\": \"/~math101-student\",\n" +
            "        \"_path\": \"/~math101-student/public/authprofile\",\n" +
            "        \"sling:resourceType\": \"sakai/group-profile\",\n" +
            "        \"sakai:group-id\": \"math101-student\",\n" +
            "        \"createdBy\": \"admin\",\n" +
            "        \"basic\": {\n" +
            "            \"access\": \"everybody\",\n" +
            "            \"elements\": {\n" +
            "                \"lastName\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                },\n" +
            "                \"email\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                },\n" +
            "                \"firstName\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"created\": 1309556504969,\n" +
            "        \"lastModifiedBy\": \"admin\",\n" +
            "        \"counts\": {\n" +
            "            \"membershipsCount\": 1,\n" +
            "            \"contentCount\": 3,\n" +
            "            \"membersCount\": 1,\n" +
            "            \"countLastUpdate\": 1309750677273\n" +
            "        },\n" +
            "        \"groupid\": \"math101-student\",\n" +
            "        \"sakai:excludeSearch\": \"true\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"sakai:category\": null,\n" +
            "        \"sakai:group-description\": null,\n" +
            "        \"sakai:group-title\": \"math301 (Students)\",\n" +
            "        \"lastModified\": 1309560858677,\n" +
            "        \"homePath\": \"/~math301-student\",\n" +
            "        \"_path\": \"/~math301-student/public/authprofile\",\n" +
            "        \"sling:resourceType\": \"sakai/group-profile\",\n" +
            "        \"sakai:group-id\": \"math301-student\",\n" +
            "        \"createdBy\": \"admin\",\n" +
            "        \"basic\": {\n" +
            "            \"access\": \"everybody\",\n" +
            "            \"elements\": {\n" +
            "                \"lastName\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                },\n" +
            "                \"email\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                },\n" +
            "                \"firstName\": {\n" +
            "                    \"value\": \"unknown\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"created\": 1309559023157,\n" +
            "        \"lastModifiedBy\": \"admin\",\n" +
            "        \"counts\": {\n" +
            "            \"membershipsCount\": 1,\n" +
            "            \"contentCount\": 3,\n" +
            "            \"membersCount\": 1,\n" +
            "            \"countLastUpdate\": 1309750677330\n" +
            "        },\n" +
            "        \"groupid\": \"math301-student\",\n" +
            "        \"sakai:excludeSearch\": \"true\"\n" +
            "    }\n" +
            "    ],\n" +
            "    \"total\": 2\n" +
            "}</pre>"},
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      },
      parameters= {
      @ServiceParameter(name = "items", description = { "The number of items per page in the result set." }),
      @ServiceParameter(name = "page", description = { "The page number to start listing the results on." }),
      @ServiceParameter(name = "q", description = { "The Query to filter on." }),
      @ServiceParameter(name = "uid", description = {"The Optional userid to bind to, if not present the current user."})
      }
    )
  }
)
@SlingServlet(paths = { "/system/me/groups" }, generateComponent = true, generateService = true, methods = { "GET" })
@Reference(name="profileService", referenceInterface=ProfileService.class)
public class LiteMyGroupsServlet extends LiteAbstractMyGroupsServlet {
  private static final long serialVersionUID = 8809581334593701801L;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.meservice.LiteAbstractMyGroupsServlet#getGroups(org.apache.jackrabbit.api.security.user.Authorizable, org.apache.jackrabbit.api.security.user.UserManager)
   */
  @Override
  protected TreeMap<String, Group> getGroups(Authorizable member,
      AuthorizableManager userManager, final SlingHttpServletRequest request)
      throws StorageClientException, AccessDeniedException {
    TreeMap<String, Group> groups = new TreeMap<String, Group>();
    String[] principals = member.getPrincipals();
    for(String principal : principals) {
      Authorizable group = userManager.findAuthorizable(principal);
      if (group == null || !(group instanceof Group) || group.getId().equals(Group.EVERYONE)) {
        // we don't want the "everyone" group in this feed
        continue;
      }
      if (group.getProperty(UserConstants.PROP_MANAGED_GROUP) != null) {
        // fetch the group that the manager group manages
        group = userManager.findAuthorizable((String) group.getProperty(UserConstants.PROP_MANAGED_GROUP));
        if (group == null || !(group instanceof Group) || group.getProperty("sakai:group-title") == null) {
          continue;
        }
      }

      if (isPseudoGroup((Group)group) && !isManagerGroup((Group)group, userManager)) {
        // The group we want is the child of the pseudo group
        group = userManager.findAuthorizable((String) group.getProperty(UserConstants.PROP_PSEUDO_GROUP_PARENT));
      }

      // KERN-1600 Group's without a title should only be system groups for things like
      // managing contacts. The UI requires a title.
      if (group.getProperty("sakai:group-title") != null) {
        final String category = stringRequestParameter(request, "category", null);
        if (category == null) { // no filtering
          groups.put(group.getId(), (Group) group);
        } else { // KERN-1865 category filter
          if (category.equals(group.getProperty("sakai:category"))) {
            groups.put(group.getId(), (Group) group);
          }
        }
      }
    }
    return groups;
  }

}
