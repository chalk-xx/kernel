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
package org.sakaiproject.kernel.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * 
 */
public class JcrUtilsTest {

  @Test
  public void testDeepCreate() throws RepositoryException {
    Session session = createMock(Session.class);
    Node nodeB = createMock(Node.class);
    Node nodeC = createMock(Node.class);
    Node nodeD = createMock(Node.class);
    expect(session.itemExists("/x/a/b/c/d")).andReturn(false);
    expect(session.itemExists("/x/a/b/c")).andReturn(false);
    expect(session.itemExists("/x/a/b")).andReturn(true);
    expect(session.getItem("/x/a/b")).andReturn(nodeB);
    expect(nodeB.hasNode("c")).andReturn(false);
    expect(nodeB.addNode("c")).andReturn(nodeC);
    expect(nodeC.hasNode("d")).andReturn(false);
    expect(nodeC.addNode("d")).andReturn(nodeD);

    replay(session, nodeB, nodeC, nodeD);
    JcrUtils.deepGetOrCreateNode(session, "/x/a/b/c/d");
    verify(session, nodeB, nodeC, nodeD);
  }

}
