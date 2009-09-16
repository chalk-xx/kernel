package org.sakaiproject.kernel.message.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sakaiproject.kernel.api.message.MessageConstants;

import javax.jcr.Node;
import javax.jcr.Property;

public class MessageRoutesImplTest {

  @Test
  public void testConstructWithNode() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn("smtp:foo@localhost,smtp:bar@localhost");

    Node node = createMock(Node.class);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(prop);

    replay(node, prop);
    MessageRoutesImpl mri = new MessageRoutesImpl(node);
    assertEquals(2, mri.size());
  }
}
