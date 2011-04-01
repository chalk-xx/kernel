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
package org.sakaiproject.nakamura.personal;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.personal.PersonalTrackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

@Component(immediate = true)
@Service(value = PersonalTrackingStore.class)
public class LitePersonalTrackingStore implements PersonalTrackingStore {

  private static final Logger LOG = LoggerFactory
      .getLogger(LitePersonalTrackingStore.class);

  @Reference
  private transient Repository repository;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.personal.PersonalTrackingStore#recordActivity(java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String, java.util.Date)
   */
  public void recordActivity(String resourceId, String resourceType, String activityType,
      String userId, Calendar timestamp) {
    Session session = null;
    try {
      session = repository.loginAdministrative();
      final ContentManager cm = session.getContentManager();
      final String trackingNodePath = "/activity/" + resourceType + "/" + resourceId;
      Content trackingNode = null;
      if (cm.exists(trackingNodePath)) {
        trackingNode = cm.get(trackingNodePath);
      } else {
        trackingNode = new Content(trackingNodePath, new HashMap<String, Object>());
      }
      if (!trackingNode.hasProperty("count")) {
        trackingNode.setProperty("count", BigDecimal.ZERO);
      }
      if (!trackingNode.hasProperty("sling:resourceType")) {
        trackingNode.setProperty("sling:resourceType", "sakai/resource-activity");
      }
      final String generatedNodeName = Base64
          .encodeBase64URLSafeString(asShorterByteArray(UUID.randomUUID()));
      final String activityNodePath = trackingNodePath + "/" + generatedNodeName;
      Content activityNode = null;
      if (cm.exists(activityNodePath)) {
        activityNode = cm.get(activityNodePath);
      } else {
        activityNode = new Content(activityNodePath, new HashMap<String, Object>());
      }
      BigDecimal activityCount = (BigDecimal) trackingNode.getProperty("count");
      activityNode.setProperty("sling:resourceType", "sakai/resource-update");
      trackingNode.setProperty("count", activityCount.add(BigDecimal.ONE));
      activityNode.setProperty("resourceId", resourceId);
      activityNode.setProperty("resourcetype", resourceType);
      activityNode.setProperty("activitytype", activityType);
      activityNode.setProperty("timestamp", timestamp);
      activityNode.setProperty("userid", userId);
      cm.update(activityNode);
      cm.update(trackingNode);
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          LOG.error(e.getLocalizedMessage(), e);
          throw new IllegalStateException(e);
        }
      }
    }

  }

  private byte[] asShorterByteArray(UUID uuid) {

    long msb = uuid.getMostSignificantBits();
    byte[] buffer = new byte[8];

    for (int i = 0; i < 8; i++) {
      buffer[i] = (byte) (msb >>> 8 * (7 - i));
    }

    return buffer;

  }

}
