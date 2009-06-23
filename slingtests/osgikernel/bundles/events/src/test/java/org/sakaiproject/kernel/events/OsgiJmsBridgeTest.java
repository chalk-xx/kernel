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
package org.sakaiproject.kernel.events;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;

import java.util.Hashtable;

import javax.jms.Session;

/**
 *
 */
public class OsgiJmsBridgeTest {
  private Hashtable<Object, Object> defaultCompProps;
  private OsgiJmsBridge bridge;
  private Event event;
  private ComponentContext ctx;

  @Before
  public void setUp() {
    defaultCompProps = buildComponentProperties();

    ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(defaultCompProps);

    bridge = new OsgiJmsBridge();
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testHandleEvent() {
    bridge.activate(ctx);

    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    event = new Event("Test Event", dict);
    bridge.handleEvent(event);
  }

  /**
   * @return
   */
  private Hashtable<Object, Object> buildComponentProperties() {
    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    dict.put(OsgiJmsBridge.ACKNOWLEDGE_MODE, Session.AUTO_ACKNOWLEDGE);
    dict.put(OsgiJmsBridge.BROKER_URL, "tcp://localhost:61616");
    dict.put(OsgiJmsBridge.CONNECTION_CLIENT_ID, "sakai.event.bridge");
    dict.put(OsgiJmsBridge.EVENT_JMS_TOPIC, "sakai.event.bridge");
    dict.put(OsgiJmsBridge.PROCESS_EVENTS, Boolean.FALSE);
    dict.put(OsgiJmsBridge.SESSION_TRANSACTED, Boolean.FALSE);
    dict.put(OsgiJmsBridge.TOPICS, "*");
    return dict;
  }
}
