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
package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, label = "ConnectionSearchPropertyProvider", description= "Provides properties to handle connection searches.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"), 
    @Property(name = "sakai.search.provider", value="Connection")
})
@Service(value = SearchPropertyProvider.class)
public class ConnectionSearchPropertyProvider implements SearchPropertyProvider {

  @Reference
  protected ConnectionManager connectionManager;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      String user = request.getRemoteUser();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable auMe = um.getAuthorizable(user);
      String connectionPath = ISO9075.encodePath(ConnectionUtils
          .getConnectionPathBase(auMe));
      if ( connectionPath.startsWith("/"))  {
        connectionPath = connectionPath.substring(1);
      }
      propertiesMap.put(SEARCH_PROP_CONNECTIONSTORE, connectionPath);
      String query = getConnectionQuery(request);
      if (query == null) {
        query = "/" + connectionPath
            + "//*[@sling:resourceType=\"sakai/contact\" and  @sakai:state!=\"NONE\"]";
      }
      propertiesMap.put("_friendsQuery", query);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public String getConnectionQuery(SlingHttpServletRequest request) {
    String user = request.getRemoteUser();
    List<String> friends = connectionManager.getConnectedUsers(user,
        ConnectionState.ACCEPTED);

    RequestParameter param = request.getRequestParameter("s");
    String s = (param != null) ? param.getString() : "";
    // If our friends list is < 500 then we construct a query.
    int size = friends.size();
    if (size > 0 && size < 500) {
      StringBuilder sbQuery = new StringBuilder();
      sbQuery.append("//_user/public//*[@sling:resourceType=\"sakai/user-profile\" and ");
      sbQuery.append("(jcr:contains(@firstName, \"*").append(s).append("*\") or ");
      sbQuery.append("jcr:contains(@lastName, \"*").append(s).append("*\") or ");
      sbQuery.append("jcr:contains(@email, \"*").append(s).append("*\")) and (");
      for (String friend : friends) {
        sbQuery.append("@rep:userId=\"").append(friend).append("\" or ");
      }
      String query = sbQuery.toString();
      query = query.substring(0, sbQuery.lastIndexOf(" or "));
      query += ")] order by @firstName, @lastName ascending";
      return query;
    }
    return null;
  }
  
  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }
}
