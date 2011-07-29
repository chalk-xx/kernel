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
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Iterator;
import java.util.TreeMap;

@ServiceDocumentation(
  name = "My Managed Groups Servlet", okForVersion = "0.11",
  shortDescription = "Gets the groups where the current user is a manager",
  description = "Gets the groups where the current user is a manager",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE, bindings = { "system/me/managedgroups" }
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = {"Get the groups this user manages.",
          "curl -u zach:zach http://localhost:8080/system/me/managedgroups.tidy.json" +
          "<pre>{\n" +
            "    \"items\": 25,\n" +
            "    \"results\": [{\n" +
            "        \"sakai:group-description\": \"This is a good college-level introduction to cool stuff about math.\",\n" +
            "        \"sakai:category\": \"courses\",\n" +
            "        \"sakai:group-title\": \"math101\",\n" +
            "        \"lastModified\": 1309748555249,\n" +
            "        \"sakai:group-joinable\": \"no\",\n" +
            "        \"homePath\": \"/~math101\",\n" +
            "        \"_path\": \"/~math101/public/authprofile\",\n" +
            "        \"sling:resourceType\": \"sakai/group-profile\",\n" +
            "        \"sakai:group-id\": \"math101\",\n" +
            "        \"createdBy\": \"admin\",\n" +
            "        \"created\": 1309556505076,\n" +
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
            "        \"lastModifiedBy\": \"admin\",\n" +
            "        \"counts\": {\n" +
            "            \"membershipsCount\": 0,\n" +
            "            \"contentCount\": 0,\n" +
            "            \"membersCount\": 2,\n" +
            "            \"countLastUpdate\": 1309748555248\n" +
            "        },\n" +
            "        \"groupid\": \"math101\",\n" +
            "        \"sakai:excludeSearch\": false,\n" +
            "        \"sakai:group-visible\": \"members-only\"\n" +
            "    }],\n" +
            "    \"total\": 1\n" +
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
@SlingServlet(paths = { "/system/me/managedgroups" }, generateComponent = true, generateService = true, methods = { "GET" })
@Reference(name="profileService", referenceInterface=ProfileService.class)
public class LiteMyManagedGroupsServlet extends LiteAbstractMyGroupsServlet {
  private static final long serialVersionUID = 5286762541480563822L;
  @Override
  protected TreeMap<String, Group> getGroups(Authorizable member,
      AuthorizableManager userManager, SlingHttpServletRequest request)
      throws StorageClientException, AccessDeniedException {
    TreeMap<String, Group> managedGroups = new TreeMap<String, Group>();
    Iterator<Group> allGroupsIter = member.memberOf(userManager);
    for (String principal : member.getPrincipals()) {
      Group group = (Group)userManager.findAuthorizable(principal);
      if (group != null && !group.getId().equals(Group.EVERYONE)) {

        boolean isManager = false;

        if (isPseudoGroup(group) && isManagerGroup(group, userManager)) {
          // The group we want is the child of the pseudo group
          isManager = true;
          group = (Group)userManager.findAuthorizable((String) group.getProperty(UserConstants.PROP_PSEUDO_GROUP_PARENT));
        } else {
          for(String managerId : StorageClientUtils.nonNullStringArray(
            (String[]) group.getProperty(UserConstants.PROP_GROUP_MANAGERS))) {
            if (member.getId().equals(managerId)) {
              isManager = true;
              break;
            }
          }
        }
              
        if (isManager) {
          final String category = stringRequestParameter(request, "category", null);
          if (category == null) { // no filtering
            managedGroups.put(group.getId(), group);
          } else { // KERN-1865 category filter
            if (category.equals(group.getProperty("sakai:category"))) {
              managedGroups.put(group.getId(), group);
            }
          }
        }
      }
    }

    return managedGroups;
  }

}
