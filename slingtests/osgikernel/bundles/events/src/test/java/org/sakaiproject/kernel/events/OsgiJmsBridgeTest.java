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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.activemq.command.ActiveMQMapMessage;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Unit test for bridging events from OSGi to JMS.
 */
public class OsgiJmsBridgeTest {
  private static final String BROKER_URL = "tcp://localhost:61616";

  private Hashtable<Object, Object> compProps;
  private ComponentContext ctx;
  private ConnectionFactory connFactory;
  private Connection conn;
  private Session sess;
  private Topic topic;
  private MessageProducer prod;
  private MapMessage mapMessage;
  private OsgiJmsBridge bridge;
  private Event event;

  /**
   * Test handling an event with event processing turned off.
   *
   * @throws JMSException
   */
  @Test
  public void testHandleEventNoProcess() throws JMSException {
    // setup to do no processing
    setUpNoProcess();

    // start the mocks
    replay(ctx, connFactory);

    // construct and send the message
    sendMessage();

    // verify that all expected calls were made.

    // when no processing, the map message should never be instantiated and
    // should finish without exception
    assertNull(mapMessage);
  }

  /**
   * Test handling an event with full processing.
   *
   * @throws JMSException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleEvent() throws JMSException {
    // setup to do full processing
    setUpProcess();

    // start the mocks
    replay(ctx, connFactory, conn, sess, topic, prod);

    // construct and send the message
    sendMessage();

    // verify that all expected calls were made.
    verify(ctx, connFactory, conn, sess, topic, prod);

    // there should be an entry for each property plus the name of the topics
    int namesCount = 0;
    Enumeration names = mapMessage.getMapNames();
    while (names.hasMoreElements()) {
      names.nextElement();
      namesCount++;
    }
    assertEquals(3, namesCount);
  }

  /**
   * Constructs the bridge, activates it, constructs a message with 2 properties
   * and calls the bridge to handle it.
   */
  private void sendMessage() {
    bridge = new OsgiJmsBridge(connFactory, BROKER_URL);

    bridge.activate(ctx);

    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    dict.put("test", "thing");
    dict.put("another", "test");
    event = new Event("test-event", dict);
    bridge.handleEvent(event);
  }

  /**
   * Creates a dictionary of default values as found in the bridge.
   *
   * @return
   */
  private Hashtable<Object, Object> buildComponentProperties() {
    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    dict.put(OsgiJmsBridge.ACKNOWLEDGE_MODE, Session.AUTO_ACKNOWLEDGE);
    dict.put(OsgiJmsBridge.BROKER_URL, BROKER_URL);
    dict.put(OsgiJmsBridge.CONNECTION_CLIENT_ID, "sakai.event.bridge");
    dict.put(OsgiJmsBridge.EVENT_JMS_TOPIC, "sakai.event.bridge");
    dict.put(OsgiJmsBridge.PROCESS_EVENTS, "true");
    dict.put(OsgiJmsBridge.SESSION_TRANSACTED, "false");
    dict.put(OsgiJmsBridge.TOPICS, "*");
    return dict;
  }

  /**
   * Setup the needed objects for handling an event with processing turned off.
   *
   * @throws JMSException
   */
  private void setUpNoProcess() throws JMSException {
    // construct the default component properties
    compProps = buildComponentProperties();

    // set event processing to false
    compProps.put(OsgiJmsBridge.PROCESS_EVENTS, "false");

    // mock the context and expect a call to get the properties
    ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(compProps);

    // mock a connection factory
    connFactory = createMock(ConnectionFactory.class);
  }

  /**
   * Setup the needed objects for handling an enent with processing turned on.
   *
   * @throws JMSException
   */
  private void setUpProcess() throws JMSException {
    setUpNoProcess();

    // set event processing to false
    compProps.put(OsgiJmsBridge.PROCESS_EVENTS, "true");

    // mock a connection for the factory to return and expect it
    conn = createMock(Connection.class);
    expect(connFactory.createConnection()).andReturn(conn);

    // expect the client id to be set
    conn.setClientID((String) anyObject());
    expectLastCall();

    // mock a session to be returned by the connection and expect it
    sess = createMock(Session.class);
    expect(conn.createSession(false, Session.AUTO_ACKNOWLEDGE)).andReturn(sess);

    // mock a destination as a topic from the session and expect it
    topic = createMock(Topic.class);
    expect(sess.createTopic((String) anyObject())).andReturn(topic);

    // mock a producer for the session to create and expect it
    prod = createMock(MessageProducer.class);
    expect(sess.createProducer(topic)).andReturn(prod);

    // mock the return of a mapped message
    mapMessage = new ActiveMQMapMessage();
    expect(sess.createMapMessage()).andReturn(mapMessage);

    // expect the session to get run
    sess.run();
    expectLastCall();

    // expect the connection to get started
    conn.start();
    expectLastCall();

    // expect the message to be sent
    prod.send(mapMessage);
    expectLastCall();

    // expect the session to get closed
    sess.close();
    expectLastCall();

    // expect the connection to get closed
    conn.close();
    expectLastCall();
  }
}
