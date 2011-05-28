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
package org.sakaiproject.nakamura.files.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.lite.util.Iterables;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.ResourceIndexingService;
import org.sakaiproject.nakamura.api.tika.TikaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Indexes content with the property sling:resourceType = "sakai/pooled-content".
 */
@Component(immediate = true)
public class PoolContentResourceTypeHandler implements IndexingHandler {

  private static final Set<String> IGNORE_NAMESPACES = ImmutableSet.of("jcr", "rep");
  private static final Set<String> IGNORE_PROPERTIES = ImmutableSet.of();
  private static final Map<String, String> INDEX_FIELD_MAP = getFieldMap();

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PoolContentResourceTypeHandler.class);
  private static final String[] CONTENT_TYPES = new String[] {
    "sakai/pooled-content"
  };

  @Reference(target="(type=sparse)")
  protected ResourceIndexingService resourceIndexingService;

  @Reference
  private TikaService tika;

  private static Map<String, String> getFieldMap() {
    Builder<String, String> builder = ImmutableMap.builder();
    builder.put(FilesConstants.POOLED_CONTENT_USER_MANAGER, "manager");
    builder.put(FilesConstants.POOLED_CONTENT_USER_VIEWER, "viewer");
    builder.put(FilesConstants.POOLED_CONTENT_FILENAME, "filename");
    builder.put(FilesConstants.POOLED_NEEDS_PROCESSING, "needsprocessing");
    builder.put(FilesConstants.POOLED_CONTENT_CUSTOM_MIMETYPE, "custom-mimetype");
    builder.put(FilesConstants.SAKAI_FILE, "file");
    builder.put(FilesConstants.SAKAI_TAG_NAME, "tagname");
    builder.put(FilesConstants.SAKAI_TAG_UUIDS, "taguuid");
    builder.put(FilesConstants.SAKAI_TAGS, "tag");
    builder.put(FilesConstants.SAKAI_PAGE_COUNT, "pagecount");
    builder.put(FilesConstants.LAST_MODIFIED, FilesConstants.LAST_MODIFIED);
    builder.put(FilesConstants.LAST_MODIFIED_BY, FilesConstants.LAST_MODIFIED_BY);
    builder.put(FilesConstants.CREATED, FilesConstants.CREATED);
    builder.put(FilesConstants.CREATED_BY, FilesConstants.CREATED_BY);
    builder.put(FilesConstants.LINK_PATHS, FilesConstants.LINK_PATHS);
    builder.put(FilesConstants.SAKAI_DESCRIPTION, "description");
    return builder.build();
  }

  // ---------- SCR integration-------------------------------------------------

  @Activate
  public void activate(BundleContext bundleContext, Map<String, Object> properties) throws Exception {
    for (String type : CONTENT_TYPES) {
      resourceIndexingService.addHandler(type, this);
    }
  }

  @Deactivate
  public void deactivate(Map<String, Object> properties) {
    for (String type : CONTENT_TYPES) {
      resourceIndexingService.removeHandler(type, this);
    }
  }

  // ---------- IndexingHandler interface --------------------------------------
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDocuments(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<SolrInputDocument> getDocuments(RepositorySession repositorySession,
      Event event) {
    LOGGER.debug("GetDocuments for {} ", event);
    String path = (String) event.getProperty("path");
    if (ignorePath(path)) {
      return Collections.emptyList();
    }
    List<SolrInputDocument> documents = Lists.newArrayList();
    if (path != null) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        ContentManager contentManager = session.getContentManager();
        Content content = contentManager.get(path);
        if (content != null) {
          SolrInputDocument doc = new SolrInputDocument();

          Map<String, Object> properties = content.getProperties();

          for (Entry<String, Object> p : properties.entrySet()) {
            String indexName = index(p);
            if (indexName != null) {
              for (Object o : convertToIndex(p)) {
                doc.addField(indexName, o);
              }
            }
          }

          InputStream contentStream = contentManager.getInputStream(path);
          if (contentStream != null) {
            try {
              String extracted = tika.parseToString(contentStream);
              doc.addField("content", extracted);
            } catch (TikaException e) {
              LOGGER.warn(e.getMessage(), e);
            }
          }

          doc.addField(_DOC_SOURCE_OBJECT, content);
          documents.add(doc);
        }
      } catch (ClientPoolException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (StorageClientException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (IOException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    }
    LOGGER.debug("Got documents {} ", documents);
    return documents;
  }

  /**
   * Gets the principals that can read content at a given path.
   *
   * @param session
   * @param path
   *          The path to check.
   * @return {@link String[]} of principal names that can read {@link path}. An empty
   *         array is returned if no principals can read the path.
   * @throws StorageClientException
   */
  @SuppressWarnings("unused")
  private String[] getReadingPrincipals(Session session, String path) throws StorageClientException {
    AccessControlManager accessControlManager = session.getAccessControlManager();
    return accessControlManager.findPrincipals(Security.ZONE_CONTENT ,path, Permissions.CAN_READ.getPermission(), true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDeleteQueries(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<String> getDeleteQueries(RepositorySession repositorySession, Event event) {
    LOGGER.debug("GetDelete for {} ", event);
    String path = (String) event.getProperty("path");
    boolean ignore = ignorePath(path);
    if ( ignore ) {
      return Collections.emptyList();
    } else {
      return ImmutableList.of(FIELD_ID + ":" + ClientUtils.escapeQueryChars(path));
    }
  }

  public void setResourceIndexingService(ResourceIndexingService resourceIndexingService) {
    if (resourceIndexingService != null) {
      this.resourceIndexingService = resourceIndexingService;
    }
  }

  /**
   * Determine whether a path should be ignored for indexing.
   *
   * @param path
   *          The path to check.
   * @return true if the path should be ignored. false, otherwise.
   */
  protected boolean ignorePath(String path) {
    return false;
  }

  /**
   * Converts an entry to the proper storage format. If the entry is listed as a property
   * that should be stored as an array, the value is split on comma into an array.
   * Otherwise the value is stored as-is in the first element of an array.
   *
   * @param p
   *          The entry to format.
   * @return {@link Iterable} of values for storage. If the property should be an array,
   *         it is split on comma. Otherwise it is stored as-is as the first element of an
   *         array.
   */
  private Iterable<?> convertToIndex(Entry<String, Object> p) {
    Object values = p.getValue();
    if ( values instanceof Object[] ) {
      return Iterables.of((Object[])values);
    }

    return Iterables.of(new Object[] { values });
  }

  /**
   * Get the index name for a given {@link Map.Entry}. Checks that the entry is on the
   * whitelist and is not listed as an ignored namespace or ignored property.
   *
   * @param e
   *          The entry to get an index name for.
   * @return The name of the index to use for the given entry. null if the entry should
   *         not be indexed.
   */
  protected String index(Entry<String, Object> e) {
    String name = e.getKey();
    if (!INDEX_FIELD_MAP.containsKey(name)) {
      String[] parts = StringUtils.split(name, ':');
      if (IGNORE_NAMESPACES.contains(parts[0])) {
        return null;
      }
      if (IGNORE_PROPERTIES.contains(name)) {
        return null;
      }
    }
    String mappedName = INDEX_FIELD_MAP.get(name);
    // only fields in the map will be used, and those are in the schema.
    return mappedName;
  }

}
