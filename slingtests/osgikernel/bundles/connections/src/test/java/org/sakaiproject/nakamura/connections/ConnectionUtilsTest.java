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
package org.sakaiproject.nakamura.connections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 
 */
public class ConnectionUtilsTest {

  /**
   *
   */
  private static final String BASE = "/_user/contacts/b3/da/a7/7b/user1/a1/88/1c/06/user2";

  @Test
  public void testBasePath() {
    assertEquals(BASE, ConnectionUtils.getConnectionPath("user1", "user2", null));
    assertEquals(BASE, ConnectionUtils.getConnectionPath("user1", "user2"));
  }

  @Test
  public void testPathEmpty() {
    assertEquals(BASE, ConnectionUtils.getConnectionPath("user1", "user2", ""));
  }

  @Test
  public void testPathSelector() {
    assertEquals(BASE+".dsfljksfkjs.sdfsdf", ConnectionUtils.getConnectionPath("user1",
        "user2", ".dsfljksfkjs.sdfsdf"));
  }

  @Test
  public void testPathStub() {
    assertEquals(BASE+"/sdfsd/sdfsdf", ConnectionUtils.getConnectionPath("user1",
        "user2", "/sdfsd/sdfsdf"));
  }

  @Test
  public void testPathUserStub() {
    assertEquals(BASE+"/sdfsdf/sdfsdf", ConnectionUtils.getConnectionPath("user1",
        "user2", "user2/sdfsdf/sdfsdf"));
  }

  @Test
  public void testPathUserSelector() {
    assertEquals(BASE+".dfsdfds.sdfsdf", ConnectionUtils.getConnectionPath("user1",
        "user2", "user2.dfsdfds.sdfsdf"));
  }
}
