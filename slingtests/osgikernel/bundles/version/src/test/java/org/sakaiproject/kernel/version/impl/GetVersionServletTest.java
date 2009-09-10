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
package org.sakaiproject.kernel.version.impl;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.version.impl.GetVersionServlet;

/**
 * 
 */
public class GetVersionServletTest {

  private GetVersionServlet getVersionServlet;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    getVersionServlet = new GetVersionServlet();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }
  
  @Test
  public void testGetVersionName() {
    assertEquals("1.1", getVersionServlet.getVersionName("version.,1.1,.tidy.json"));
    assertEquals("1", getVersionServlet.getVersionName("version.1.tidy.json"));
    assertEquals("1.1.tidy.json", getVersionServlet.getVersionName("version.,1.1.tidy.json"));
  }

}
