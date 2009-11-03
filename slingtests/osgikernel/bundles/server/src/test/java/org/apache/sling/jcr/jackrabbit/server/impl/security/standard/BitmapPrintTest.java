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

import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class BitmapPrintTest {

  
  private List<Object> objects = new ArrayList<Object>();
  private JackrabbitAccessControlEntry ace;

  
  @Before
  public void before() {
    try {
      objects = new ArrayList<Object>();
      ace = createMock(JackrabbitAccessControlEntry.class);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // this reqires inspection to really test
  @Test
  public void testPrintBitmap() {
    
    int allows = 0x00;
    int denies = 0x00;
    int allowPrivileges = 0x00;
    int denyPrivileges = 0x00;
    int parentAllows = 0x00;
    int parentDenies = 0x00;
    replay();
    ACLProvider.logState("test", ace, allows, denies, allowPrivileges, denyPrivileges, parentAllows, parentDenies);
    ACLProvider.logState("test", ace, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20);
    ACLProvider.logState("test", ace, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800);
    ACLProvider.logState("test", ace, 0x1000, 0x2000, 0x4000, 0x8000, 0x10000, 0x20000);
    ACLProvider.logState("test", ace, 0x40000, 0x2000, 0x80000, 0x100000, 0x200000, 0x400000);
    ACLProvider.logState("test", ace, 0x800000, 0x1000000, 0x2000000, 0x4000000, 0x8000000, 0x10000000);
    ACLProvider.logState("test", ace, 0x20000000, 0x40000000, 0x80000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
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
