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
package org.sakaiproject.nakamura.message;

import com.google.common.collect.ImmutableMap;

import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This PostProcessor listens to post operations on User objects and creates a message
 * store.
 */
@Component(immediate = true, label = "LiteMessageAuthorizablePostProcessor", description = "Creates the message stores for users and groups.", metatype = false)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Creates the message stores for users and groups."),
    @Property(name = "service.ranking", intValue=10)})
 public class LiteMessageAuthorizablePostProcessor implements LiteAuthorizablePostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteMessageAuthorizablePostProcessor.class);

  @Reference
  protected transient LiteMessagingService messagingService;

  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    LOGGER.debug("Starting MessageAuthorizablePostProcessor process");
    if (ModificationType.CREATE.equals(change.getType())) {
      if (authorizable != null) {
        String authorizableId =  authorizable.getId();
        if (authorizableId != null) {
          String path = messagingService.getFullPathToStore(authorizableId, session);
          LOGGER.debug("Creating message store node: {}", path);
          Map<String, Object> messageStoreProperties = ImmutableMap.of(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              StorageClientUtils.toStore(MessageConstants.SAKAI_MESSAGESTORE_RT));
          Content messageStore = new Content(path, messageStoreProperties);

          List<AclModification> acls = new ArrayList<AclModification>();
          if (!User.ANON_USER.equals(authorizableId)) {
            AclModification.addAcl(true, Permissions.CAN_ANYTHING, authorizableId, acls);
          }
          // explicitly deny anon and everyone, this is private space.
          AclModification.addAcl(false, Permissions.CAN_ANYTHING, User.ANON_USER, acls);
          AclModification.addAcl(false, Permissions.CAN_ANYTHING, Group.EVERYONE, acls);

          session.getContentManager().update(messageStore);
          session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, path,
              acls.toArray(new AclModification[acls.size()]));
        }
      }
    }
  }

}
