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
package org.sakaiproject.nakamura.auth.sso;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class SsoAuthenticationTokenServiceWrapperTest {

  SsoLoginServlet servlet;

  TrustedTokenServiceImpl delegate;

  @Mock
  SsoLoginServlet mockedServlet;

  @Mock
  SlingHttpServletRequest request;

  @Mock
  SlingHttpServletResponse response;

  SsoAuthenticationTokenServiceWrapper wrapper;

  @Before
  public void setUp() throws Exception {
    servlet = new SsoLoginServlet();
    delegate = new TrustedTokenServiceImpl();
    delegate.activateForTesting();
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateFailure() throws Exception {
    wrapper = new SsoAuthenticationTokenServiceWrapper(mockedServlet, delegate);
  }

  @Test
  public void validateSuccess() {
    wrapper = new SsoAuthenticationTokenServiceWrapper(servlet, delegate);
  }

  @Test
  public void addToken() throws Exception {
    validateSuccess();
    wrapper.addToken(request, response);
  }
}
