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
package org.sakaiproject.nakamura.files.search;

import com.google.common.base.Join;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides properties to process the search
 *
 */
@Component(label = "FileSearchPropertyProvider", description = "Property provider for file searches")
@Service
@Properties({
  @Property(name = "service.vendor", value = "The Sakai Foundation"),
  @Property(name = "sakai.search.provider", value = "MeManagerViewer") })
public class MeManagerViewerSearchPropertyProvider implements SolrSearchPropertyProvider {

  @Reference
  protected ConnectionManager connectionManager;

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    
    String user = request.getRemoteUser();
    RequestParameter useridParam = request.getRequestParameter("userid");
    if (useridParam != null) {
      user = useridParam.getString();
    }
    
    if (User.ANON_USER.equals(user)) {
      // stop here, anonymous is not a manager or a viewer of anything
      return;
    }
    javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session =
      StorageClientUtils.adaptToSession(jcrSession);
    try {
      AuthorizableManager authManager = session.getAuthorizableManager();
      Authorizable userAuthorizable = authManager.findAuthorizable(user);
      if (userAuthorizable != null) {
        List<String> viewerAndManagerPrincipals = new ArrayList<String>();
        for (String principal : userAuthorizable.getPrincipals()) {
          viewerAndManagerPrincipals.add(ClientUtils.escapeQueryChars(principal));
        }
        viewerAndManagerPrincipals.remove("everyone");
        viewerAndManagerPrincipals.add(ClientUtils.escapeQueryChars(user));

        propertiesMap.put("au", Join.join(" OR ", viewerAndManagerPrincipals));
      }
    } catch (StorageClientException e) {
      throw new RuntimeException(e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    }


  }

}
