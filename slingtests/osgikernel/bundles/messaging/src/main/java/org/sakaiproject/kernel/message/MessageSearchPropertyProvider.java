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
package org.sakaiproject.kernel.message;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;
import org.sakaiproject.kernel.util.PathUtils;

import java.util.Map;

import javax.jcr.Session;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="MessageSearchPropertyProvider"
 *                description="Formatter for connection search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="Message"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class MessageSearchPropertyProvider implements SearchPropertyProvider {


  private MessagingService messagingService;
  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }
  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest, java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String user = request.getRemoteUser();
    String path = request.getResource().getPath();
    path = PathUtils.removeLastElement(path);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ISO9075.encodePath(messagingService.getFullPathToStore(user, session)));
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGEROOT, ISO9075.encodePath(MessageConstants._USER_MESSAGE));
  }
}
