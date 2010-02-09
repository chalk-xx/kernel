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
package org.sakaiproject.nakamura.testutils.easymock;

import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class SakaiMockNode extends MockNode {

  /**
   * @param path
   */
  public SakaiMockNode(String path) {
    super(path);
  }

  private Session session;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.testing.jcr.MockNode#getSession()
   */
  @Override
  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.testing.jcr.MockNode#getProperties(java.lang.String)
   */
  @Override
  public PropertyIterator getProperties(String namePattern) {
    PropertyIterator iterator = getProperties();
    List<Property> properties = new ArrayList<Property>();

    try {
      while (iterator.hasNext()) {
        Property p = iterator.nextProperty();
        String name = p.getName();
        if (ChildrenCollectorFilter.matches(name, namePattern)) {
          properties.add(p);
        }
      }
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return new MockPropertyIterator(properties.iterator());

  }

}
