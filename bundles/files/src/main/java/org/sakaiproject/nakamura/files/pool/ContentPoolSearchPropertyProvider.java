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
package org.sakaiproject.nakamura.files.pool;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides some extra properties for the PooledContent searches."),
    @Property(name = "sakai.search.provider", value = "PooledContent") })
public class ContentPoolSearchPropertyProvider implements SolrSearchPropertyProvider {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(ContentPoolSearchPropertyProvider.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String userID = request.getRemoteUser();

    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));
    if (!UserConstants.ANON_USERID.equals(userID)) {
      addMyGroups(session, propertiesMap);
    }

  }

  /**
   * Gets all the declared groups for a user and adds an xpath constraint for both
   * managers and viewers to the map.
   *
   * @param session
   * @param propertiesMap
   */
  protected void addMyGroups(Session session, Map<String, String> propertiesMap) {
    String sessionUserId = session.getUserId();

    try {
      AuthorizableManager authMgr = session.getAuthorizableManager();
      Authorizable auth = authMgr.findAuthorizable(sessionUserId);

      // create the manager and viewer query parameters
      String userId = ClientUtils.escapeQueryChars(sessionUserId);
      StringBuilder managers = new StringBuilder("AND manager:(").append(userId);
      StringBuilder viewers = new StringBuilder("AND viewer:(").append(userId);

      // add groups to the parameters
      List<String> groups = new ArrayList<String>();
      groups.addAll(Arrays.asList(auth.getPrincipals()));
      // KERN-1634 'everyone' should not be considered a group
      groups.remove("everyone");
      for (String group : groups) {
        String groupId = ClientUtils.escapeQueryChars(group);
        managers.append(" OR ").append(groupId);
        viewers.append(" OR ").append(groupId);
      }

      // cap off the parameters
      managers.append(")");
      viewers.append(")");

      // add properties for query templates
      propertiesMap.put("_meManagerGroups", managers.toString());
      propertiesMap.put("_meViewerGroups", viewers.toString());
    } catch (StorageClientException e) {
      LOGGER.error("Could not get the groups for user [{}].",sessionUserId , e);
    } catch (AccessDeniedException e) {
      LOGGER.error("Could not get the groups for user [{}].",sessionUserId , e);
    }
  }
}
