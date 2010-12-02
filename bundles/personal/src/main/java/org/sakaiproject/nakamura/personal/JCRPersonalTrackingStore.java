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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.personal.PersonalTrackingStore;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.math.BigDecimal;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true)
@Service(value = PersonalTrackingStore.class)
public class JCRPersonalTrackingStore implements PersonalTrackingStore {
  
  @Reference
  private transient SlingRepository slingRepository;
  

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.personal.PersonalTrackingStore#recordActivity(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Date)
   */
  public void recordActivity(String resourceId, String resourceType, String activityType,
      String userId, Date timestamp) {
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      Node activityNode = JcrUtils.deepGetOrCreateNode(session, "/activity/" + resourceType + "/" + resourceId, "nt:unstructured");
      if (!activityNode.hasProperty("count")) {
        activityNode.setProperty("count", 0);
      }
      
      BigDecimal activityCount = activityNode.getProperty("count").getDecimal();
      activityNode.setProperty("count", activityCount.add(BigDecimal.ONE));
      
      activityNode.setProperty("resourceType", resourceType);
      activityNode.setProperty("activityType", activityType);
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
    } finally {
      if (session != null) {
        session.logout();
      }
    }

  }

}
