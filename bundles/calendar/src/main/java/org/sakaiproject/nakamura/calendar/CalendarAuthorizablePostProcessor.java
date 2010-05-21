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
package org.sakaiproject.nakamura.calendar;

import net.fortuna.ical4j.model.Calendar;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.calendar.CalendarConstants;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.CalendarService;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.util.PathUtils;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Creates a Calendar for each user/group.
 */
@Service
@Component(immediate = true)
public class CalendarAuthorizablePostProcessor implements AuthorizablePostProcessor {

  @Reference
  protected transient CalendarService calendarService;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#getSequence()
   */
  public int getSequence() {
    // We make sure that the home folders get created before we run ours.
    return 10;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session, org.apache.sling.api.SlingHttpServletRequest, java.util.List)
   */
  public void process(Authorizable authorizable, Session session, Modification change) throws Exception {
    // We only process new users/groups.
    if (ModificationType.CREATE.equals(change.getType())) {
      // Store it.
      createCalendar(authorizable, session);
    }
  }

  /**
   * Create a calenadr for an authorizable.
   * 
   * @param authorizable
   * @param session
   * @param request
   * @throws CalendarException
   * @throws RepositoryException
   */
  protected void createCalendar(Authorizable authorizable, Session session) throws CalendarException, RepositoryException {

    // The path to the calendar of an authorizable.
    String path = PersonalUtils.getHomeFolder(authorizable);
    path += "/" + CalendarConstants.SAKAI_CALENDAR_NODENAME;
    path = PathUtils.normalizePath(path);

    Calendar cal = new Calendar();
    calendarService.store(cal, session, path);

    // If the authorizable is a group, we give the group access to it.
    String[] granted = new String[] { "jcr:all" };
    AccessControlUtil.replaceAccessControlEntry(session, path, authorizable
        .getPrincipal(), granted, null, null);

    if (session.hasPendingChanges()) {
      session.save();
    }

  }
}
