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
package org.sakaiproject.kernel2.uxloader;

import org.sakaiproject.kernel.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * 
 */
public class FileCache implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = -5424547795667511253L;
  private byte[] content;
  private long lastModified;
  private String contentType;
  private int contentLength;

  /**
   * @param welcome
   * @param string 
   * @throws IOException 
   */
  public FileCache(File welcome, String contentType) throws IOException {
    lastModified = welcome.lastModified();
    this.contentType = contentType;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    InputStream in = new FileInputStream(welcome);
    try {
    IOUtils.stream(in, baos);
    content = baos.toByteArray();
    } finally {
      baos.close();
      in.close();
    }
    contentLength = content.length;
  }

  /**
   * @return
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * @return
   */
  public int getContentLength() {
    return contentLength;
  }

  /**
   * @return
   */
  public byte[] getContent() {
    return content;
  }

  /**
   * @return
   */
  public long getLastModified() {
    return lastModified;
  }

}
