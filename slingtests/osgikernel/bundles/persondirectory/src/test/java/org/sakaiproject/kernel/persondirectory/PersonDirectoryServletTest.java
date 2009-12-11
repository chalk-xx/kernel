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
package org.sakaiproject.kernel.persondirectory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

/**
 * Test the federation of person providers. Tests that the federating happens
 * correctly. Not so much about the actual retrieval of info but that each bound
 * provider is used.
 *
 * @author Carl Hall
 */
public class PersonDirectoryServletTest {
  private Node profileNode;

  private PersonDirectoryServlet servlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private Resource resource;
  private StringWriter writer;

  private PersonProvider provider0;
  private PersonProvider provider1;
  private PersonProvider provider2;
  private PersonProvider provider3;
  private PersonProvider provider4;

  private PersonImpl person0;
  private PersonImpl person1;
  private PersonImpl person2;
  private PersonImpl person3;
  private PersonImpl person4;

  private static HashMap<String, String[]> attrs0 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs1 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs2 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs3 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs4 = new HashMap<String, String[]>();

  @BeforeClass
  public static void beforeClass() {
    attrs0.put("attr0", new String[] { "val0" });

    attrs1.putAll(attrs0);
    attrs1.put("attr1", new String[] { "val1" });

    attrs2.putAll(attrs1);
    attrs2.put("attr2", new String[] { "val2" });

    attrs3.putAll(attrs2);
    attrs3.put("attr3", new String[] { "val3" });

    attrs4.putAll(attrs3);
    attrs4.put("attr4", new String[] { "val4" });
  }

  @Before
  public void setUp() throws Exception {
    profileNode = createMock(Node.class);

    servlet = new PersonDirectoryServlet();

    resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(profileNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    writer = new StringWriter();
    expect(response.getWriter()).andReturn(new PrintWriter(writer));

    // create some providers
    provider0 = createMock(PersonProvider.class);
    provider1 = createMock(PersonProvider.class);
    provider2 = createMock(PersonProvider.class);
    provider3 = createMock(PersonProvider.class);
    provider4 = createMock(PersonProvider.class);

    servlet.bindProvider(provider0);
    servlet.bindProvider(provider1);
    servlet.bindProvider(provider2);
    servlet.bindProvider(provider3);
    servlet.bindProvider(provider4);

    // create some people to return
    person0 = new PersonImpl("user0");
    person1 = new PersonImpl("user1");
    person2 = new PersonImpl("user2");
    person3 = new PersonImpl("user3");
    person4 = new PersonImpl("user4");

    person0.addAttributes(attrs0);
    person1.addAttributes(attrs1);
    person2.addAttributes(attrs2);
    person3.addAttributes(attrs3);
    person4.addAttributes(attrs4);
  }

  @After
  public void tearDown() {
    servlet.unbindProvider(provider0);
    servlet.unbindProvider(provider1);
    servlet.unbindProvider(provider2);
    servlet.unbindProvider(provider3);
    servlet.unbindProvider(provider4);
  }

  /**
   * Get a person when there are no providers bound.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonWithNoProviders() throws Exception {
    PersonDirectoryServlet servlet = new PersonDirectoryServlet();
    Person person = servlet.getPerson("doesn't matter", profileNode);
    assertNull(person);
  }

  /**
   * Try to get a person that is not returned by any of the providers.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsNoneFound() throws Exception {
    expect(provider0.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider1.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider2.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider3.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider4.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = servlet.getPerson("user0", profileNode);
    // servlet.doGet(request, response);
    assertNull(person);
  }

  /**
   * Get a person and all associated attributes. Tests with all providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsAllReturn() throws Exception {
    expect(provider0.getPerson(isA(String.class), isA(Node.class))).andReturn(person0);
    expect(provider1.getPerson(isA(String.class), isA(Node.class))).andReturn(person1);
    expect(provider2.getPerson(isA(String.class), isA(Node.class))).andReturn(person2);
    expect(provider3.getPerson(isA(String.class), isA(Node.class))).andReturn(person3);
    expect(provider4.getPerson(isA(String.class), isA(Node.class))).andReturn(person4);

    expect(profileNode.getName()).andReturn("user0");
    replay(provider0, provider1, provider2, provider3, provider4, profileNode);

    Person person = servlet.getPerson("user0", profileNode);
    // servlet.doGet(request, response);
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person4, we can get up to 5 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(5, attrs.size());
    assertEquals(5, attrNames.size());

    for (int i = 0; i < 5; i++) {
      String[] vals = person.getAttributeValues("attr" + i);
      assertEquals(5 - i, vals.length);
      for (String val : vals) {
        assertEquals("val" + i, val);
      }
    }
  }

  /**
   * Get a person and all associated attributes. Tests with only some providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsSomeReturn() throws Exception {
    expect(provider0.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider1.getPerson(isA(String.class), isA(Node.class))).andReturn(person1);
    expect(provider2.getPerson(isA(String.class), isA(Node.class))).andReturn(null);
    expect(provider3.getPerson(isA(String.class), isA(Node.class))).andReturn(person3);
    expect(provider4.getPerson(isA(String.class), isA(Node.class))).andReturn(null);

    expect(profileNode.getName()).andReturn("user1");
    replay(provider0, provider1, provider2, provider3, provider4, profileNode);

    Person person = servlet.getPerson("user1", profileNode);
    // servlet.doGet(request, response);
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person3, we can get up to 4 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(4, attrs.size());
    assertEquals(4, attrNames.size());

    String[] vals = person.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val0", val);
    }

    vals = person.getAttributeValues("attr1");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val1", val);
    }

    vals = person.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("val2", vals[0]);

    vals = person.getAttributeValues("attr3");
    assertEquals(1, vals.length);
    assertEquals("val3", vals[0]);

    vals = person.getAttributeValues("attr4");
    assertNull(vals);
  }

  @Test
  public void testDoGet() throws Exception {
    expect(profileNode.getName()).andReturn("user0");
    expect(provider0.getPerson(isA(String.class), isA(Node.class))).andReturn(person0);
    expect(provider1.getPerson(isA(String.class), isA(Node.class))).andReturn(person2);
    response.setStatus(HttpServletResponse.SC_OK);
    expectLastCall();
    replay(request, response, provider0, provider1, resource, profileNode);
    servlet.doGet(request, response);

    JSONObject respObj = new JSONObject(writer.toString());
    JSONArray attr0 = respObj.getJSONArray("attr0");
    assertEquals(2, attr0.length());
    assertEquals("val0", attr0.get(0));
    assertEquals("val0", attr0.get(1));

    String attr1 = respObj.getString("attr1");
    assertEquals("val1", attr1);

    String attr2 = respObj.getString("attr2");
    assertEquals("val2", attr2);
  }

  @Test
  public void testDoGetNoResults() throws Exception {
    expect(profileNode.getName()).andReturn("user0");
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    expectLastCall();
    response.setIntHeader("Content-Length", 0);
    expectLastCall();
    replay(request, response, resource, profileNode);
    servlet.doGet(request, response);
    assertEquals(0, writer.toString().length());
  }

  @Test
  public void testDoGetThrowsRepositoryException() throws Exception {
    expect(profileNode.getName()).andThrow(new RepositoryException());
    response.sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), (String) anyObject());
    expectLastCall();
    replay(request, response, resource, profileNode);
    servlet.doGet(request, response);
  }

  @Test
  public void testDoGetThrowsPersonProviderException() throws Exception {
    expect(profileNode.getName()).andReturn("user0");
    expect(provider0.getPerson(isA(String.class), isA(Node.class))).andThrow(
        new PersonProviderException());
    response.sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), (String) anyObject());
    replay(request, response, resource, profileNode, provider0);
    servlet.doGet(request, response);
  }
}
