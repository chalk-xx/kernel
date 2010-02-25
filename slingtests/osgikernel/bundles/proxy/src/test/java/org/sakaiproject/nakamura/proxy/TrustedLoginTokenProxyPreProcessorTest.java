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
package org.sakaiproject.nakamura.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;

import java.security.MessageDigest;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TrustedLoginTokenProxyPreProcessorTest {

  private final String secret = "e2KS54H35j6vS5Z38nK40";

  private TrustedLoginTokenProxyPreProcessor proxyPreProcessor;

  @Mock
  SlingHttpServletRequest request;
  
  @Mock
  ComponentContext mockContext;
  
  Dictionary<String,String> properties = new Hashtable<String,String>();

  Map<String, String> headers;

  Map<String, Object> templateParams;

  @Before
  public void setup() throws Exception {
    proxyPreProcessor = new TrustedLoginTokenProxyPreProcessor();
    headers = new HashMap<String, String>();
    templateParams = new HashMap<String, Object>();
  }

  @Test
  public void nameIsAsExpected() {
    assertEquals("trusted-token", proxyPreProcessor.getName());
  }

  @Test
  public void processorAddsValidHashToHeaders() throws Exception {
    // given
    requestCanReturnUserName();

    // when
    proxyPreProcessor.preProcessRequest(request, headers, templateParams);

    // then
    assertNotNull(headers
        .get(TrustedLoginTokenProxyPreProcessor.SECURE_TOKEN_HEADER_NAME));
    String[] tokenParts = headers.get(
        TrustedLoginTokenProxyPreProcessor.SECURE_TOKEN_HEADER_NAME).split(";");
    String theirHash = tokenParts[0];
    assertEquals(theirHash, myHash(tokenParts));

  }
  
  @Test
  public void reflectsPortParameterAsConfigured() {
    // given
    requestCanReturnUserName();
    componentContextReturnsProperties();
    properties.put("port", "8080");
    
    // when
    proxyPreProcessor.activate(mockContext);
    proxyPreProcessor.preProcessRequest(request, headers, templateParams);
    
    // then
    assertEquals(8080, templateParams.get("port"));
  }

  private void componentContextReturnsProperties() {
    when(mockContext.getProperties()).thenReturn(properties);
  }

  private String myHash(String[] tokenParts) throws Exception {
    String user = tokenParts[1];
    String timestamp = tokenParts[2];
    String toHash = secret + TrustedLoginTokenProxyPreProcessor.TOKEN_SEPARATOR + user
        + TrustedLoginTokenProxyPreProcessor.TOKEN_SEPARATOR + timestamp;
    return byteArrayToHexStr(MessageDigest.getInstance(
        TrustedLoginTokenProxyPreProcessor.HASH_ALGORITHM).digest(
        toHash.getBytes("UTF-8")));
  }

  protected String byteArrayToHexStr(byte[] data) {
    char[] chars = new char[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      byte current = data[i];
      int hi = (current & 0xF0) >> 4;
      int lo = current & 0x0F;
      chars[2 * i] = (char) (hi < 10 ? ('0' + hi) : ('A' + hi - 10));
      chars[2 * i + 1] = (char) (lo < 10 ? ('0' + lo) : ('A' + lo - 10));
    }
    return new String(chars);
  }

  private void requestCanReturnUserName() {
    when(request.getRemoteUser()).thenReturn("zach");
  }

}
