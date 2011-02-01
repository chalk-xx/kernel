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

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.RepositoryImpl;

/**
 * 
 */
public class ConnectionUtilsTest  {

  private Authorizable user1;
  private Authorizable user2;
  private RepositoryImpl repository;
  
  @Before
  public void setUp() throws Exception {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    repository = baseMemoryRepository.getRepository();
    Session session = repository.loginAdministrative();
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    authorizableManager.createUser("user1", "user1", "test", null);
    authorizableManager.createUser("user2", "user2", "test", null);
    user1 = authorizableManager.findAuthorizable("user1");
    user2 = authorizableManager.findAuthorizable("user2");
  }    

  /**
   *
   */
  private static final String BASE = "a:user1/contacts/user2";

  @Test
  public void testBasePath() {
    assertEquals(BASE, ConnectionUtils.getConnectionPath(user1, user2, null));
    assertEquals(BASE, ConnectionUtils.getConnectionPath(user1, user2));
  }

  @Test
  public void testPathEmpty() {
    assertEquals(BASE, ConnectionUtils.getConnectionPath(user1, user2, ""));
  }

  @Test
  public void testPathSelector() {
    assertEquals(BASE+".dsfljksfkjs.sdfsdf", ConnectionUtils.getConnectionPath(user1, user2, ".dsfljksfkjs.sdfsdf"));
  }

  @Test
  public void testPathStub() {
    assertEquals(BASE+"/sdfsd/sdfsdf", ConnectionUtils.getConnectionPath(user1, user2, "/sdfsd/sdfsdf"));
  }

  @Test
  public void testPathUserSelector() {
    assertEquals(BASE+".dfsdfds.sdfsdf", ConnectionUtils.getConnectionPath(user1, user2, ".dfsdfds.sdfsdf"));
  }
}
