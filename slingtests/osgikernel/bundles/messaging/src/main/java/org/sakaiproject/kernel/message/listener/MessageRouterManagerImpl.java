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
package org.sakaiproject.kernel.message.listener;

import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRouterManager;
import org.sakaiproject.kernel.api.message.MessageRoutes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * 
 * @scr.component inherit="true" label="%sakai-manager.name" immediate="true"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageRouterManager"
 * @scr.property name="service.description"
 *               value="Manages Routing"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="messageRouters"
 *                interface="org.sakaiproject.kernel.api.message.MessageTransport"
 *                policy="dynamic" cardinality="0..n" bind="addMessageRouter"
 *                unbind="removeMessageRouter"
 */
public class MessageRouterManagerImpl implements MessageRouterManager {

  private List<MessageRouter> routers = new ArrayList<MessageRouter>();
  private Set<MessageRouter> routerList = new HashSet<MessageRouter>();


  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   * @see org.sakaiproject.kernel.api.message.MessageRouterManager#getMessageRouting(javax.jcr.Node)
   */
  public MessageRoutes getMessageRouting(Node n) throws RepositoryException {
    MessageRoutes routing = new MessageRoutesImpl(n);
    for ( MessageRouter messageRouter : routers ) {
      messageRouter.route(n,routing); 
    }
    return routing;
  }
  
  public void addMessageRouter(MessageRouter router ) {
    routerList.add(router);
    routers = getSortedRouterList();
  }
  
  public void removeMessageRouter(MessageRouter router ) {
    routerList.remove(router);
    routers = getSortedRouterList();
  }

  /**
   * @return
   */
  private List<MessageRouter> getSortedRouterList() {
    
    List<MessageRouter> sortedRouterList = new ArrayList<MessageRouter>(routerList);
    Collections.sort(sortedRouterList,new Comparator<MessageRouter>() {

      public int compare(MessageRouter o1, MessageRouter o2) {
        return o1.getPriority() - o2.getPriority();
      }
    });
    return sortedRouterList;
  }
  

}
