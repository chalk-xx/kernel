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
package org.apache.sling.jcr.jackrabbit.server.impl.security.standard;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.sling.jcr.jackrabbit.server.impl.security.standard.ACLTemplate.ComparableEntry;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 *
 */
public class ComparableEntryTest {

  private ArrayList<Object> objects;
  private Principal principalAdmin;
  private NameResolver nameResolver;
  private Principal principalAnon;

  @Before
  public void before() {
    try {
      objects = new ArrayList<Object>();
      principalAdmin = createMock(Principal.class);
      principalAnon = createMock(Principal.class);
      nameResolver = createMock(NameResolver.class);
      NameFactory nf = NameFactoryImpl.getInstance();
      expect(nameResolver.getQName(Privilege.JCR_READ)).andReturn(
          nf.create(Privilege.JCR_READ));
      expect(principalAdmin.getName()).andReturn("admin").anyTimes();
      expect(principalAnon.getName()).andReturn("anonymous").anyTimes();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testComparableEntry() throws RepositoryException {

    replay();

    PrivilegeRegistry pr = new PrivilegeRegistry(nameResolver);
    Privilege[] privileges = new Privilege[] { pr.getPrivilege(Privilege.JCR_READ) };
    ComparableEntry entry1 = new ComparableEntry("/pathlonger", false, principalAnon,
        privileges, true);
    ComparableEntry entry2 = new ComparableEntry("/path", false, principalAdmin, privileges,
        true);
    compare(entry1,entry2);    

  }

  @Test
  public void testComparableEntryGroup() throws RepositoryException {

    replay();

    PrivilegeRegistry pr = new PrivilegeRegistry(nameResolver);
    Privilege[] privileges = new Privilege[] { pr.getPrivilege(Privilege.JCR_READ) };
    ComparableEntry entry1 = new ComparableEntry("/pathlonger", false, principalAnon,
        privileges, true);
    ComparableEntry entry2 = new ComparableEntry("/path", true, principalAdmin, privileges,
        true);
    compare(entry1,entry2);   
  }

  @Test
  public void testComparableEntryGroupFirst() throws RepositoryException {

    replay();

    PrivilegeRegistry pr = new PrivilegeRegistry(nameResolver);
    Privilege[] privileges = new Privilege[] { pr.getPrivilege(Privilege.JCR_READ) };
    ComparableEntry entry1 = new ComparableEntry("/path", false, principalAdmin, privileges,
        true);
    ComparableEntry entry2 = new ComparableEntry("/pathlonger", true, principalAnon,
        privileges, true);
    compare(entry1,entry2);   
  }

  

  @Test
  public void testComparableEntrySamePathGroup() throws RepositoryException {

    replay();

    PrivilegeRegistry pr = new PrivilegeRegistry(nameResolver);
    Privilege[] privileges = new Privilege[] { pr.getPrivilege(Privilege.JCR_READ) };
    ComparableEntry entry1 = new ComparableEntry("/path", false, principalAnon,
        privileges, true);
    ComparableEntry entry2 = new ComparableEntry("/path", true, principalAdmin, privileges,
        true);
    compare(entry1,entry2);

  }

  
  private void compare(ComparableEntry entry1, ComparableEntry entry2) {
    assertFalse(entry1.equals(entry2));
    
    List<ComparableAccessControlEntry> list = new ArrayList<ComparableAccessControlEntry>();
    list.add(entry1);
    list.add(entry2);

    Collections.sort(list);
    assertTrue(entry1.equals(list.get(0)));
    assertTrue(entry2.equals(list.get(1)));
    assertFalse(entry2.equals(list.get(0)));
    assertFalse(entry1.equals(list.get(1)));

    list.clear();
    list.add(entry2);
    list.add(entry1);
    Collections.sort(list);
    assertTrue(entry1.equals(list.get(0)));
    assertTrue(entry2.equals(list.get(1)));
    assertFalse(entry2.equals(list.get(0)));
    assertFalse(entry1.equals(list.get(1)));
        
    verify();

  }

  private void replay() {
    EasyMock.replay(objects.toArray());
  }

  private void verify() {
    EasyMock.verify(objects.toArray());
  }

  private <T> T createMock(Class<T> clazz) {
    T m = EasyMock.createMock(clazz);
    objects.add(m);
    return m;
  }
}
