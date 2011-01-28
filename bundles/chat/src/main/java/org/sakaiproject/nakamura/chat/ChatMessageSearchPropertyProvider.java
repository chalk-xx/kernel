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
package org.sakaiproject.nakamura.chat;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;

import java.util.Map;

@Component(label = "MessageSearchPropertyProvider", description = "Provides properties to process the chat message searches.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "ChatMessage"),
    @Property(name = "service.description", value = "Provides properties to process the chat message searches.") })
public class ChatMessageSearchPropertyProvider implements SolrSearchPropertyProvider {

//  private static final Logger LOG = LoggerFactory.getLogger(ChatMessageSearchPropertyProvider.class);

  @Reference
  protected transient LiteMessagingService messagingService;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    // TODO BL120 figure out what search properties (if any) will be needed for the sparse version of this search
    // in other words, no more JCR query stuff here.
//    try {
//      String user = request.getRemoteUser();
//      Session session = request.getResourceResolver().adaptTo(Session.class);
//      propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ISO9075
//          .encodePath(messagingService.getFullPathToStore(user, session)));
//
//      RequestParameter usersParam = request.getRequestParameter("_from");
//      if (usersParam != null && !usersParam.getString().equals("")) {
//        StringBuilder sql = new StringBuilder(" and ((");
//        String[] users = StringUtils.split(usersParam.getString(), ',');
//
//        for (String u : users) {
//          sql.append("@sakai:from=\"").append(escapeString(u, Query.XPATH)).append("\" or ");
//        }
//        sql.append("@sakai:from=\"").append(escapeString(user, Query.XPATH)).append("\") or (");
//
//        for (String u : users) {
//          sql.append("@sakai:to=\"").append(escapeString(u, Query.XPATH)).append("\" or ");
//        }
//        sql.append("@sakai:to=\"").append(escapeString(user, Query.XPATH)).append("\"))");
//
//        propertiesMap.put("_from", sql.toString());
//      }
//    } catch (MessagingException e) {
//      LOG.error(e.getLocalizedMessage());
//    }
  }
}
