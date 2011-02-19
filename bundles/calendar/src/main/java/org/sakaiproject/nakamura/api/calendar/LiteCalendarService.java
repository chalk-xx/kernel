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
package org.sakaiproject.nakamura.api.calendar;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.InputStream;
import java.io.Reader;

/**
 * A service that allows one to fetch and store calendars.
 */
public interface LiteCalendarService {

  /**
   * Builds a {@link Calendar calendar} from a {@link Content node}. The node should have a
   * resource type of sakai/calendar. Each subnode who has a resource type of
   * sakai/calendar-event will be added as a {@link VEvent VEVENT}.
   * @param session
   * @param node
   *          The content that is a parent of all the underlying event nodes.
   *
   * @return A {@link Calendar calendar} that represents the Sparse nodes.
   * @throws CalendarException
   *           Failed to export a Sparse representation to a valid Calendar representation.
   */
  Calendar export(Session session, Content node) throws CalendarException;

  /**
   * Builds a {@link Calendar calendar} from a {@link Content node}. The node should have a
   * resource type of sakai/calendar. All subnodes who's resource type match one of the
   * types in the type array 'sakai/calendar-|type|' will be used to create the calendar
   * object.
   * @param session
   * @param node
   *          The content that is a parent of all the underlying event nodes.
   * @param types
   *          An array of Strings that should be used to match the subnodes. eg: valarm,
   *          vavailability, vevent, .. {@see Component Component}
   *
   * @return A {@link Calendar calendar} that represents the Sparse nodes.
   * @throws CalendarException
   *           Failed to export a Sparse representation to a valid Calendar representation.
   */
  Calendar export(Session session, Content node, String[] types) throws CalendarException;

  /**
   * Creates a Sparse based representation of a {@link Calendar calendar}.
   *
   * @param calendar
   *          The calendar to create in Sparse {@link Content nodes}.
   * @param session
   *          The session that allows access to the Sparse repository.
   * @param path
   *          The path in Sparse storage where the calendar should be created. Given a path
   *          /foo/bar/calendar, the following structure will be created: <code>
   *          + foo
   *              + bar
   *                  + calender
   *                    sling:resourceType = sakai/calendar
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *           <code>
   * @return The top calendar {@link Content node}.
   * @throws CalendarException
   *           Something went wrong trying to create the Sparse based representation.
   */
  Content store(Calendar calendar, Session session, String path) throws CalendarException;

  /**
   * Creates a Sparse based representation of a String containing valid iCalendar data.
   *
   * @param calendar
   *          The calendar to create in Sparse {@link Content nodes}.
   * @param session
   *          The session that allows access to the JCR repository.
   * @param path
   *          The path in Sparse storage where the calendar should be created. Given a path
   *          /foo/bar/calendar, the following structure will be created: <code>
   *          + foo
   *              + bar
   *                  + calender
   *                    sling:resourceType = sakai/calendar
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *           <code>
   * @return The top calendar {@link Content node}.
   */
  Content store(String calendar, Session session, String path) throws CalendarException;

  /**
   * Creates a Sparse based representation from the specified {@link InputStream input
   * stream}.
   *
   * @param calendar
   *          The calendar to create in Sparse {@link Content nodes}.
   * @param session
   *          The session that allows access to the Sparse repository.
   * @param path
   *          The path in Sparse storage where the calendar should be created. Given a path
   *          /foo/bar/calendar, the following structure will be created: <code>
   *          + foo
   *              + bar
   *                  + calender
   *                    sling:resourceType = sakai/calendar
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *           <code>
   * @return The top calendar {@link Content node}.
   * @throws CalendarException
   *           Something went wrong trying to create the JCR based representation.
   */
  Content store(InputStream calendar, Session session, String path) throws CalendarException;

  /**
   * Creates a JCR based representation from the specified {@link Reader reader}.
   *
   * @param calendar
   *          The calendar to create in storage {@link Content nodes}.
   * @param session
   *          The session that allows access to the Sparse repository.
   * @param path
   *          The path in Sparse storage where the calendar should be created. Given a path
   *          /foo/bar/calendar, the following structure will be created: <code>
   *          + foo
   *              + bar
   *                  + calender
   *                    sling:resourceType = sakai/calendar
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *                    + ../event1
   *                           sling:resourceType = sakai/calendar-event
   *                           properties
   *           <code>
   * @return The top calendar {@link Content node}.
   * @throws CalendarException
   *           Something went wrong trying to create the JCR based representation.
   */
  Content store(Reader calendar, Session session, String path) throws CalendarException;

}
