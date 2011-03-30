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
package org.sakaiproject.nakamura.pages.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.ResourceIndexingService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Indexing handler for widget data stored under a group. See {@link https
 * ://confluence.sakaiproject.org/display/KERNDOC/KERN-1675+Searching+Widget+Data} for
 * more details.
 */
public class WidgetDataIndexingHandler implements IndexingHandler {

  public static final String INDEXED_FIELDS = "sakai:indexed-fields";

  private static final Logger logger = LoggerFactory
      .getLogger(WidgetDataIndexingHandler.class);

  @Reference(target = "(type=sparse)")
  private ResourceIndexingService resourceIndexingService;

  @Activate
  protected void activate() {
    resourceIndexingService.addHandler("sakai/widget-data", this);
  }

  @Deactivate
  protected void deactivate() {
    resourceIndexingService.removeHandler("sakai/widget-data", this);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDocuments(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<SolrInputDocument> getDocuments(RepositorySession repositorySession,
      Event event) {
    String path = (String) event.getProperty(FIELD_PATH);

    Collection<SolrInputDocument> docs = Lists.newArrayList();
    if (!StringUtils.isBlank(path)) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        ContentManager cm = session.getContentManager();
        Content content = cm.get(path);

        String[] authPath = getAuthHomePath(content, cm);
        if (authPath == null) {
          logger.warn("Unable to find auth (user,group) container for widget data [{}]; not indexing widget data", path);
          return docs;
        }

        Object fields = content.getProperty(INDEXED_FIELDS);
        String[] indexedFields = null;
        if (fields instanceof String) {
          indexedFields = new String[] { (String) fields };
        } else if (fields instanceof String[]) {
          indexedFields = (String[]) fields;
        }

        // concatenate the fields requested to be indexed.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexedFields.length; i++) {
          sb.append(indexedFields[i]);
          if (i < indexedFields.length - 1) {
            sb.append(' ');
          }
        }

        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("type", authPath[0]);
        doc.setField("path", authPath[1]);
        doc.setField("widgetdata", sb.toString());
        docs.add(doc);
      } catch (StorageClientException e) {
        logger.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.warn(e.getMessage(), e);
      }
    }
    return docs;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.solr.IndexingHandler#getDeleteQueries(org.sakaiproject.nakamura.api.solr.RepositorySession,
   *      org.osgi.service.event.Event)
   */
  public Collection<String> getDeleteQueries(RepositorySession respositorySession,
      Event event) {
    logger.debug("GetDelete for {} ", event);
    String path = (String) event.getProperty(FIELD_PATH);
    return ImmutableList.of("id:" + ClientUtils.escapeQueryChars(path));
  }

  /**
   * Starting at {@link content}, go up the content tree to find a "home" node and return
   * the resource path to that node with a marker of user or group.
   * 
   * @param content
   * @param cm
   * @return String[] 0:(u|g); 1:path to auth home
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  private String[] getAuthHomePath(Content content, ContentManager cm)
      throws AccessDeniedException, StorageClientException {
    if (content.hasProperty(UserConstants.GROUP_HOME_RESOURCE_TYPE)) {
      return new String[] { "g", content.getPath().substring(2) };
    } else if (content.hasProperty(UserConstants.USER_HOME_RESOURCE_TYPE)) {
      return new String[] { "u", content.getPath().substring(2) };
    } else if ("/".equals(content.getPath())) {
      return null;
    } else {
      return getAuthHomePath(cm.get(PathUtils.getParentReference(content.getPath())), cm);
    }
  }
}
