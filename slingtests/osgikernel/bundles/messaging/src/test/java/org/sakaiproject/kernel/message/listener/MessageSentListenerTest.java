package org.sakaiproject.kernel.message.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRouterManager;
import org.sakaiproject.kernel.api.message.MessageRoutes;
import org.sakaiproject.kernel.api.message.MessageTransport;

import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Property;

public class MessageSentListenerTest {
  private MessageSentListener msl;
  private MessageRouterManager messageRouterManager;

  @Before
  public void setup() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn(":admin");

    Node node = createMock(Node.class);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(prop);

    replay(prop, node);

    messageRouterManager = createMock(MessageRouterManager.class);
    expect(messageRouterManager.getMessageRouting(isA(Node.class))).andReturn(
        new MessageRoutesImpl(node));

    replay(messageRouterManager);

    msl = new MessageSentListener();
    msl.bindMessageRouterManager(messageRouterManager);
  }

  @After
  public void cleanup() {
    msl.unbindMessageRouterManager(messageRouterManager);
  }

  @Test
  public void testHandleEvent() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn(MessageConstants.SAKAI_MESSAGE_RT);

    Node node = createMock(Node.class);
    expect(node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(prop);

    Properties eventProps = new Properties();
    eventProps.put(MessageConstants.EVENT_LOCATION, node);
    Event event = new Event("myTopic", eventProps);

    MessageTransport transport = createMock(MessageTransport.class);
    transport.send(isA(MessageRoutes.class), eq(event), eq(node));
    expectLastCall();

    replay(prop, node, transport);

    msl.addTransport(transport);
    msl.handleEvent(event);
    msl.removeTransport(transport);
  }
}
