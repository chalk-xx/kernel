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
package org.sakaiproject.kernel.proxy;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.kernel.api.proxy.ProxyClientException;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.api.proxy.ProxyResponse;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 *
 */
public class ProxyClientServiceImplTest extends AbstractEasyMockTest {

  /**
   * 
   */
  private static final String APPLICATION_SOAP_XML_CHARSET_UTF_8 = "application/soap+xml; charset=utf-8";
  private static final String REQUEST_TEMPLATE = "<?xml version=\"1.0\"?>\n"
      + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2001/12/soap-envelope\" "
      + "soap:encodingStyle=\"http://www.w3.org/2001/12/soap-encoding\">"
      + "<soap:Body xmlns:m=\"http://www.example.org/stock\">" + "  <m:GetStockPrice>"
      + "    <m:StockName>$stockName</m:StockName>" + "  </m:GetStockPrice>"
      + "</soap:Body>" + "</soap:Envelope>";

  private static final String STOCK_NAME = "IBM";
  private static final String CHECK_REQUEST = "<m:StockName>" + STOCK_NAME
      + "</m:StockName>";
  private static final String RESPONSE_BODY = "<?xml version=\"1.0\"?>\n"
      + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2001/12/soap-envelope\" "
      + "soap:encodingStyle=\"http://www.w3.org/2001/12/soap-encoding\"> "
      + " <soap:Body xmlns:m=\"http://www.example.org/stock\">"
      + "  <m:GetStockPriceResponse> " + "    <m:Price>34.5</m:Price>"
      + "  </m:GetStockPriceResponse>" + "</soap:Body>" + " </soap:Envelope>";
  private static DummyServer dummyServer;
  private ProxyClientServiceImpl proxyClientServiceImpl;

  @BeforeClass
  public static void beforeClass() {
    dummyServer = new DummyServer();
  }

  @AfterClass
  public static void afterClass() {
    dummyServer.close();
  }

  @Before
  public void before() throws Exception {

    proxyClientServiceImpl = new ProxyClientServiceImpl();
    proxyClientServiceImpl.activate(null);
  }

  @After
  public void after() throws Exception {
    proxyClientServiceImpl.deactivate(null);
  }

  @Test
  public void testInvokeServiceMissingNode() throws ProxyClientException,
      RepositoryException {
    Resource resource = createMock(Resource.class);

    expect(resource.adaptTo(Node.class)).andReturn(null);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      ProxyResponse response = proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);
      try {
        response.close();
      } catch ( Throwable t) {
        
      }
      fail();
    } catch (ProxyClientException ex) {

    }
    verify();
  }

  @Test
  public void testInvokeServiceNodeNoEndPoint() throws ProxyClientException,
      RepositoryException {
    Node node = createMock(Node.class);
    Resource resource = createMock(Resource.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        false);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      ProxyResponse response = proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);
      try {
        response.close();
      } catch ( Throwable t) {
        
      }
      fail();
    } catch (ProxyClientException ex) {
    }
    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPoint() throws ProxyClientException,
      RepositoryException, IOException {
    Node node = createMock(Node.class);
    Resource resource = createMock(Resource.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);
    Property requestContentType = createMock(Property.class);
    Property templateProperty = createMock(Property.class);
    Property lastModifiedProperty = createMock(Property.class);

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);
    expect(endpointProperty.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn("POST");
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_CONTENT_TYPE)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_CONTENT_TYPE)).andReturn(
        requestContentType);
    expect(requestContentType.getString()).andReturn(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(
        true).atLeastOnce();
    expect(node.getProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(
        templateProperty).atLeastOnce();
    expect(templateProperty.getString()).andReturn(REQUEST_TEMPLATE).atLeastOnce();

    expect(node.hasProperty(JcrConstants.JCR_LASTMODIFIED)).andReturn(true).atLeastOnce();
    expect(node.getProperty(JcrConstants.JCR_LASTMODIFIED)).andReturn(
        lastModifiedProperty).atLeastOnce();
    GregorianCalendar now = new GregorianCalendar();
    now.setTimeInMillis(System.currentTimeMillis() - 1000);
    expect(lastModifiedProperty.getDate()).andReturn(now).atLeastOnce();

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(RESPONSE_BODY);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("SOAPAction", "");
    ProxyResponse response = proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", "POST", request.getMethod());
    assertEquals("No Soap Action in request", "", request.getHeader("SOAPAction"));
    assertEquals("Incorrect Content Type in request", APPLICATION_SOAP_XML_CHARSET_UTF_8,
        request.getContentType());

    assertTrue("Template Not merged correctly ", request.getRequestBody().indexOf(
        CHECK_REQUEST) > 0);
    response.close();

    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPointPut() throws ProxyClientException,
      RepositoryException, IOException {
    Node node = createMock(Node.class);
    Resource resource = createMock(Resource.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);
    expect(endpointProperty.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn("PUT");

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(RESPONSE_BODY);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    byte[] bas = new byte[1024];
    for (int i = 0; i < bas.length; i++) {
      bas[i] = (byte) (i & 0xff);
    }
    ByteArrayInputStream bais = new ByteArrayInputStream(bas);
    ProxyResponse response = proxyClientServiceImpl.executeCall(resource, headers, input, bais, bas.length,
        "binary/x-data");

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", "PUT", request.getMethod());
    assertEquals("Incorrect Content Type in request", "binary/x-data", request
        .getContentType());

    assertArrayEquals("Request Not equal ", bas, request.getRequestBodyAsByteArray());
    response.close();

    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPointGet() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("GET", "GET", RESPONSE_BODY);
  }

  @Test
  public void testInvokeServiceNodeEndPointOptions() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("OPTIONS", "OPTIONS", RESPONSE_BODY);
  }

  @Test
  public void testInvokeServiceNodeEndPointHead() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("HEAD", "HEAD", null);
  }

  @Test
  public void testInvokeServiceNodeEndPointOther() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest(null, "GET", RESPONSE_BODY);
  }

  private void testRequest(String type, String expectedMethod, String body)
      throws ProxyClientException, RepositoryException, IOException {
    Node node = createMock(Node.class);
    Resource resource = createMock(Resource.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);
    expect(endpointProperty.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn(type);

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(body);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    ProxyResponse response = proxyClientServiceImpl.executeCall(resource, headers, input,
        null, 0, null);

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", expectedMethod, request.getMethod());
    assertEquals("Incorrect Content Type in request", null, request.getContentType());

    assertEquals(type + "s dont have request bodies ", null, request
        .getRequestBodyAsByteArray());

    assertEquals(body, response.getResponseBodyAsString());
    assertEquals(APPLICATION_SOAP_XML_CHARSET_UTF_8, response.getResponseHeaders().get(
        "Content-Type")[0]);
    
    response.close();

    verify();
  }

}
