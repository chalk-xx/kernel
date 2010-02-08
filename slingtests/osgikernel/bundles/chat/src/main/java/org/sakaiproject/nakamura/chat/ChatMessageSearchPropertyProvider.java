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

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.util.StringUtils;

import java.util.Map;

import javax.jcr.Session;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="MessageSearchPropertyProvider"
 *                description="Formatter for connection search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="ChatMessage"
 * @scr.service interface="org.sakaiproject.nakamura.api.search.SearchPropertyProvider"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.nakamura.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class ChatMessageSearchPropertyProvider implements SearchPropertyProvider {

  private MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String user = request.getRemoteUser();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ISO9075
        .encodePath(messagingService.getFullPathToStore(user, session)));
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGEROOT, ISO9075
        .encodePath(MessageConstants._USER_MESSAGE));

    RequestParameter usersParam = request.getRequestParameter("_from");
    if (usersParam != null && !usersParam.getString().equals("")) {
      StringBuilder sql = new StringBuilder(" and ((");
      String[] users = StringUtils.split(usersParam.getString(), ',');

      for (String u : users) {
        sql.append("@sakai:from=\"").append(u).append("\" or ");
      }
      sql.append("@sakai:from=\"").append(user).append("\") or (");

      for (String u : users) {
        sql.append("@sakai:to=\"").append(u).append("\" or ");
      }
      sql.append("@sakai:to=\"").append(user).append("\"))");

      propertiesMap.put("_from", sql.toString());
    }
  }
}
