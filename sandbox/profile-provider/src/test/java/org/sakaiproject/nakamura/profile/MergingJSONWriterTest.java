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
package org.sakaiproject.nakamura.profile;

import junit.framework.Assert;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class MergingJSONWriterTest {

  @Mock
  private Node baseNode;
  @Mock
  private Future<Map<String, Object>> futureMap1;
  @Mock
  private Node externalNode1;
  @Mock
  private NodeIterator nodeIterator;
  @Mock
  private Node node1;
  @Mock
  private NodeIterator emptyNodeIterator;

  /**
   * 
   */
  public MergingJSONWriterTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testMerge() throws JSONException, RepositoryException,
      InterruptedException, ExecutionException {
    MergingJSONWriter mw = new MergingJSONWriter(new HashSet<String>());

    Map<Node, Future<Map<String, Object>>> providedNodeData = new HashMap<Node, Future<Map<String, Object>>>();
    providedNodeData.put(externalNode1, futureMap1);

    dumpNodeProperties(baseNode, "testproperty", "prop1value");

    Mockito.when(baseNode.getNodes()).thenReturn(nodeIterator);
    Mockito.when(nodeIterator.hasNext()).thenReturn(true, true, false);
    Mockito.when(nodeIterator.nextNode()).thenReturn(node1, externalNode1);

    Mockito.when(externalNode1.getName()).thenReturn("external1");
    Mockito.when(node1.getName()).thenReturn("node1");

    dumpNodeProperties(node1, "node1property", "innerNodeProperty");

    Mockito.when(node1.getNodes()).thenReturn(emptyNodeIterator);
    Mockito.when(emptyNodeIterator.hasNext()).thenReturn(false);

    Map<String, Object> externalNodeMap = new HashMap<String, Object>();
    externalNodeMap.put("externalproperties", "externalvalue");
    Map<String, Object> externalMap = new HashMap<String, Object>();
    externalMap.put("subtreeprop", "subtreevalue");
    externalNodeMap.put("externalObject", externalMap);
    Mockito.when(futureMap1.get()).thenReturn(externalNodeMap);

    StringWriter w = new StringWriter();

    mw.dump(baseNode, providedNodeData, w);

    Mockito.verify(futureMap1, Mockito.times(1)).get();

    JSONObject jo = new JSONObject(w.toString());
    Assert.assertEquals(jo.toString(4), 3, jo.length());
    Assert.assertEquals(jo.toString(4), "prop1value", jo.getString("testproperty"));
    JSONObject node1jo = jo.getJSONObject("node1");
    Assert.assertEquals(node1jo.toString(4), 1, node1jo.length());
    Assert.assertEquals(node1jo.toString(4), "innerNodeProperty", node1jo
        .getString("node1property"));
    JSONObject external1jo = jo.getJSONObject("external1");
    Assert.assertEquals(external1jo.toString(4), 2, external1jo.length());
    Assert.assertEquals(external1jo.toString(4), "externalvalue", external1jo
        .getString("externalproperties"));
    JSONObject externalObjectjo = external1jo.getJSONObject("externalObject");
    Assert.assertEquals(externalObjectjo.toString(4), 1, externalObjectjo.length());
    Assert.assertEquals(externalObjectjo.toString(4), "subtreevalue", externalObjectjo
        .getString("subtreeprop"));

    /*
     * 
     * Assert.assertEquals("{\"testproperty\":\"prop1value\"," + "\"node1\":{" +
     * "\"node1property\":\"innerNodeProperty\"}," + "\"external1\":{" +
     * "\"externalproperties\":\"externalvalue\"," +
     * "\"externalObject\":{\"subtreeprop\":\"subtreevalue\"}}}", w.toString());
     */
  }

  /**
   * @param baseNode2
   * @param string
   * @param string2
   * @throws RepositoryException
   */
  public static void dumpNodeProperties(Node baseNode, String propertyName,
      String propertyValue) throws RepositoryException {
    PropertyIterator propertyIterator = Mockito.mock(PropertyIterator.class);
    Property property = Mockito.mock(Property.class);
    PropertyDefinition propertyDefnition = Mockito.mock(PropertyDefinition.class);
    Value propertyV = Mockito.mock(Value.class);

    Mockito.when(baseNode.getProperties()).thenReturn(propertyIterator);
    Mockito.when(propertyIterator.hasNext()).thenReturn(true, false);
    Mockito.when(propertyIterator.nextProperty()).thenReturn(property);
    Mockito.when(property.getType()).thenReturn(PropertyType.STRING);
    Mockito.when(property.getName()).thenReturn(propertyName);
    Mockito.when(property.getDefinition()).thenReturn(propertyDefnition);
    Mockito.when(propertyDefnition.isMultiple()).thenReturn(false);
    Mockito.when(property.getValue()).thenReturn(propertyV);
    Mockito.when(propertyV.getType())
        .thenReturn(PropertyType.STRING, PropertyType.STRING);
    Mockito.when(propertyV.getString()).thenReturn(propertyValue);

  }
}
