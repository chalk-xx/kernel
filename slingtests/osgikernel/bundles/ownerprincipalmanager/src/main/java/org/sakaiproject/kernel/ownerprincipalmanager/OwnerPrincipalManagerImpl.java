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
package org.sakaiproject.kernel.ownerprincipalmanager;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.sakaiproject.kernel.api.session.SessionManagerService;
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
 *                name="org.sakaiproject.kernel.ownerprincipalmanager.OwnerPrincipalManagerImpl"
 * @scr.service 
 *              interface="org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description"
 *               value="Owner Principal Manager Implementation"
 * @scr.reference name="sessionManagerService"
 *                interface="org.sakaiproject.kernel.api.session.SessionManagerService"
 *                bind="bindSessionManagerService"
 *                unbind="unbindSessionManagerService"
 */
public class OwnerPrincipalManagerImpl implements DynamicPrincipalManager {

  private static Logger LOG = LoggerFactory.getLogger(OwnerPrincipalManagerImpl.class);
  protected SessionManagerService sessionManagerService;
  
  public boolean hasPrincipalInContext(String principalName, Node aclNode) {
    try {
      if (".owner".equals(principalName))
      {
        /* Request comes in at node/rep:policy */
        Node contextNode = aclNode.getParent();
        LOG.info("Granting .owner privs to node owner");
        if (contextNode.hasProperty("jcr:createdBy"))
        {
          Property owner = contextNode.getProperty("jcr:createdBy");
          String ownerName = owner.getString();
          LOG.info("Got node owner: " + ownerName);
          String currentUser = sessionManagerService.getCurrentUserId();
          LOG.info("Got current user: " + currentUser);
          if (currentUser.equals(ownerName))
          {
            return true;
          } 
          LOG.info(ownerName + " didn't match " + currentUser);
        }
        else
        {
          LOG.info("Node: " + contextNode.getPath() + " has no jcr:createdBy property");
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Unable to determine node ownership", e);
    }
    return false;
  }

  protected void bindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = sessionManagerService;
  }

  protected void unbindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = null;
  }


}
