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
package org.sakaiproject.nakamura.resource;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
/**
 *
 */
public class VirtualResourceProviderImplTest {

  private List<Object> all = new ArrayList<Object>();
  private VirtualResourceProviderImpl v;
  private Session session;

  @Before
  public void before() {
    clear();
     v = new VirtualResourceProviderImpl();
     session = createMock(Session.class);
  }
 
  @After
  public void after() {
    verify();

  }

  /**
   *
   */
  private void clear() {
   all.clear();
  }
  public <T> T createMock(Class<T> c) {
    T t = EasyMock.createMock(c);
    all.add(t);
    return t;
  }
  public void replay() {
    EasyMock.replay(all.toArray());
  }
  public void verify() {
    EasyMock.verify(all.toArray());
  }

  public boolean checkIgnore(String path, boolean check, boolean exists) throws RepositoryException {
    if ( check ) {
      expect(session.itemExists(path)).andReturn(exists).atLeastOnce();
    }
    replay();
    return v.ignoreThisPath(session,path);
  }

  @Test
  public void testIgnoreThisPath() throws RepositoryException {
    assertTrue(checkIgnore("/trertre/erter.sdf", false, false));
  }

  @Test
  public void testIgnoreThisPath1() throws RepositoryException {
    assertTrue(checkIgnore("/trertre/erter", true, true));
  }
 
  @Test
  public void testIgnoreThisPath2() throws RepositoryException {
    assertFalse(checkIgnore("/trertre/erter", true, false));
  }
 
  @Test
  public void testIgnoreThisPath3() throws RepositoryException {
    assertFalse(checkIgnore("/tre.rtre/erter", true, false));
  }

  @Test
  public void testIgnoreThisPath5() throws RepositoryException {
    assertTrue(checkIgnore("/tre.rtre", false, true));
  }

}
