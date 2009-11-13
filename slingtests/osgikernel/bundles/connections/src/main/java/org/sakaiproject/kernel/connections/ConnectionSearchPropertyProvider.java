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
package org.sakaiproject.kernel.connections;

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;

import java.util.Map;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="ConnectionSearchPropertiesProvider"
 *                description="Formatter for connection search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="Connection"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 */
public class ConnectionSearchPropertyProvider implements SearchPropertyProvider {


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest, java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String user = request.getRemoteUser();
    propertiesMap.put(SEARCH_PROP_CONNECTIONSTORE, ISO9075.encodePath(ConnectionUtils.getConnectionPathBase(user)));
  }

  


}
