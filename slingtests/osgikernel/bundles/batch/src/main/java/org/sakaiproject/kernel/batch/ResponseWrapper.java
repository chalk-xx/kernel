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
package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class ResponseWrapper extends SlingHttpServletResponseWrapper {

  ByteArrayOutputStream boas = new ByteArrayOutputStream();
  ServletOutputStream servletOutputStream = new ServletOutputStream() {
    @Override
    public void write(int b) throws IOException {
      boas.write(b);
    }
  };
  PrintWriter pw = new PrintWriter(servletOutputStream);
  private String type;
  private String charset;
  private int status = 200; // Default is 200, this is also the statuscode if none get's
  // set on the response.
  private Dictionary<String, String> headers;

  public ResponseWrapper(HttpServletResponse wrappedResponse) {
    super((SlingHttpServletResponse)wrappedResponse);
    headers = new Hashtable<String, String>();
  }

  @Override
  public String getCharacterEncoding() {
    return charset;
  }

  @Override
  public String getContentType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#flushBuffer()
   */
  @Override
  public void flushBuffer() throws IOException {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#getOutputStream()
   */
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return servletOutputStream;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#getWriter()
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    return pw;
  }

  @Override
  public void setCharacterEncoding(String charset) {
    this.charset = charset;
  }

  @Override
  public void setContentType(String type) {
    this.type = type;
    headers.put("Content-Type", type);
  }

  public String getDataAsString() throws UnsupportedEncodingException {
    pw.close();
    return boas.toString("utf-8");
  }

  @Override
  public void reset() {
    System.err.println("reset()");
  }

  @Override
  public void resetBuffer() {
    System.err.println("resetBuffer()");
  }

  //
  // Status
  // 

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void setStatus(int sc, String sm) {
    this.status = sc;
  }

  @Override
  public void sendError(int sc) throws IOException {
    this.status = sc;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    this.status = sc;
  }

  public int getResponseStatus() {
    return this.status;
  }

  //
  // Headers
  //

  @Override
  public void setHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    headers.put(name, "" + value);
  }

  @Override
  public void addDateHeader(String name, long date) {
    headers.put(name, "" + date);
  }

  public void setHeaders(Dictionary<String, String> headers) {
    Enumeration<String> keys = headers.keys();
    while (keys.hasMoreElements()) {
      String k = keys.nextElement();
      this.headers.put(k, headers.get(k));
    }
  }

  @Override
  public void setDateHeader(String name, long date) {
    headers.put(name, "" + date);
  }

  public Dictionary<String, String> getResponseHeaders() {
    return headers;
  }
  

}
