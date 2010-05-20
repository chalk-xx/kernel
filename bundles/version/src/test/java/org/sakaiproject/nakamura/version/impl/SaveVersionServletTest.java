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
package org.sakaiproject.nakamura.version.impl;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.servlet.ServletException;


/**
 *
 */
public class SaveVersionServletTest extends AbstractEasyMockTest {

  @Test
  public void testSave404() throws ServletException, IOException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createNiceMock(SlingHttpServletResponse.class);
    Resource resource = createNiceMock(Resource.class);

    EasyMock.expect(request.getResource()).andReturn(resource);
    response.sendError(404);
    EasyMock.expectLastCall();


    replay();

    SaveVersionServlet saveVersionServlet = new SaveVersionServlet();

    saveVersionServlet.doPost(request, response);

    verify();
  }

  @Test
  public void testSaveVersion() throws ServletException, IOException, VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createNiceMock(SlingHttpServletResponse.class);
    Resource resource = createNiceMock(Resource.class);
    Node node = createNiceMock(Node.class);
    Version version = createNiceMock(Version.class);
    Session session = createNiceMock(Session.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);

    EasyMock.expect(request.getResource()).andReturn(resource);
    EasyMock.expect(resource.adaptTo(Node.class)).andReturn(node);
    EasyMock.expect(node.checkin()).andReturn(version);
    node.checkout();
    EasyMock.expectLastCall();
    EasyMock.expect(node.getSession()).andReturn(session);
    EasyMock.expect(session.hasPendingChanges()).andReturn(true);
    EasyMock.expect(node.getSession()).andReturn(session);
    session.save();
    EasyMock.expectLastCall();

    EasyMock.expect(response.getWriter()).andReturn(new PrintWriter(new ByteArrayOutputStream()));
    EasyMock.expect(version.getProperties()).andReturn(propertyIterator);
    EasyMock.expect(propertyIterator.hasNext()).andReturn(false);

    replay();

    SaveVersionServlet saveVersionServlet = new SaveVersionServlet();
    VersionServiceImpl versionServiceImpl = new VersionServiceImpl();
    saveVersionServlet.versionService = versionServiceImpl;

    saveVersionServlet.doPost(request, response);

    verify();
  }


}
