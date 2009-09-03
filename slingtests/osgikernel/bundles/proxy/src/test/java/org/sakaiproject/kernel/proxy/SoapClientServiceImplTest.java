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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.kernel.api.proxy.ProxyClientException;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.kernel.testutils.http.CapturedRequest;
import org.sakaiproject.kernel.testutils.http.DummyServer;

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
public class SoapClientServiceImplTest extends AbstractEasyMockTest {

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

  @Test
  public void testInvokeServiceMissingNode() throws ProxyClientException,
      RepositoryException {
    Resource resource = createMock(Resource.class);

    expect(resource.adaptTo(Node.class)).andReturn(null);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);
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

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(false);

    replay();
    Map<String, String> input = new HashMap<String, String>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);
      fail();
    } catch (ProxyClientException ex) {

    }
    verify();
  }

//  @Test
  public void testInvokeServiceNodeEndPoint() throws ProxyClientException,
      RepositoryException, IOException {
    Node node = createMock(Node.class);
    Resource resource = createMock(Resource.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();
    
    
    Property endpointProperty = createMock(Property.class);
    Property templateProperty = createMock(Property.class);
    Property lastModifiedProperty = createMock(Property.class);

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(true);
    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);
    expect(endpointProperty.getString()).andReturn(dummyServer.getUrl());
    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(true)
        .atLeastOnce();
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
    proxyClientServiceImpl.executeCall(resource, headers, input, null, 0, null);

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("No Soap Action in request", "", request.getHeader("SOAPAction"));
    assertEquals("Incorrect Content Type in request", APPLICATION_SOAP_XML_CHARSET_UTF_8,
        request.getContentType());

    assertTrue("Template Not merged correctly ", request.getRequestBody().indexOf(
        CHECK_REQUEST) > 0);

    verify();
  }

}
