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
package org.sakaiproject.nakamura.email.outgoing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.AbstractMessageRoute;
import org.sakaiproject.nakamura.api.message.LiteMessageRouter;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.RepositoryException;

@Service
@Component(inherit = true, immediate = true)
public class LiteSmtpRouter implements LiteMessageRouter {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpRouter.class);

  /**
   * The Content Repository we access.
   *
   */
  @Reference
  private Repository contentRepository;

  /**
   * @param contentRepository
   *          the contentRepository to set
   */
  protected void bindSlingRepository(Repository contentRepository) {
    this.contentRepository = contentRepository;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Content message, MessageRoutes routing) {
    Collection<MessageRoute> rewrittenRoutes = new ArrayList<MessageRoute>();
    Iterator<MessageRoute> routeIterator = routing.iterator();
    while (routeIterator.hasNext()) {
      MessageRoute route = routeIterator.next();
      String rcpt = route.getRcpt();
      String transport = route.getTransport();

      LOG.debug("Checking Message Route {} ",route);
      boolean rcptNotNull = rcpt != null;
      boolean transportNullOrInternal = transport == null || "internal".equals(transport);

      if (rcptNotNull && transportNullOrInternal) {
        // check the user's profile for message delivery preference. if the
        // preference is set to smtp, change the transport to 'smtp'.
        try {
          Session session = contentRepository.loginAdministrative();
          Authorizable user = session.getAuthorizableManager().findAuthorizable(rcpt);
          if (user != null) {
            boolean smtpPreferred = isPreferredTransportSmtp(user);
            boolean smtpMessage = isMessageTypeSmtp(message);
            if (smtpPreferred || smtpMessage) {
              LOG.debug("Message is an SMTP Message, getting email address for the user {}", user.getId());
              // TODO PersonalUtils not yet implemented for Sparse
              // String rcptEmailAddress = PersonalUtils.getPrimaryEmailAddress(profileNode);
              String rcptEmailAddress = "someguy@example.com";

              if (rcptEmailAddress == null || rcptEmailAddress.trim().length() == 0) {
                LOG.warn("Can't find a primary email address for [" + rcpt
                    + "]; smtp message will not be sent to user.");
              } else {
                AbstractMessageRoute smtpRoute = new AbstractMessageRoute(
                    MessageConstants.TYPE_SMTP + ":" + rcptEmailAddress) {
                };
                rewrittenRoutes.add(smtpRoute);
                routeIterator.remove();
              }
            }
          }
        } catch (RepositoryException e) {
          LOG.error(e.getMessage());
        } catch (ClientPoolException e) {
          LOG.error(e.getMessage());
        } catch (StorageClientException e) {
          LOG.error(e.getMessage());
        } catch (AccessDeniedException e) {
          LOG.error(e.getMessage());
        }
      }
    }
    routing.addAll(rewrittenRoutes);

    LOG.debug("Final Routing is [{}]", Arrays.toString(routing.toArray(new MessageRoute[routing.size()])));
  }

  private boolean isMessageTypeSmtp(Content message) throws RepositoryException {
    boolean isSmtp = false;

    if (message != null && message.hasProperty(MessageConstants.PROP_SAKAI_TYPE)) {
      String prop = (String) message.getProperty(MessageConstants.PROP_SAKAI_TYPE);
      isSmtp = MessageConstants.TYPE_SMTP.equals(prop);
    }


    return isSmtp;
  }

  private boolean isPreferredTransportSmtp(Authorizable user) throws RepositoryException {
    boolean prefersSmtp = false;

    if (user != null) {
      // TODO BL120 Get this user's preferred message transport
//      String transport = PersonalUtils.getPreferredMessageTransport(profileNode);
      String transport = MessageConstants.TYPE_INTERNAL;
      prefersSmtp = MessageConstants.TYPE_SMTP.equals(transport);
    }

    return prefersSmtp;
  }
}
