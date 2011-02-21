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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_PROPERTY_PREFIX;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_RT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_RT;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DateProperty;

import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.LiteCalendarService;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.DateUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * TODO Does not yet correctly distinguish between iCal4j Date and DateTime properties.
 * TODO Does not yet handle TimeZone.
 * TODO Does not yet do anything special with DateListProperty values.
 */
@org.apache.felix.scr.annotations.Component(immediate = true)
@Service(value = LiteCalendarService.class)
public class LiteCalendarServiceImpl implements LiteCalendarService {
  public static final Logger LOGGER = LoggerFactory.getLogger(LiteCalendarServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#export(Session, org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public Calendar export(Session session, Content node) throws CalendarException {
    return export(session, node, new String[] { VEvent.VEVENT });
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#export(Session, org.sakaiproject.nakamura.api.lite.content.Content, java.lang.String[])
   */
  public Calendar export(Session session, Content node, String[] types) throws CalendarException {
    // Translate input type strings (e.g., "VEVENT") into Sling resource types
    // (e.g., "sakai/calendar-vevent").
    Set<String> wantedResourceTypes = Sets.newHashSetWithExpectedSize(types.length);
    for (String type : types) {
      wantedResourceTypes.add(SAKAI_CALENDAR_RT + "-" + type.toLowerCase());
    }

    // Start constructing the iCal Calendar.
    Calendar calendar = new Calendar();
    PropertyFactory propFactory = PropertyFactoryImpl.getInstance();
    try {
      // Add any Calendar properties.
      addNodePropertiesToCal(node, calendar.getProperties(), propFactory);

      // Traverse the tree.
      Iterable<Content> children = node.listChildren();
      for (Content childContent : children) {
        recurseForEvents(childContent, calendar, propFactory, wantedResourceTypes);
      }
    } catch (IOException e) {
      LOGGER.error("Caught an IOException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (URISyntaxException e) {
      LOGGER.error("Caught a URISyntaxException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParseException e) {
      LOGGER.error("Caught a ParseException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    }

    return calendar;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#store(net.fortuna.ical4j.model.Calendar, org.sakaiproject.nakamura.api.lite.Session, java.lang.String)
   */
  public Content store(Calendar calendar, Session session, String path)
      throws CalendarException {
    final Content returnNode;
    try {
      ContentManager contentManager = session.getContentManager();
      Content calendarNode = new Content(path, ImmutableMap.of(
          SLING_RESOURCE_TYPE_PROPERTY, (Object) SAKAI_CALENDAR_RT
      ));

      addCalPropertiesToNode(calendar.getProperties(), calendarNode);
      contentManager.update(calendarNode);

      // Now loop over all the events and store these.
      // We could do calendar.getComponents(Component.VEVENT) but we will choose
      // everything. They can be filtered in the export method.
      @SuppressWarnings("unchecked")
      Iterator<CalendarComponent> eventIter = (Iterator<CalendarComponent>) calendar.getComponents().iterator();
      while (eventIter.hasNext()) {
        CalendarComponent component = eventIter.next();
        storeEvent(calendarNode, component, session);
      }

      // We need to fetch the node from storage for its internal structure to be
      // set up correctly.
      returnNode = contentManager.get(path);
    } catch (StorageClientException e) {
      LOGGER.error("Caught StorageClientException when trying to store a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (AccessDeniedException e) {
      LOGGER.error("Caught AccessDeniedException when trying to store a calendar", e);
      throw new CalendarException(500, e.getMessage());
    }
    return returnNode;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#store(java.lang.String, org.sakaiproject.nakamura.api.lite.Session, java.lang.String)
   */
  public Content store(String calendar, Session session, String path)
      throws CalendarException {
    ByteArrayInputStream in = new ByteArrayInputStream(calendar.getBytes());
    return store(in, session, path);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#store(java.io.InputStream, org.sakaiproject.nakamura.api.lite.Session, java.lang.String)
   */
  public Content store(InputStream calendar, Session session, String path)
      throws CalendarException {
    CalendarBuilder builder = new CalendarBuilder();
    try {
      Calendar inputCalendar = builder.build(calendar);
      return store(inputCalendar, session, path);
    } catch (IOException e) {
      LOGGER.error(
          "Caught an IOException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParserException e) {
      LOGGER.error(
          "Caught a ParserException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.calendar.LiteCalendarService#store(java.io.Reader, org.sakaiproject.nakamura.api.lite.Session, java.lang.String)
   */
  public Content store(Reader calendar, Session session, String path)
      throws CalendarException {
    CalendarBuilder builder = new CalendarBuilder();
    try {
      Calendar inputCalendar = builder.build(calendar);
      return store(inputCalendar, session, path);
    } catch (IOException e) {
      LOGGER.error(
          "Caught an IOException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParserException e) {
      LOGGER.error(
          "Caught a ParserException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    }
  }

  private void storeEvent(Content calendarNode, CalendarComponent component, Session session)
      throws AccessDeniedException, StorageClientException {
    // Get the start date.
    final CalendarSubPathProducer producer = new CalendarSubPathProducer(component);

    // Initialize the event node.
    final String path = calendarNode.getPath() + PathUtils.getSubPath(producer);
    final String resourceType = SAKAI_CALENDAR_RT + "-" + producer.getType();
    Content eventNode = new Content(path, ImmutableMap.of(
        SLING_RESOURCE_TYPE_PROPERTY, (Object) resourceType
    ));
    addCalPropertiesToNode(component.getProperties(), eventNode);

    handlePrivacy(eventNode, component, session);

    ContentManager contentManager = session.getContentManager();
    contentManager.update(eventNode);

    // If this is an event, we add a signup node.
    if (component instanceof VEvent) {
      String signupPath = StorageClientUtils.newPath(eventNode.getPath(), SIGNUP_NODE_NAME);
      if (!contentManager.exists(signupPath)) {
        Content signupNode = new Content(signupPath, ImmutableMap.of(
            SLING_RESOURCE_TYPE_PROPERTY, (Object) SIGNUP_NODE_RT
        ));
        contentManager.update(signupNode);
      }
    }

  }

  private void handlePrivacy(Content eventNode, CalendarComponent component, Session session)
      throws StorageClientException, AccessDeniedException {
    if (component.getProperty(Clazz.CLASS) != null) {
      Clazz c = (Clazz) component.getProperty(Clazz.CLASS);
      if (c == Clazz.PRIVATE) {
        List<AclModification> aclModifications = new ArrayList<AclModification>();

        // Grant access to the current user.
        final String userId = session.getUserId();
        AclModification.addAcl(true, Permissions.CAN_ANYTHING, userId, aclModifications);

        // Deny everybody else.
        AclModification.addAcl(false, Permissions.ALL, User.ANON_USER, aclModifications);
        AclModification.addAcl(false, Permissions.ALL, Group.EVERYONE, aclModifications);

        session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, eventNode.getPath(),
            aclModifications.toArray(new AclModification[aclModifications.size()]));
      }
    }
  }

  private void recurseForEvents(Content node, Calendar calendar, PropertyFactory propFactory,
      Set<String> wantedResourceTypes) throws IOException, URISyntaxException, ParseException {
    final String resourceType = (String) node.getProperty(SLING_RESOURCE_TYPE_PROPERTY);
    if (resourceType != null && wantedResourceTypes.contains(resourceType)) {
      // Treat the content as event storage.
      PropertyList eventProperties = new PropertyList();
      addNodePropertiesToCal(node, eventProperties, propFactory);
      VEvent event = new VEvent(eventProperties);

      // Add the event to the calendar.
      calendar.getComponents().add(event);
    } else {
      Iterable<Content> children = node.listChildren();
      for (Content childContent : children) {
        recurseForEvents(childContent, calendar, propFactory, wantedResourceTypes);
      }
    }
  }

  private void addCalPropertiesToNode(PropertyList propertyList, Content node) {
    @SuppressWarnings("unchecked")
    Iterator<Property> propIter = (Iterator<Property>) propertyList.iterator();
    while (propIter.hasNext()) {
      final Property calProp = propIter.next();
      final String key = SAKAI_CALENDAR_PROPERTY_PREFIX + calProp.getName();
      final Object value;
      if (calProp instanceof DateProperty) {
        final Date date = ((DateProperty) calProp).getDate();
        // TODO Need to handle TimeZone, DateTime/Date distinction.
        // value = Dates.getCalendarInstance(date);
        java.util.Calendar javaCal = java.util.Calendar.getInstance();
        javaCal.setTime(date);
        value = javaCal;
      } else {
        value = calProp.getValue();
      }
      node.setProperty(key, value);
    }
  }

  private void addNodePropertiesToCal(Content node, PropertyList propertyList,
      PropertyFactory propFactory) throws IOException, URISyntaxException, ParseException {
    final Map<String, Object> props = node.getProperties();
    for (final Entry<String, Object> entry : props.entrySet()) {
      if (entry.getKey().startsWith(SAKAI_CALENDAR_PROPERTY_PREFIX)) {
        // Get the name of the property and strip off the prefix.
        final String propName = entry.getKey().substring(SAKAI_CALENDAR_PROPERTY_PREFIX.length());
        final Object rawValue = entry.getValue();

        // Create an iCal property and add it to the event properties.
        Property calProp = propFactory.createProperty(propName);

        final String stringValue;
        if (rawValue == null) {
          stringValue = null;
        } else  if (rawValue instanceof java.util.Calendar) {
          // TODO Need to handle TimeZone, DateTime/Date distinction,
          // and become much smarter about ical's formatting.
          stringValue = DateUtils.rfc2445((java.util.Calendar) rawValue);
        } else {
          stringValue = String.valueOf(rawValue);
        }
        calProp.setValue(stringValue);
        propertyList.add(calProp);
      }
    }
  }

}
