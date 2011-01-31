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
package org.sakaiproject.nakamura.api.resource.lite;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.util.PreemptiveIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class SparseContentResourceIterator extends PreemptiveIterator<Resource> {
  private static final Logger logger = LoggerFactory.getLogger(SparseContentResourceIterator.class);
  final private Iterator<Content> contentIterator;
  final private Session session;
  final private ResourceResolver resourceResolver;
  private SparseContentResource nextResource;

  public SparseContentResourceIterator(Iterator<Content> contentIterator, Session session, ResourceResolver resourceResolver) {
    this.contentIterator = contentIterator;
    this.session = session;
    this.resourceResolver = resourceResolver;
  }

  @Override
  protected boolean internalHasNext() {
    nextResource = null;
    while (nextResource == null && contentIterator.hasNext()) {
      Content content = contentIterator.next();
      try {
        nextResource = new SparseContentResource(content, session, resourceResolver);
      } catch (StorageClientException e) {
        logger.debug("Unable to convert content {} to resource; cause {}", new Object[] {
            content, e.getMessage() }, e);
      }
    }
    return (nextResource != null);
  }

  @Override
  protected Resource internalNext() {
    return nextResource;
  }

}
