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
package org.sakaiproject.nakamura.batch;

import static org.mockito.Mockito.verify;

import static org.junit.Assert.fail;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.batch.CreateTreeOperation.TREE_PARAM;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class CreateTreeOperationTest {

  private CreateTreeOperation operation;
  private SlingHttpServletRequest request;
  private Session session;

  @Before
  public void setUp() {
    operation = new CreateTreeOperation();
    request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    session = mock(Session.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);
  }

  @Test
  public void testMissingParameter() {
    try {
      when(request.getRequestParameter(TREE_PARAM)).thenReturn(null);
      operation.doRun(request, null, null);
      fail("With no tree parameter, this should have thrown an exception.");
    } catch (RepositoryException e) {
      assertEquals("No " + TREE_PARAM + " parameter found.", e.getMessage());
    }
  }

  @Test
  public void testInvalidJSON() {
    try {
      RequestParameter jsonParam = mock(RequestParameter.class);
      when(jsonParam.getString()).thenReturn("foo");
      when(request.getRequestParameter(TREE_PARAM)).thenReturn(jsonParam);
      operation.doRun(request, null, null);
      fail("With no tree parameter, this should have thrown an exception.");
    } catch (RepositoryException e) {
      assertEquals("Invalid JSON tree structure", e.getMessage());
    }
  }

  @Test
  public void testAddNewNode() throws RepositoryException {
    Node node = mock(Node.class);
    when(node.hasNode("newChildNode")).thenReturn(false);
    operation.addNode(node, "newChildNode");
    verify(node).addNode("newChildNode");
  }

  @Test
  public void testAddExistingNode() throws RepositoryException {
    Node node = mock(Node.class);
    when(node.hasNode("existingNode")).thenReturn(true);
    operation.addNode(node, "existingNode");
    verify(node).getNode("existingNode");
  }

  @Test
  public void testCreateTree() throws RepositoryException, JSONException {
    RequestParameter jsonParam = mock(RequestParameter.class);
    JSONObject top = new JSONObject();
    top.put("foo", "bar");
    top.put("intVal", 25);
    top.put("boolVal", true);
    JSONArray arr = new JSONArray();
    arr.put("alfa");
    arr.put("beta");
    top.put("arrVal", arr);
    // Child node
    JSONObject child = new JSONObject();
    child.put("isChild", true);
    top.put("child", child);

    String json = top.toString(2);
    System.err.println(top);
    when(jsonParam.getString()).thenReturn(json);
    when(request.getRequestParameter(TREE_PARAM)).thenReturn(jsonParam);

    Resource resource = mock(Resource.class);
    when(resource.getPath()).thenReturn("/path/to/tree");
    when(request.getResource()).thenReturn(resource);

    // Handle the node mocks.
    Node topNode = mock(Node.class);
    Node childNode = mock(Node.class);
    when(topNode.addNode("child")).thenReturn(childNode);
    when(session.getItem(resource.getPath())).thenReturn(topNode);
    when(session.itemExists(resource.getPath())).thenReturn(true);
    when(session.hasPendingChanges()).thenReturn(true);

    operation.doRun(request, null, null);

    verify(topNode).setProperty("foo", "bar");
    verify(topNode).setProperty("intVal", 25.0);
    verify(topNode).setProperty("boolVal", true);
    verify(topNode).setProperty("arrVal", new String[] { "alfa", "beta" });
    verify(childNode).setProperty("isChild", true);
    verify(session).save();

  }

}
