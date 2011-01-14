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
package org.sakaiproject.nakamura.message.listener;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component(inherit = true, label = "%sakai-manager.name", immediate = true)
@Service
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Manages messaging routing.") })
@Reference(name = "messageRouters", referenceInterface = LiteMessageRouter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addMessageRouter", unbind = "removeMessageRouter")
public class LiteMessageRouterManagerImpl implements LiteMessageRouterManager {

  private List<LiteMessageRouter> routers = new ArrayList<LiteMessageRouter>();
  private Set<LiteMessageRouter> messageRouters = new HashSet<LiteMessageRouter>();


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessageRouterManager#getMessageRouting(Content)
   */
  public MessageRoutes getMessageRouting(Content message) {
    MessageRoutes routing = new LiteMessageRoutesImpl(message);
    for ( LiteMessageRouter messageRouter : routers ) {
      messageRouter.route(message,routing);
    }
    return routing;
  }

  protected void addMessageRouter(LiteMessageRouter router ) {
    messageRouters.add(router);
    routers = getSortedRouterList();
  }

  protected void removeMessageRouter(LiteMessageRouter router ) {
    messageRouters.remove(router);
    routers = getSortedRouterList();
  }

  /**
   * @return list of message routers sorted by priority
   */
  protected List<LiteMessageRouter> getSortedRouterList() {

    List<LiteMessageRouter> sortedRouterList = new ArrayList<LiteMessageRouter>(messageRouters);
    Collections.sort(sortedRouterList,new Comparator<LiteMessageRouter>() {

      public int compare(LiteMessageRouter o1, LiteMessageRouter o2) {
        return o2.getPriority() - o1.getPriority();
      }
    });
    return sortedRouterList;
  }


}
