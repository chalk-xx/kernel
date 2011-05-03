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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 *
 */
@Component
@Service
@Properties({
  @Property(name = "service.vendor", value = "The Sakai Foundation"),
  @Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "GroupMembers") })
public class GroupMembersSearchPropertyProvider implements SolrSearchPropertyProvider {
  private static final Logger logger = LoggerFactory
      .getLogger(GroupMembersSearchPropertyProvider.class);

  @Reference
  protected Repository repository;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
      AuthorizableManager am = session.getAuthorizableManager();

      if (request.getParameter("q") == null) {
        throw new IllegalArgumentException(
            "Must provide 'q' parameter to use for search.");
      }

      // get the request group name
      String groupName = request.getParameter("group");
      if (groupName == null) {
        throw new IllegalArgumentException("Must provide group to search within.");
      }

      // get the authorizable associated to the requested group name
      Group group = (Group) am.findAuthorizable(groupName);
      if (group == null) {
        throw new IllegalArgumentException("Unable to find group [" + groupName + "]");
      }

      LinkedHashSet<String> memberIds = new LinkedHashSet<String>();

      // collect the declared members of the requested group
      addDeclaredMembers(memberIds, group);

      boolean includeSelf = Boolean.parseBoolean(request.getParameter("includeSelf"));
      String currentUser = request.getRemoteUser();
      if (!includeSelf) {
        memberIds.remove(currentUser);
      }

      // 900 is the number raydavis said we should split on. This can be tuned as needed.
      if (memberIds.size() > 900) {
        // more than the threshold; pass along for post processing
        request.setAttribute("memberIds", memberIds);
      } else {
        // update the query to filter before writing nodes

        // start building solr query
        StringBuilder solrQuery = new StringBuilder("name:(");

        // add member id's
        Iterator<String> members = memberIds.iterator();
        while(members.hasNext()) {
          String memberId = members.next();
          solrQuery.append("'");
          solrQuery.append(ClientUtils.escapeQueryChars(memberId));
          solrQuery.append("'");

          if (members.hasNext()) {
            solrQuery.append(" OR ");
          }
        }

        // finish building solr query
        solrQuery.append(")");

        propertiesMap.put("_groupQuery", solrQuery.toString());
      }
    } catch (StorageClientException e) {
      logger.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * Add any declared members of {@link group} to {@link memberIds}.
   *
   * @param memberIds List to collect member IDs.
   * @param group Group to plunder through for member IDs.
   */
  private void addDeclaredMembers(Collection<String> memberIds, Group group) {
    String[] members = group.getPrincipals();
    for (String member : members) {
      memberIds.add(member);
    }
  }
  
}
