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
package org.sakaiproject.nakamura.docproxy.xythos;

import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public class XythosDocumentResult implements ExternalDocumentResult {

  private InputStream inputStream;
  private long contentLength;
  private String contentType;
  private Map<String, Object> properties;
  private String uri;

  public XythosDocumentResult(byte[] data, long contentLength, String contentType, Map<String, Object> props, String uri) {
    this.inputStream = new ByteArrayInputStream(data);
    this.contentLength = contentLength;
    this.contentType = contentType;
    this.properties = props;
    this.uri = uri;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult#getDocumentInputStream(long)
   */
  public InputStream getDocumentInputStream(long startingAt) throws DocProxyException {
    return inputStream;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getContentLength()
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getContentType()
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getProperties()
   */
  public Map<String, Object> getProperties() throws DocProxyException {
    return properties;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getType()
   */
  public String getType() {
    return XythosRepositoryProcessor.TYPE;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getUri()
   */
  public String getUri() {
    return uri;
  }

}
