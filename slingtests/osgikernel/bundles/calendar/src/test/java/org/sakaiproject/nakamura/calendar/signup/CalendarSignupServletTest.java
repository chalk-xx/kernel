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
package org.sakaiproject.nakamura.calendar.signup;

import static org.mockito.Mockito.verify;

import static org.easymock.EasyMock.verify;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CalendarSignupServletTest {

  private CalendarSignupServlet servlet;

  @Before
  public void setUp() {
    servlet = new CalendarSignupServlet();
  }

  @Test
  public void testAnon() throws ServletException, IOException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(request.getRemoteUser()).thenReturn(UserConstants.ANON_USERID);
    servlet.doPost(request, response);

    verify(response).sendError(Mockito.eq(HttpServletResponse.SC_UNAUTHORIZED),
        Mockito.anyString());
  }

}
