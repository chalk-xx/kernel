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
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.lite.Session;
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
import java.util.Iterator;
import java.util.Map;

/**
 * Wrapper for {@link Content} objects to become a {@link Resource}
 */
public class SparseContentResource extends AbstractResource {
  /**
   * Servlets that are designed to deal with any Sparse Content may bind to this resource Type. All Sparse Content has this Resource Type as a Super type.
   * Do not bind to sling/servlet/default or try and using an opting servlet as this will not work.
   */
  public static final String SPARSE_CONTENT_RT = "sparse/Content";

  /**
   * If the Sparse Content contains no "sling:resourceType" property, then the
   * Resource will return this as the fallback resource type. (More or less
   * takes the place of "nt:unstructured" in the JCR world.)
   */
  public static final String SPARSE_CONTENT_UNKNOWN_RT = "sparse/unknown";

  private static final Logger logger = LoggerFactory.getLogger(SparseContentResource.class);

  private Content content;
  private Session session;
  private ResourceResolver resourceResolver;
  private ResourceMetadata metadata;
  private String resourcePath;

  private ContentManager contentManager;

  public SparseContentResource(Content content, Session session, ResourceResolver resourceResolver) throws StorageClientException {
    this(content, session, resourceResolver, null);
  }

  public SparseContentResource(Content content, Session session, ResourceResolver resourceResolver, String resourcePath)
      throws StorageClientException {
    this.content = content;
    this.session = session;
    this.contentManager = session.getContentManager();
    this.resourceResolver = resourceResolver;
    this.resourcePath = resourcePath;

    if (content != null) {
      Map<String, Object> props = content.getProperties();
      metadata = new ResourceMetadata();
      metadata.setCharacterEncoding(UTF_8);
      metadata
          .setContentLength(StorageClientUtils.toLong(props.get(Content.LENGTH_FIELD)));
      metadata
          .setCreationTime(StorageClientUtils.toLong(props.get(Content.CREATED_FIELD)));
      metadata.setModificationTime(StorageClientUtils.toLong(props
          .get(Content.LASTMODIFIED_FIELD)));
      metadata.setResolutionPath(content.getPath());
      metadata.setResolutionPathInfo(content.getPath());
      if (content.hasProperty(Content.MIMETYPE_FIELD)) {
        metadata.setContentType((String) content.getProperty(Content.MIMETYPE_FIELD));
      }
    } else {
      // TODO Better argument checking? Throw IAE?
      IllegalArgumentException iae = new IllegalArgumentException("content is null");
      logger.warn(iae.getLocalizedMessage(), iae);
    }
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
    } else if (type == Session.class) {
      retval = (Type) session;
    } else if (type == InputStream.class) {
      try {
        retval = (Type) contentManager.getInputStream(content.getPath());
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.error(e.getMessage(), e);
      } catch (StorageClientException e) {
        logger.error(e.getMessage(), e);
      }
      if ( retval == null ) {
        try {
          StringWriter sw = new StringWriter();
          ExtendedJSONWriter writer = new ExtendedJSONWriter(sw);
          writer.valueMap(content.getProperties());
          String s = sw.toString();
          ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes(UTF_8));
          retval = (Type) bais;
        } catch (JSONException e) {
          logger.error(e.getMessage(), e);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
    } else if (type == ValueMap.class) {
      // TODO BL120 logic in SparseValueMapDecorator may need another look
      SparseValueMapDecorator vm = new SparseValueMapDecorator(content.getProperties());
      retval = (Type) vm;
    } else {
      retval = super.adaptTo(type);
    }
    return retval;
  }

  /**
   * Returns the path at which the Resource was resolved, or the Content path
   * if the Resource path was not explicitly set. This allows divergences
   * between internal storage paths and externalized paths.
   *
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    if (resourcePath != null) {
      return resourcePath;
    } else {
      return content.getPath();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceType()
   */
  public String getResourceType() {
    String type = (String) content.getProperty("sling:resourceType");
    if (type == null) {
      type = SPARSE_CONTENT_UNKNOWN_RT;
    }
    return type;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    return SPARSE_CONTENT_RT;
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

  /**
   * @see org.sakaiproject.nakamura.api.resource.lite.AbstractResource#listChildren()
   */
  @Override
  public Iterator<Resource> listChildren() {
    return new SparseContentResourceIterator(content.listChildren().iterator(), session, resourceResolver, this);
  }

  @Override
  public String toString() {
    return "{ path=\""+getPath()+"\", resourceType=\""+getResourceType()+"\", resourceSuperType=\""+getResourceSuperType()+"\" metadata=\""+getResourceMetadata()+"\" }";
  }
}
