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
package org.sakaiproject.kernel.site.servlet;

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

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
  
}
