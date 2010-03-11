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
package org.sakaiproject.nakamura.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class ExtendedJSONWriterTest {

  private List<Object> mocks;

  @Before
  public void setUp() {
    mocks = new ArrayList<Object>();
  }

  @Test
  public void testValueMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("l", 1.5);
    map.put("foo", "bar");

    ValueMapDecorator valueMap = new ValueMapDecorator(map);

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);
    try {
      ext.valueMap(valueMap);

      String s = writer.toString();
      assertEquals("Returned JSON was not as excepted",
          "{\"foo\":\"bar\",\"l\":1.5}", s);

    } catch (JSONException e) {
      fail("A good ValueMap should not throw a JSONException.");
    }
  }

  @Test
  public void testNode() {
    try {
      Node node = createMock(Node.class);
      expect(node.getPath()).andReturn("/path/to/node");
      expect(node.getName()).andReturn("node");
      PropertyIterator propertyIterator = createMock(PropertyIterator.class);
      PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
      PropertyDefinition propDefMultiple = createMock(PropertyDefinition.class);
      expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();
      expect(propDefMultiple.isMultiple()).andReturn(true).anyTimes();

      // Properties
      // Double
      Property doubleProp = createMock(Property.class);
      Value doubleValue = createMock(Value.class);

      expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE);
      expect(doubleValue.getDouble()).andReturn(Double.parseDouble("1.5"));
      expect(doubleProp.getName()).andReturn("doub").once();
      expect(doubleProp.getDefinition()).andReturn(propDefSingle);
      expect(doubleProp.getValue()).andReturn(doubleValue).once();

      // Multi string prop
      Property multiStringProp = createMock(Property.class);
      expect(multiStringProp.getDefinition()).andReturn(propDefMultiple).once();
      expect(multiStringProp.getName()).andReturn("multiString").once();
      Value[] multiStringValues = new Value[2];
      multiStringValues[0] = createMock(Value.class);
      multiStringValues[1] = createMock(Value.class);
      expect(multiStringValues[0].getType()).andReturn(PropertyType.STRING);
      expect(multiStringValues[1].getType()).andReturn(PropertyType.STRING);
      expect(multiStringValues[0].getString()).andReturn("foo");
      expect(multiStringValues[1].getString()).andReturn("bar");
      expect(multiStringProp.getValues()).andReturn(multiStringValues);

      // Iterator
      expect(propertyIterator.hasNext()).andReturn(true);
      expect(propertyIterator.nextProperty()).andReturn(doubleProp);
      expect(propertyIterator.hasNext()).andReturn(true);
      expect(propertyIterator.nextProperty()).andReturn(multiStringProp);
      expect(propertyIterator.hasNext()).andReturn(false);

      expect(node.getProperties()).andReturn(propertyIterator).anyTimes();

      replay();

      StringWriter writer = new StringWriter();
      ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);
      try {
        ext.node(node);
        writer.flush();
        String s = writer.toString();
        JSONObject o = new JSONObject(s);
        assertEquals(1.5, o.getDouble("doub"), 0);
        assertEquals(2, o.getJSONArray("multiString").length());
        assertEquals("/path/to/node", o.get("jcr:path"));
        assertEquals("node", o.get("jcr:name"));

      } catch (JSONException e) {
        fail("Should not throw a JSONException.");
      }
    } catch (RepositoryException e) {
      fail("Should not throw a RepositoryException.");
    }
  }

  /*
   * Helper methods for mocking.
   */

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }

}
