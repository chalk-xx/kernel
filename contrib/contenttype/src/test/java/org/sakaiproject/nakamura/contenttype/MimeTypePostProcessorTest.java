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
package org.sakaiproject.nakamura.contenttype;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.contenttype.ContentTypePostProcessor.CONTENT_TYPE_HEADER;
import static org.sakaiproject.nakamura.contenttype.ContentTypePostProcessor.CONTENT_TYPE_KEY;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

@RunWith(value = MockitoJUnitRunner.class)
public class MimeTypePostProcessorTest {
  private ContentTypePostProcessor processor;

  @Mock
  SlingHttpServletResponse response;

  @Mock
  ProxyResponse proxyResponse;

  @Before
  public void setUp() {
    processor = new ContentTypePostProcessor();
  }

  @Test
  public void testName() {
    assertEquals(ContentTypePostProcessor.NAME, processor.getName());
  }

  @Test
  public void testProcessWithContentTypeHeader() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, new String[] { "application/json" });
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    processor.process(response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_KEY, "application/json");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void testProcessWithoutContentTypeHeader() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    processor.process(response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_KEY, "");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void testProcessWithNullContentTypeHeader() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, null);
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    processor.process(response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_KEY, "");

    assertEquals(jsonObject.toString(), sw.toString());
  }
}
