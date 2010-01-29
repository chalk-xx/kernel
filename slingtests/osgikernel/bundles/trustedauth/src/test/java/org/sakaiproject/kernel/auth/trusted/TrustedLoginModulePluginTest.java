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
package org.sakaiproject.kernel.auth.trusted;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.auth.trusted.TrustedTokenService.TrustedUser;

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 *
 */
public class TrustedLoginModulePluginTest {
  private TrustedTokenService trustedTokenService;
  private List<Object> mocks = new ArrayList<Object>();

  @Before
  public void before() throws NoSuchAlgorithmException {
    mocks.clear();
    trustedTokenService = new TrustedTokenService();
  }
  
  public ComponentContext configureForSession() {
    ComponentContext context = createMock(ComponentContext.class);
    Hashtable<String, Object> dict = new Hashtable<String, Object>();
    dict.put(TrustedTokenService.USE_SESSION, true);
    dict.put(TrustedTokenService.COOKIE_NAME, "secure-cookie");
    dict.put(TrustedTokenService.TTL, 1200000L);
    dict.put(TrustedTokenService.SECURE_COOKIE, false);
    EasyMock.expect(context.getProperties()).andReturn(dict);
    return context;
  }
  
  

  @Test
  public void testGetCredentialsValidSession() throws RepositoryException, LoginException {
    ComponentContext context = configureForSession();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpSession session = createMock(HttpSession.class);
    EasyMock.expect(request.getSession(true)).andReturn(session);
    
    Principal principal = createMock(Principal.class);
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal);
    EasyMock.expect(principal.getName()).andReturn(null);
    EasyMock.expect(request.getRemoteUser()).andReturn("ieb");
    Capture<SimpleCredentials> attributeValue = new Capture<SimpleCredentials>();
    Capture<String> attributeName = new Capture<String>();
    session.setAttribute(EasyMock.capture(attributeName), EasyMock.capture(attributeValue));
    
    
    HttpServletResponse response = createMock(HttpServletResponse.class);
    
    replay();
    trustedTokenService.activate(context); 
    trustedTokenService.injectToken(request, response);
    Assert.assertTrue(attributeName.hasCaptured());
    Assert.assertTrue(attributeValue.hasCaptured());
    Credentials credentials = attributeValue.getValue();
    
    verify();
    reset();

//    EasyMock.expect(request.getSession(false)).andReturn(session);
    EasyMock.expect(session.getAttribute(TrustedTokenService.SA_AUTHENTICATION_CREDENTIALS)).andReturn(credentials).anyTimes();
    
    replay();
    
    TrustedLoginModulePlugin loginModulePlugin = new TrustedLoginModulePlugin();
    loginModulePlugin.doInit(null, null, null);
    Assert.assertTrue(loginModulePlugin.canHandle(credentials));
    Assert.assertFalse(loginModulePlugin.canHandle(new SimpleCredentials("user", new char[0])));
    Principal principal1 = loginModulePlugin.getPrincipal(credentials);
    Assert.assertNotNull(principal1);
    Assert.assertNull(loginModulePlugin.getPrincipal(new SimpleCredentials("user", new char[0])));
    
    AuthenticationPlugin authenticationPlugin = loginModulePlugin.getAuthentication(principal1, credentials);
    Assert.assertNotNull(authenticationPlugin);
    Assert.assertNull(loginModulePlugin.getAuthentication(principal1, new SimpleCredentials("user1", new char[0])));
    
    Assert.assertTrue(authenticationPlugin.authenticate(credentials));
    Assert.assertFalse(authenticationPlugin.authenticate(new SimpleCredentials("user2", new char[0])));
    verify();
  }

  


  public <T> T createMock(Class<T> mockClass) {
    T m = EasyMock.createMock(mockClass);
    mocks.add(m);
    return m;
  }
  
  public void replay() {
    EasyMock.replay(mocks.toArray());
  }
  
  public void verify() {
    EasyMock.verify(mocks.toArray());
  }
  
  public void reset() {
    EasyMock.reset(mocks.toArray());
  }
}
