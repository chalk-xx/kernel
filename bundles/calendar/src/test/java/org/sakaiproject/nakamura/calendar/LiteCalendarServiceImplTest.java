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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

public class LiteCalendarServiceImplTest {
  Repository repository;
  LiteCalendarServiceImpl liteCalendarService;
  Session session;
  String testKey;

  public LiteCalendarServiceImplTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setUp() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, IOException {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    repository = baseMemoryRepository.getRepository();
    session = repository.loginAdministrative();
    testKey = "test-" + java.util.Calendar.getInstance().getTimeInMillis();
    liteCalendarService = new LiteCalendarServiceImpl();
  }

  @Test
  public void testExport() throws AccessDeniedException, StorageClientException, CalendarException {
    ContentManager contentManager = session.getContentManager();

    // Set up the content tree with simple POSTs.
    String calendarPath = testKey + "/export-calendar";
    contentManager.update(new Content(calendarPath, ImmutableMap.of(
        "sling:resourceType", (Object) "sakai/calendar"
    )));
    String eventUid = testKey + "-plain-event";
    contentManager.update(new Content(calendarPath + "/blah/blah/a-plain-event", ImmutableMap.of(
        "sling:resourceType", (Object) "sakai/calendar-event",
        "sakai:vcal-DTSTART", "20110107T172000Z",
        "sakai:vcal-DTEND", "20110107T180000Z",
        "sakai:vcal-UID", eventUid,
        "sakai:vcal-SUMMARY", "Big party"
    )));
    String veventUid = testKey + "-vevent";
    contentManager.update(new Content(calendarPath + "/blah/blah/a-vevent", ImmutableMap.of(
        "sling:resourceType", (Object) "sakai/calendar-vevent",
        "sakai:vcal-DTSTART", "20110207T172000Z",
        "sakai:vcal-DTEND", "20110207T180000Z",
        "sakai:vcal-UID", veventUid,
        "sakai:vcal-SUMMARY", "Lesser party"
    )));

    Content calendarContent = contentManager.get(calendarPath);

    // Export specifying a type.
    Calendar calendar = liteCalendarService.export(session, calendarContent, new String[] {"event"});
    assertNotNull(calendar);
    ComponentList components = calendar.getComponents();
    assertEquals(1, components.size());
    Component eventComponent = (Component) components.get(0);
    Property prop = eventComponent.getProperty(Property.UID);
    assertEquals(eventUid, prop.getValue());

    // Export without specifying a type.
    calendar = liteCalendarService.export(session, calendarContent);
    assertNotNull(calendar);
    components = calendar.getComponents();
    assertEquals(1, components.size());
    Component veventComponent = (Component) components.get(0);
    prop = veventComponent.getProperty(Property.UID);
    assertEquals(veventUid, prop.getValue());
  }

  @Test
  public void testStoreAsCalendar() throws CalendarException, IOException, ParserException {
    Calendar inputCalendar = loadTestCalendar();
    String calendarPath = testKey + "/store-as-calendar";
    Content createdContent = liteCalendarService.store(inputCalendar, session, calendarPath);
    checkStoredCalendar(inputCalendar, createdContent);
  }

  @Test
  public void testStoreAsInputStream() throws CalendarException, IOException, ParserException {
    Calendar inputCalendar = loadTestCalendar();
    InputStream calendarStream = getClass().getClassLoader().getResourceAsStream("home.ics");
    String calendarPath = testKey + "/store-as-calendar";
    Content createdContent = liteCalendarService.store(calendarStream, session, calendarPath);
    checkStoredCalendar(inputCalendar, createdContent);
  }

  @Test
  public void testStoreAsString() throws CalendarException, IOException, ParserException {
    Calendar inputCalendar = loadTestCalendar();
    InputStream in = getClass().getClassLoader().getResourceAsStream("home.ics");
    String calendarString = IOUtils.readFully(in, "UTF-8");
    String calendarPath = testKey + "/store-as-calendar";
    Content createdContent = liteCalendarService.store(calendarString, session, calendarPath);
    checkStoredCalendar(inputCalendar, createdContent);
  }

  @Test
  public void testStoreAsReader() throws CalendarException, IOException, ParserException, URISyntaxException {
    Calendar inputCalendar = loadTestCalendar();
    URI fileUri = getClass().getClassLoader().getResource("home.ics").toURI();
    File file = new File(fileUri);
    Reader calendarReader = new FileReader(file);
    String calendarPath = testKey + "/store-as-calendar";
    Content createdContent = liteCalendarService.store(calendarReader, session, calendarPath);
    checkStoredCalendar(inputCalendar, createdContent);
  }

  private Calendar loadTestCalendar() throws IOException, ParserException {
    InputStream in = getClass().getClassLoader().getResourceAsStream("home.ics");
    CalendarBuilder builder = new CalendarBuilder();
    Calendar inputCalendar = builder.build(in);
    return inputCalendar;
  }

  private void checkStoredCalendar(Calendar inputCalendar, Content createdContent) throws CalendarException {
    assertNotNull(createdContent);
    Calendar exportCalendar = liteCalendarService.export(session, createdContent);
    assertEquivalentCalendar(inputCalendar, exportCalendar);
  }

  /**
   * Calendar.equals() is too strong a check, since re-ordered properties count as
   * inequality.
   */
  private void assertEquivalentCalendar(Calendar expectedCalendar, Calendar actualCalendar) {
    List<String> expectedProperties = convertToSortedStrings(expectedCalendar.getProperties());
    List<String> actualProperties = convertToSortedStrings(actualCalendar.getProperties());
    assertEquals(expectedProperties, actualProperties);
    List<String> expectedComponents = convertToSortedStrings(expectedCalendar.getComponents());
    List<String> actualComponents = convertToSortedStrings(actualCalendar.getComponents());
    assertEquals(expectedComponents, actualComponents);
  }

  private List<String> convertToSortedStrings(PropertyList propertyList) {
    List<String> strings = Lists.newArrayListWithExpectedSize(propertyList.size());
    for (int i = 0; i < propertyList.size(); i++) {
      Property property = (Property) propertyList.get(i);
      strings.add(property.toString());
    }
    Collections.sort(strings);
    return strings;
  }

  private List<String> convertToSortedStrings(ComponentList componentList) {
    List<String> strings = Lists.newArrayListWithExpectedSize(componentList.size());
    for (int i = 0; i < componentList.size(); i++) {
      Component component = (Component) componentList.get(i);
      StringBuilder sb = new StringBuilder();
      List<String> propertyStrings = convertToSortedStrings(component.getProperties());
      for (String propertyString : propertyStrings) {
        sb.append(propertyString);
      }
      strings.add(sb.toString());
    }
    Collections.sort(strings);
    return strings;
  }

}
