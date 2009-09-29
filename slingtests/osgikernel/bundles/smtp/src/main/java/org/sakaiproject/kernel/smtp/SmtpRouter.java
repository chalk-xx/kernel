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

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRoutes;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.message.listener.MessageRouteImpl;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class SmtpRouter implements MessageRouter {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpRouter.class);

  /**
   * The JCR Repository we access.
   * 
   */
  @Reference
  private SlingRepository slingRepository;

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    Collection<MessageRoute> rewrittenRoutes = new ArrayList<MessageRoute>();
    Iterator<MessageRoute> routeIterator = routing.iterator();
    while (routeIterator.hasNext()) {
      MessageRoute route = routeIterator.next();
      String transport = route.getTransport();

      boolean rcptNotNull = route.getRcpt() != null;
      boolean transportNullOrInternal = transport == null || "internal".equals(transport);

      if (rcptNotNull && transportNullOrInternal) {
        // check the user's profile for message delivery preference. if the
        // preference is set to smtp, change the transport to 'smtp'.
        String profilePath = PersonalUtils.getProfilePath(route.getRcpt());
        try {
          Session session = slingRepository.loginAdministrative(null);
          Node profileNode = JcrUtils.deepGetOrCreateNode(session, profilePath);
          if (PersonalUtils.getPreferredMessageTransport(profileNode) == MessageConstants.TYPE_SMTP) {
            String rcptEmailAddress = PersonalUtils.getEmailAddress(profileNode);
            MessageRoute smtpRoute = new MessageRouteImpl(MessageConstants.TYPE_SMTP
                + ":" + rcptEmailAddress);
            rewrittenRoutes.add(smtpRoute);
            routeIterator.remove();
          }
        } catch (RepositoryException e) {
          LOG.error(e.getMessage());
        }
      }
    }
    routing.addAll(rewrittenRoutes);
  }
}
