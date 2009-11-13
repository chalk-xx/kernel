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
package org.sakaiproject.kernel.resource;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class VirtualResourceProviderImplTest {

  @Test
  public void testGetResourcePath() {
    VirtualResourceProviderImpl v = new VirtualResourceProviderImpl();
    assertEquals("/test", v.getResourcePath("/test.a"));
    assertEquals("/test.a", v.getResourcePath("/test.a.b"));
    assertEquals("/test.a.b", v.getResourcePath("/test.a.b."));
    assertEquals("/test/x/y", v.getResourcePath("/test/x/y.a"));
    assertEquals("/test/x/y/", v.getResourcePath("/test/x/y/.a"));
    assertEquals("/test/a/", v.getResourcePath("/test/a/"));
    assertEquals("/", v.getResourcePath("/test"));
    assertEquals("/te.st/x/y/", v.getResourcePath("/te.st/x/y/.a"));
    assertEquals("/te.st/", v.getResourcePath("/te.st/y"));
  }

}
