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

import static org.apache.commons.lang.CharEncoding.UTF_8;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

/**
 * Wrapper for {@link Content} objects to become a {@link Resource}
 */
public class SparseContentResource extends AbstractResource {
  private static final Logger logger = LoggerFactory.getLogger(SparseContentResource.class);

  private Content content;
  private ContentManager contentManager;
  private ResourceResolver resourceResolver;
  private ResourceMetadata metadata;

  public SparseContentResource(Content content, ContentManager contentManager, ResourceResolver resourceResolver) {
    this.content = content;
    this.contentManager = contentManager;
    this.resourceResolver = resourceResolver;

    Map<String, Object> props = content.getProperties();
    metadata = new ResourceMetadata();
    metadata.setCharacterEncoding(UTF_8);
    metadata.setContentLength(StorageClientUtils.toLong(props.get(Content.LENGTH_FIELD)));
    metadata.setCreationTime(StorageClientUtils.toLong(props.get(Content.CREATED)));
    metadata.setModificationTime(StorageClientUtils.toLong(props.get(Content.LASTMODIFIED)));
    metadata.setResolutionPath(StorageClientUtils.toString(content.getPath()));
    metadata.setResolutionPathInfo(StorageClientUtils.toString(content.getPath()));
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.resource.lite.AbstractResource#adaptTo(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <Type> Type adaptTo(Class<Type> type) {
    Type retval = null;
    if (type == Content.class) {
      retval = (Type) content;
    } else if (type == ContentManager.class) {
      retval = (Type) contentManager;
    } else if (type == InputStream.class) {
      try {
        StringWriter sw = new StringWriter();
        ExtendedJSONWriter writer = new ExtendedJSONWriter(sw);
        writer.valueMap(content.getProperties());
        String s = sw.toString();
        ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes(UTF_8));
        retval = (Type) bais;
        retval = (Type) contentManager.getInputStream(content.getPath());
      } catch (JSONException e) {
        logger.error(e.getMessage(), e);
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.error(e.getMessage(), e);
      } catch (StorageClientException e) {
        logger.error(e.getMessage(), e);
      }
    } else if (type == ValueMap.class) {
      ValueMapDecorator vm = new ValueMapDecorator(content.getProperties());
      retval = (Type) vm;
    } else {
      retval = super.adaptTo(type);
    }
    return retval;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    return content.getPath();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceType()
   */
  public String getResourceType() {
    return "sparseContent";
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
   */
  public ResourceMetadata getResourceMetadata() {
    return metadata;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

}
