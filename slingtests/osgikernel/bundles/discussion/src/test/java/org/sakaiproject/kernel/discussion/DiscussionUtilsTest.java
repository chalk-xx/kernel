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

package org.sakaiproject.kernel.discussion;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.sakaiproject.kernel.api.discussion.DiscussionUtils;

public class DiscussionUtilsTest {

  private static final String POST = "6a54e2ecc61aa419f87559e74db29a38ac153ad0";
  private static final String STORE = "/test/store";
  private static final String FULL = "/test/store/ef/35/29/ed/6a54e2ecc61aa419f87559e74db29a38ac153ad0";
  
  @Test
  public void testPath() {
    assertEquals(FULL, DiscussionUtils.getFullPostPath(STORE, POST));
  }
  
  @Test
  public void TestPathWithSelector() {
    String sel = ".reply.html";
    assertEquals(FULL + sel, DiscussionUtils.getFullPostPath(STORE, POST) + sel);
  }
}
