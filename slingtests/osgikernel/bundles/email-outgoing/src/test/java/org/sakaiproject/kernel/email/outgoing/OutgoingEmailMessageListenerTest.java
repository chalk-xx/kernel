package org.sakaiproject.kernel.email.outgoing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jms.Message;

public class OutgoingEmailMessageListenerTest {
  private static final String NODE_PATH_PROPERTY = "nodePath";
  private static final String PATH = "/foo";

  private OutgoingEmailMessageListener oeml;
  private Session adminSession;
  private Node messageNode;
  private Wiser wiser;

  @Before
  public void setup() throws Exception {
    oeml = new OutgoingEmailMessageListener();

    adminSession = createMock(Session.class);

    messageNode = createMock(Node.class);

    SlingRepository repository = createMock(SlingRepository.class);
    expect(repository.loginAdministrative(null)).andReturn(adminSession);

    Resource res = createMock(Resource.class);
    expect(res.adaptTo(Node.class)).andReturn(messageNode);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.getResource(PATH)).andReturn(res);

    JcrResourceResolverFactory jrrf = createMock(JcrResourceResolverFactory.class);
    expect(jrrf.getResourceResolver(adminSession)).andReturn(rr);

    oeml.bindJcrResourceResolverFactory(jrrf);
    oeml.bindRepository(repository);

    replay(adminSession, res, rr, jrrf, repository);

    wiser = new Wiser();
    wiser.setPort(OutgoingEmailMessageListener.SMTP_PORT);
    wiser.start();
  }

  @After
  public void cleanUp() {
    wiser.stop();
  }

  @Test
  public void testNoBoxParam() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        false);
    expect(
        messageNode
            .setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, "Not an outbox"))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true);

    replay(message, messageNode);

    oeml.onMessage(message);
  }

  @Test
  public void testNotOutBox() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_INBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode
            .setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, "Not an outbox"))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testNoTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
            "Message must have a to and from set")).andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true).times(2);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testNoFalse() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
            "Message must have a to and from set")).andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true).times(2);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testSingleTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn("tonobody@example.com");

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);

    replay(message, messageNode, boxName, toProp, fromProp);

    oeml.onMessage(message);

    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
    }
  }

  @Test
  public void testMultiTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Value[] toAddresses = new Value[2];
    for (int i = 0; i < toAddresses.length; i++) {
      toAddresses[i] = createMock(Value.class);
      expect(toAddresses[i].getString()).andReturn("tonobody" + i + "@example.com");
      replay(toAddresses[i]);
    }

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andThrow(new ValueFormatException());
    expect(toProp.getValues()).andReturn(toAddresses);

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp)
        .times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);

    replay(message, messageNode, boxName, toProp, fromProp);

    oeml.onMessage(message);

    int i = 0;
    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody" + i++ + "@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
    }
  }
}
