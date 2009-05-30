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
package org.sakaiproject.kernel.messaging;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.easymock.Capture;
import org.junit.Test;
import org.sakaiproject.kernel.messaging.resource.MessageResourceProvider;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * 
 */
public class MessageResourceProviderTest {

  @Test
  public void testPathResolution() throws RepositoryException {
    ResourceResolver rr = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    Resource resource = createMock(Resource.class);
    Resource finalResource = createMock(Resource.class);
    Node nodeC = createMock(Node.class);
    Node nodeB = createMock(Node.class);
    Node nodeMessages = createMock(Node.class);
    Node rootNode = createMock(Node.class);
    Property somethingElse = createMock(Property.class);
    Property sakaiMessages = createMock(Property.class);
    ResourceMetadata resourceMetadata = new ResourceMetadata();

    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(session.getUserID()).andReturn("ieb").anyTimes();
    expect(session.getRootNode()).andReturn(rootNode).anyTimes();
    expect(rr.resolve("/test/messages/b/c/1001")).andReturn(resource);
    expect(resource.adaptTo(Node.class)).andReturn(nodeC);
    expect(nodeC.getSession()).andReturn(session);
    expect(nodeC.hasProperty("sling:resourceType")).andReturn(false).anyTimes();
    expect(nodeC.getParent()).andReturn(nodeB).anyTimes();
    expect(nodeB.hasProperty("sling:resourceType")).andReturn(true).anyTimes();
    expect(nodeB.getProperty("sling:resourceType")).andReturn(somethingElse).anyTimes();
    expect(somethingElse.getString()).andReturn("sakai/somethingElse");
    expect(nodeB.getParent()).andReturn(nodeMessages).anyTimes();
    expect(nodeMessages.hasProperty("sling:resourceType")).andReturn(true).anyTimes();
    expect(nodeMessages.getProperty("sling:resourceType")).andReturn(sakaiMessages).anyTimes();
    expect(sakaiMessages.getString()).andReturn("sakai/messages");
    expect(nodeMessages.getPath()).andReturn("/test/messages");
    Capture<String> finalPath = new Capture<String>();
    expect(rr.resolve(capture(finalPath))).andReturn(finalResource);
    expect(finalResource.getResourceMetadata()).andReturn(resourceMetadata).atLeastOnce();
    
    replay(rr, session, resource, nodeC, rootNode, nodeB, nodeMessages, somethingElse, sakaiMessages, finalResource);
    
    MessageResourceProvider mrp = new MessageResourceProvider();
    Resource resolvedResource = mrp.getResource(rr, "/test/messages/b/c/1001");

 //   assertEquals("/test/messages/AA/BB/CC/DD/b/c/1001", finalPath.getValue());
    verify(rr, session, resource, nodeC, rootNode, nodeB, nodeMessages, somethingElse, sakaiMessages, finalResource);
  }
}
