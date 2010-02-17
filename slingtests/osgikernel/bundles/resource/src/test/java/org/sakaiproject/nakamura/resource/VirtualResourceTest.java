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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;


/**
 *
 */
public class VirtualResourceTest extends AbstractEasyMockTest {

  @Test
  public void test() {
    Resource resource = createNiceMock(Resource.class);
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    expect(resource.getResourceMetadata()).andReturn(resourceMetadata);
    replay();
    VirtualResource virtualResource = new VirtualResource(resource, "/bla/bla");
    assertEquals("/bla/bla",virtualResource.getPath());
    assertEquals("sling/servlet/default",virtualResource.getResourceType());
    assertEquals(resourceMetadata, virtualResource.getResourceMetadata());
    
    verify();
  }
}
