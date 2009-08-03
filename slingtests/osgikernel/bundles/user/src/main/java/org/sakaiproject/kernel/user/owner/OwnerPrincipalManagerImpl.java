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
package org.sakaiproject.kernel.user.owner;

import static org.sakaiproject.kernel.api.user.UserConstants.JCR_CREATED_BY;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * The <code>OwnerPrincipalManager</code>
 * 
 * @scr.component immediate="true" label="OwnerPrincipalManagerImpl"
 *                description="Implementation of the Dynamic Principal Manager Service"
 *                name=
 *                "org.sakaiproject.kernel.ownerprincipalmanager.OwnerPrincipalManagerImpl"
 * @scr.service interface=
 *              "org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Owner Principal Manager Implementation"
 */
public class OwnerPrincipalManagerImpl implements DynamicPrincipalManager {

  /**
   *
   */
  private static Logger LOG = LoggerFactory.getLogger(OwnerPrincipalManagerImpl.class);

  public boolean hasPrincipalInContext(String principalName, Node aclNode, String userId) {
    try {
      if ( userId == null ) {
        return false;
      }
      if ("owner".equals(principalName)) {
        /* Request comes in at node/rep:policy */
        Node contextNode = aclNode.getParent();
        
        LOG.info("Granting .owner privs to node owner");
        if (contextNode.hasProperty(JCR_CREATED_BY)) {
          Property owner = contextNode.getProperty(JCR_CREATED_BY);
          String ownerName = owner.getString();
          LOG.info("Got node owner: " + ownerName);
          LOG.info("Got current user: " + userId);
          if (userId.equals(ownerName)) {
            return true;
          }
          LOG.info(ownerName + " didn't match " + userId);
        } else {
          LOG.info("Node: {}  has no {} property", contextNode.getPath(), JCR_CREATED_BY);
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Unable to determine node ownership", e);
    }
    return false;
  }

}
