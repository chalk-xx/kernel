/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.persondirectory.providers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.personal.PersonalConstants;
import org.sakaiproject.kernel.api.persondirectory.Person;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class JcrPersonProviderTest {
  private JcrPersonProvider provider;

  @Before
  public void setUp() {
    provider = new JcrPersonProvider();
  }

  @Test
  public void testGetPerson() throws Exception {
    Value value = createMock(Value.class);
    expect(value.getString()).andReturn("Carl");

    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    expect(propDef.isMultiple()).andReturn(false);

    Property prop = createMock(Property.class);
    expect(prop.getName()).andReturn("Firstname");
    expect(prop.getDefinition()).andReturn(propDef);
    expect(prop.getValue()).andReturn(value);

    PropertyIterator propIt = createMock(PropertyIterator.class);
    expect(propIt.hasNext()).andReturn(true);
    expect(propIt.nextProperty()).andReturn(prop);
    expect(propIt.hasNext()).andReturn(false);

    Node authprofile = createMock(Node.class);
    expect(authprofile.getProperties()).andReturn(propIt);
    expect(authprofile.hasProperty("Firstname")).andReturn(true);
    expect(authprofile.getProperty("Firstname")).andReturn(prop);

    Node node = createMock(Node.class);
    expect(node.getNode(PersonalConstants.AUTH_PROFILE)).andReturn(authprofile);

    replay(value, propDef, prop, propIt, authprofile, node);

    Person person = provider.getPerson("chall39", node);
    assertEquals("chall39", person.getName());
    assertEquals("Carl", person.getAttributeValue("Firstname"));
  }

  @Test
  public void testRepoException() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getNode(PersonalConstants.AUTH_PROFILE)).andThrow(
        new RepositoryException());

    replay(node);

    Person person = provider.getPerson("chall39", node);
    assertEquals("chall39", person.getName());
  }
}
