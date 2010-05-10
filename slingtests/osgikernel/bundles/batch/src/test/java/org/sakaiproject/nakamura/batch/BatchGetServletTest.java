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
package org.sakaiproject.nakamura.batch;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class BatchGetServletTest {

  private BatchGetServlet servlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;

  @Before
  public void setup() throws Exception {
    servlet = new BatchGetServlet();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);
  }

  @Test
  public void testParameters() throws IOException, ServletException {

    when(request.getRequestParameter(BatchGetServlet.RESOURCE_PATH_PARAMETER))
        .thenReturn(null);
    servlet.doGet(request, response);
    verify(response).sendError(Mockito.eq(HttpServletResponse.SC_BAD_REQUEST),
        Mockito.anyString());

  }

}
