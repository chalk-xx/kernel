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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.sakaiproject.kernel.api.proxy.ProxyResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the ProxyResponse holder by wrapping the HttpMethod request.
 */
public class ProxyResponseImpl implements ProxyResponse {

  private int result;
  private HttpMethod method;
  private Map<String, String[]> headers = new HashMap<String, String[]>();

  /**
   * @param result
   * @param method
   */
  public ProxyResponseImpl(int result, HttpMethod method) {
    this.result = result;
    this.method = method;

    for (Header h : method.getResponseHeaders()) {
      String name = h.getName();
      String[] values = headers.get(name);
      if ( values == null ) {
        values = new String[] {h.getValue()};
      } else {
        String[] newValues = new String[values.length+1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[values.length] = h.getValue();
        values = newValues;
      }
        headers.put(name, values);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#getResultCode()
   */
  public int getResultCode() {
    return result;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#getResponseHeaders()
   */
  public Map<String, String[]> getResponseHeaders() {
    return headers;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#getResponseBody()
   */
  public byte[] getResponseBody() throws IOException {
    return method.getResponseBody();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#getResponseBodyAsInputStream()
   */
  public InputStream getResponseBodyAsInputStream() throws IOException {
    return method.getResponseBodyAsStream();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#getResponseBodyAsString()
   */
  public String getResponseBodyAsString() throws IOException {
    return method.getResponseBodyAsString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.proxy.ProxyResponse#close()
   */
  public void close() {
    method.releaseConnection();
    method = null;
  }

}
