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
package org.sakaiproject.kernel.formauth.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import org.apache.sling.engine.auth.AuthenticationInfo;
import org.easymock.Capture;
import org.junit.Test;
import org.sakaiproject.kernel.formauth.FormAuthenticationHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 */
public class FormAuthenticationHandlerTest {

  /**
   * Test a normal login
   */
  @Test
  public void testLoginOk() {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);

    expect(request.getSession(false)).andReturn(null);
    expect(request.getMethod()).andReturn("POST");
    expect(request.getParameter(FormAuthenticationHandler.FORCE_LOGOUT)).andReturn(null);
    expect(request.getParameter(FormAuthenticationHandler.TRY_LOGIN)).andReturn("1");
    expect(request.getParameter(FormAuthenticationHandler.USERNAME)).andReturn("user");
    expect(request.getParameter(FormAuthenticationHandler.PASSWORD))
        .andReturn("password");
    expect(request.getSession(true)).andReturn(session);
    Capture<Object> captured = new Capture<Object>();
    Capture<String> key = new Capture<String>();
    session.setAttribute(capture(key), capture(captured));
    expectLastCall();

    replay(request, response, session);
    
    
    AuthenticationInfo authenticationInfo = formAuthenticationHandler.authenticate(request, response);
    assertEquals(
        "org.sakaiproject.kernel.formauth.FormAuthenticationHandler$FormAuthentication",
        key.getValue());
    assertEquals(
        "org.sakaiproject.kernel.formauth.FormAuthenticationHandler$FormAuthentication",
        captured.getValue().getClass().getName());
    assertNotNull(authenticationInfo);
    assertEquals(FormAuthenticationHandler.SESSION_AUTH, authenticationInfo.getAuthType());
    Credentials credentials = authenticationInfo.getCredentials();
    assertNotNull(credentials);
    SimpleCredentials sc = (SimpleCredentials) credentials;
    assertEquals("user",sc.getUserID());
    assertArrayEquals("password".toCharArray(),sc.getPassword());
    verify(request, response, session);
  }

  
  /**
   * Test logout from an existing session.
   */
  @Test
  public void testLogout() {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);

    // session already exists
    expect(request.getSession(false)).andReturn(session);
    expect(request.getMethod()).andReturn("POST");
    expect(request.getParameter(FormAuthenticationHandler.FORCE_LOGOUT)).andReturn("1");
    Capture<String> key = new Capture<String>();
    session.removeAttribute(capture(key));
    expectLastCall();

    replay(request, response, session);
    formAuthenticationHandler.authenticate(request, response);
    assertEquals(
        "org.sakaiproject.kernel.formauth.FormAuthenticationHandler$FormAuthentication",
        key.getValue());
    verify(request, response, session);
  }

  /**
   * Test that the logout doesnt create a session.
   */
  @Test
  public void testLogoutDOS() {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);

    // session already exists
    expect(request.getSession(false)).andReturn(null);
    expect(request.getMethod()).andReturn("POST");
    expect(request.getParameter(FormAuthenticationHandler.FORCE_LOGOUT)).andReturn("1");

    replay(request, response, session);
    formAuthenticationHandler.authenticate(request, response);
    verify(request, response, session);
  }

  @Test
  public void testRequestAthentication() throws IOException {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);

    // session already exists
    expect(response.isCommitted()).andReturn(false);
    response.reset();
    expectLastCall();
    response.setStatus(200);
    expectLastCall();
    expect(request.getContextPath()).andReturn("/");
    expect(request.getAuthType()).andReturn(null);
    expect(request.getRemoteUser()).andReturn(null);
    response.setContentType("text/html");
    expectLastCall();
    response.setCharacterEncoding("UTF-8");
    expectLastCall();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    
    expect(response.getWriter()).andReturn(pw);
    expectLastCall();
    
    replay(request, response, session);
    formAuthenticationHandler.requestAuthentication(request, response);
    pw.flush();
    baos.flush();
    assertTrue(baos.size() > 0);
    verify(request, response, session);
  }

  
  

}
