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

import static org.easymock.EasyMock.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;


/**
 *
 */
public class VirtualResourceProviderServletTest extends AbstractEasyMockTest {

  
  @Test
  public void test() throws ServletException, IOException {
    VirtualResourceType virtualResourceType = createNiceMock(VirtualResourceType.class);
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createNiceMock(SlingHttpServletResponse.class);
    expect(virtualResourceType.getResourceType()).andReturn("resourcetype");
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    expect(response.getWriter()).andReturn(printWriter);
    replay();
    VirtualResourceProviderServlet vrp = new VirtualResourceProviderServlet();
    vrp.bindVirtualResourceType(virtualResourceType);
    vrp.doGet(request, response);
    verify();
  }
}
