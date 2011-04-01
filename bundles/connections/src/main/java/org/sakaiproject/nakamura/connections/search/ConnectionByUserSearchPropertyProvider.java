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
package org.sakaiproject.nakamura.connections.search;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.connections.ConnectionUtils;

import java.util.Map;

@Component(label = "ConnectionSearchPropertyProvider", description= "Provides properties to handle connection searches.")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value="ConnectionByUserid")})
public class ConnectionByUserSearchPropertyProvider implements SolrSearchPropertyProvider {

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    RequestParameter userParameter = request.getRequestParameter("userid");
    if (userParameter != null) {
      String user = userParameter.getString();
      String connectionPath = ClientUtils.escapeQueryChars(ConnectionUtils
          .getConnectionPathBase(user));
      if ( connectionPath.startsWith("/"))  {
        connectionPath = connectionPath.substring(1);
      }
      propertiesMap.put(SEARCH_PROP_CONNECTIONSTORE, connectionPath);
    }
  }
}
