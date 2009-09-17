/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.smtp;

import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRoutes;

import javax.jcr.Node;

public class SmtpRouter implements MessageRouter {

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    for (MessageRoute route : routing) {
      String transport = route.getTransport();

      boolean rcptNotNull = route.getRcpt() != null;
      boolean transportNullOrInternal = transport == null || "internal".equals(transport);

      if (rcptNotNull && transportNullOrInternal) {
        // TODO check the user's profile for message delivery preference. if the
        // preference is set to smtp, change the transport to 'smtp'.
      }
    }
  }
}
