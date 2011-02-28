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

package org.sakaiproject.nakamura.email.outgoing;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class LiteEmailDataSource implements DataSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(LiteEmailDataSource.class);

  private Content node;

  private String streamId;

  private ContentManager contentManger;

  public LiteEmailDataSource(ContentManager contentManager, Content node, String streamId) {
    this.node = node;
    this.streamId = streamId;
    this.contentManger = contentManager;
  }

  public String getContentType() {
    String ct = "application/octet-stream";
    if ( node.hasProperty(StorageClientUtils.getAltField(Content.MIMETYPE_FIELD, streamId))) {
      ct = (String) node.getProperty(StorageClientUtils.getAltField(Content.MIMETYPE_FIELD, streamId));
    }
    return ct;
  }

  public InputStream getInputStream() throws IOException {
    try {
      return contentManger.getInputStream(node.getPath(), streamId);
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(),e);
      throw new IOException(e.getMessage(),e);
    } catch (AccessDeniedException e) {
      LOGGER.debug(e.getMessage(),e);
      LOGGER.warn(e.getMessage());
      throw new IOException(e.getMessage(),e);
    }
  }

  public String getName() {
    return streamId;
  }

  public OutputStream getOutputStream() throws IOException {
    throw new IOException("This data source is designed for read only.");
  }

}
