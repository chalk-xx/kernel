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
package org.sakaiproject.nakamura.docproxy.disk;

import org.sakaiproject.kernel.api.docproxy.ExternalDocumentResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

/**
 *
 */
public class DiskDocumentResult implements ExternalDocumentResult {

  private File file;

  public DiskDocumentResult(File file) {
    this.file = file;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResult#getDocumentInputStream(long)
   */
  public InputStream getDocumentInputStream(long startingAt) {
    try {
      FileInputStream in = new FileInputStream(file);
      in.skip(startingAt);
      return in;
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata#getContentLength()
   */
  public long getContentLength() {
    return file.length();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata#getContentType()
   */
  public String getContentType() {
    MimetypesFileTypeMap map = new MimetypesFileTypeMap();
    return map.getContentType(file);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata#getProperties()
   */
  public Map<String, Object> getProperties() {
    return new HashMap<String, Object>();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata#getType()
   */
  public String getType() {
    return DiskProcessor.TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata#getUri()
   */
  public String getUri() {
    return file.toURI().toString();
  }

}
