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
package org.sakaiproject.nakamura.discussion.searchresults;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Row;
import javax.jcr.version.VersionException;

/**
 *
 */
public class InitialPostResultProcessorTest extends AbstractEasyMockTest {
  private DiscussionInitialPostSearchResultProcessor processor;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    processor = new DiscussionInitialPostSearchResultProcessor();
  }

  @Test
  public void testProces() throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException, JSONException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    JSONWriter writer = new JSONWriter(w);

    Row row = createMock(Row.class);
    Value pathValue = new MockValue("/path/to/msg");
    expect(row.getValue("jcr:path")).andReturn(pathValue);

    Node node = new MockNode("/path/to/msg");
    node.setProperty("foo", "bar");
    node.setProperty("b", false);

    expect(session.getItem("/path/to/msg")).andReturn(node);

    replay();

    processor.writeNode(request, writer, null, row);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    assertEquals("bar", o.get("foo"));

  }
}
