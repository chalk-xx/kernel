package org.sakaiproject.nakamura.email.outgoing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.personal.PersonalConstants;
import org.sakaiproject.nakamura.message.listener.MessageRoutesImpl;
import org.sakaiproject.nakamura.util.PathUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class SmtpRouterTest {
  private SmtpRouter smtpRouter;
  private Session session;

  @Before
  public void setup() throws Exception {
    smtpRouter = new SmtpRouter();

    session = createMock(Session.class);

    SlingRepository slingRepository = createMock(SlingRepository.class);
    expect(slingRepository.loginAdministrative(null)).andReturn(session);

    replay(slingRepository);
    smtpRouter.bindSlingRepository(slingRepository);
  }

  @Test
  public void testRewriteSmtp() throws Exception {
    String username = "foo";
    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn(username);

    Node routeNode = createMock(Node.class);
    expect(routeNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);

    Property transportProp = createMock(Property.class);
    expect(transportProp.getString()).andReturn(MessageConstants.TYPE_SMTP);

    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    expect(propDef.isMultiple()).andReturn(false);

    Value emailValue = createMock(Value.class);
    expect(emailValue.getString()).andReturn(username + "@localhost");

    Property emailProp = createMock(Property.class);
    expect(emailProp.getDefinition()).andReturn(propDef);
    expect(emailProp.getValue()).andReturn(emailValue);

    Node authProfile = createMock(Node.class);
    expect(authProfile.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(true);
    expect(authProfile.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(transportProp);
    expect(authProfile.hasProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(true)
        .times(2);
    expect(authProfile.getProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(emailProp);

    String authProfilePath = PathUtils.toInternalHashedPath("/_user/public", username,
        PersonalConstants.AUTH_PROFILE);
    expect(session.itemExists(authProfilePath)).andReturn(true);
    expect(session.getItem(authProfilePath)).andReturn(authProfile);

    replay(toProp, routeNode, transportProp, propDef, emailValue, emailProp, authProfile,
        session);
    MessageRoutes routing = new MessageRoutesImpl(routeNode);
    smtpRouter.route(null, routing);

    for (MessageRoute route : routing) {
      assertEquals(username + "@localhost", route.getRcpt());
      assertEquals(MessageConstants.TYPE_SMTP, route.getTransport());
    }
  }

  @Test
  public void testSkipInternal() throws Exception {
    Node messageNode = createMock(Node.class);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TYPE)).andReturn(false);

    String username = "foo";
    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn(username);

    Node routeNode = createMock(Node.class);
    expect(routeNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);

    Property transportProp = createMock(Property.class);
    expect(transportProp.getString()).andReturn(MessageConstants.TYPE_INTERNAL);

    Node authProfile = createMock(Node.class);
    expect(authProfile.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(true);
    expect(authProfile.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(transportProp);

    String authProfilePath = PathUtils.toInternalHashedPath("/_user/public", username,
        PersonalConstants.AUTH_PROFILE);
    expect(session.itemExists(authProfilePath)).andReturn(true);
    expect(session.getItem(authProfilePath)).andReturn(authProfile);

    replay(messageNode, toProp, routeNode, transportProp, authProfile, session);
    MessageRoutes routing = new MessageRoutesImpl(routeNode);
    smtpRouter.route(messageNode, routing);

    for (MessageRoute route : routing) {
      assertEquals(username, route.getRcpt());
      assertEquals(MessageConstants.TYPE_INTERNAL, route.getTransport());
    }
  }

  @Test
  public void testMessageTypeSmtp() throws Exception {
    Property typeProp = createMock(Property.class);
    expect(typeProp.getString()).andReturn(MessageConstants.TYPE_SMTP);

    Node messageNode = createMock(Node.class);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TYPE)).andReturn(true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TYPE)).andReturn(typeProp);

    String username = "foo";
    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn(username);

    Node routeNode = createMock(Node.class);
    expect(routeNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);

    Property transportProp = createMock(Property.class);
    expect(transportProp.getString()).andReturn(MessageConstants.TYPE_INTERNAL);

    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    expect(propDef.isMultiple()).andReturn(false);

    Value emailValue = createMock(Value.class);
    expect(emailValue.getString()).andReturn(username + "@localhost");

    Property emailProp = createMock(Property.class);
    expect(emailProp.getDefinition()).andReturn(propDef);
    expect(emailProp.getValue()).andReturn(emailValue);

    Node authProfile = createMock(Node.class);
    expect(authProfile.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(true);
    expect(authProfile.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT))
        .andReturn(transportProp);
    expect(authProfile.hasProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(true)
        .times(2);
    expect(authProfile.getProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(emailProp);

    String authProfilePath = PathUtils.toInternalHashedPath("/_user/public", username,
        PersonalConstants.AUTH_PROFILE);
    expect(session.itemExists(authProfilePath)).andReturn(true);
    expect(session.getItem(authProfilePath)).andReturn(authProfile);

    replay(typeProp, messageNode, toProp, routeNode, transportProp, propDef, emailValue,
        emailProp, authProfile, session);
    MessageRoutes routing = new MessageRoutesImpl(routeNode);
    smtpRouter.route(messageNode, routing);

    for (MessageRoute route : routing) {
      assertEquals(username + "@localhost", route.getRcpt());
      assertEquals(MessageConstants.TYPE_SMTP, route.getTransport());
    }
  }
}
