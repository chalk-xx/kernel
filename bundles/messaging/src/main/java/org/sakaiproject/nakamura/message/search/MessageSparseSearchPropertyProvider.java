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
package org.sakaiproject.nakamura.message.search;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;

import java.util.Map;

/**
 * Provides properties to process the search
 */
@Component(immediate = true, label = "MessageSearchPropertyProvider", description = "Provides some message search properties.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides some message search properties."),
    @Property(name = "sakai.search.provider", value = "MessageSparse") })
public class MessageSparseSearchPropertyProvider implements SolrSearchPropertyProvider {

  @Reference
  LiteMessagingService messagingService;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String user = request.getRemoteUser();
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ClientUtils
        .escapeQueryChars(messagingService.getFullPathToStore(user, session)));

    RequestParameter address = request.getRequestParameter("address");
    if (address != null && !address.getString().equals("")) {
      // resolve the address by finding the authorizables.
      String addressString = address.getString();
      String storePath = messagingService.getFullPathToStore(addressString, session);
      propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE,
          ClientUtils.escapeQueryChars(storePath) + "*");
    }

    String[] categoryStrings = request.getParameterValues("category");
    if (categoryStrings != null && !categoryStrings[0].equals("") && !categoryStrings[0].equals("*")) {
      StringBuffer categoryClauseBuffer = new StringBuffer(" AND sakai\\:category:(");
      String[] commaSeparatedTerms = categoryStrings[0].split(",");
      categoryClauseBuffer.append(commaSeparatedTerms[0]);
      for (int h = 0; h < categoryStrings.length; h++) {
        commaSeparatedTerms = categoryStrings[h].split(",");
        int starter = 0;
        if (h == 0) {
          starter++;
        }
        for (int i = starter; i < commaSeparatedTerms.length; i++) {
          categoryClauseBuffer.append(" OR " + commaSeparatedTerms[i]);
        }
      }
      categoryClauseBuffer.append(")");
      propertiesMap.put("_categoryClause", categoryClauseBuffer.toString());
    } else {
      propertiesMap.put("_categoryClause", "");
    }

    RequestParameter usersParam = request.getRequestParameter("_from");
    if (usersParam != null && !usersParam.getString().equals("")) {
      String[] users = StringUtils.split(usersParam.getString(), ',');

      StringBuilder solrQuery = new StringBuilder();

      //build solr query
      solrQuery.append("from:(");
      for (int i = 0; i < users.length; i++) {
        solrQuery.append('"').append(ClientUtils.escapeQueryChars(users[i])).append('"');

        if (i < users.length - 1) {
          solrQuery.append(" OR ");
        }
      }
      solrQuery.append(")");

      propertiesMap.put("_from", solrQuery.toString());
    }
  }
}
