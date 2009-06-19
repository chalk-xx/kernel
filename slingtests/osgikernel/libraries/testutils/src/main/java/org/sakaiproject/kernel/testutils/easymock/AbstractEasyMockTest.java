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
package org.sakaiproject.kernel.testutils.easymock;

import static org.easymock.EasyMock.expect;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

public class AbstractEasyMockTest {
  private List<Object> mocks;

  @Before
  public void setUp() throws Exception {
    mocks = new ArrayList<Object>();
  }

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }

  protected void verify() {
    org.easymock.EasyMock.verify(mocks.toArray());
  }
  
  protected void addStringPropertyToNode(Node node, String propertyName,
      String propertyValue) throws ValueFormatException, RepositoryException {
    Property property = createMock(Property.class);
    expect(property.getString()).andReturn(propertyValue).anyTimes();
    PropertyDefinition definition = createMock(PropertyDefinition.class);
    expect(property.getDefinition()).andReturn(definition).anyTimes();
    expect(definition.isMultiple()).andReturn(false).anyTimes();

    expect(node.hasProperty(propertyName)).andReturn(true);
    expect(node.getProperty(propertyName)).andReturn(property);
  }
  
  protected void addPropertyToNode(Node node, String propertyName, Value[] values) throws ValueFormatException, RepositoryException {
    Property property = createMock(Property.class);
    expect(property.getValues()).andReturn(values).anyTimes();
    PropertyDefinition definition = createMock(PropertyDefinition.class);
    expect(property.getDefinition()).andReturn(definition).anyTimes();
    expect(definition.isMultiple()).andReturn(true).anyTimes();

    expect(node.hasProperty(propertyName)).andReturn(true);
    expect(node.getProperty(propertyName)).andReturn(property);
  }
  
  protected void addStringRequestParameter(SlingHttpServletRequest request,
      String key, String value) {
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString()).andReturn(value).anyTimes();
    expect(request.getRequestParameter(key)).andReturn(param);
  }

}
