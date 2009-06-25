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
package org.sakaiproject.kernel.search;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.query.Query;

/**
 * 
 */
public class SearchServletParsingTest {

  private SlingHttpServletRequest request;
  private SearchServlet searchServlet;
  private Object[] mocks;

  @Before
  public void setup() {
    searchServlet = new SearchServlet();
    request = createMock(SlingHttpServletRequest.class);
    RequestParameter rp = createMock(RequestParameter.class);
    expect(request.getRemoteUser()).andReturn("bob").anyTimes();
    expect(request.getRequestParameter("q")).andReturn(rp).anyTimes();
    expect(rp.getString()).andReturn("testing").anyTimes();
    RequestParameter rp_a = createMock(RequestParameter.class);

    expect(request.getRequestParameter("a")).andReturn(rp_a).anyTimes();
    expect(rp_a.getString()).andReturn("again").anyTimes();

    mocks = new Object[] { request, rp, rp_a };
    replay(mocks);

  }

  @After
  public void tearDown() {
    verify(mocks);
  }

  @Test
  public void testQueryParsing() {
    String result = searchServlet.processQueryTemplate(request, " {q}",
        Query.SQL);
    assertEquals(" testing", result);
  }

  @Test
  public void testQueryParsing1() {
    String result = searchServlet.processQueryTemplate(request, "{q} ",
        Query.SQL);
    assertEquals("testing ", result);
  }

  @Test
  public void testQueryParsing2() {
    String result = searchServlet.processQueryTemplate(request, "{q} {a}",
        Query.SQL);
    assertEquals("testing again", result);
  }
}
