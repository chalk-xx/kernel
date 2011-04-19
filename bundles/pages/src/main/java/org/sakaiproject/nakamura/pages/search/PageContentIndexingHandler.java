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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.ResourceIndexingService;
import org.sakaiproject.nakamura.api.tika.TikaService;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 *
 */
@Component(immediate = true)
public class PageContentIndexingHandler implements IndexingHandler {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(PageContentIndexingHandler.class);

  @Reference(target = "(type=sparse)")
  private ResourceIndexingService resourceIndexingService;

  @Reference
  private TikaService tika;

  @Activate
  protected void activate(BundleContext bundleContext) throws Exception {
    resourceIndexingService.addHandler("sakai/pagecontent", this);
  }

  @Deactivate
  protected void deactivate() {
    resourceIndexingService.removeHandler("sakai/pagecontent", this);
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

    List<SolrInputDocument> docs = Lists.newArrayList();
    if (!StringUtils.isBlank(path)) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        ContentManager cm = session.getContentManager();
        Content content = cm.get(path);

        if (content != null) {
          try {
            String pageContent = (String) content.getProperty("sakai:pagecontent");
            if (pageContent != null) {
              // set the path of the parent that holds the content
              String authId = PathUtils.getAuthorizableId(content.getPath());
              if (authId == null) {
                LOGGER.warn("Unable to find auth (user,group) container for widget data [{}]; not indexing widget data", path);
                return docs;
              }

              SolrInputDocument doc = new SolrInputDocument();

              // extract the content
              String extracted = tika.parseToString(new ByteArrayInputStream(pageContent
                  .getBytes("UTF-8")));
              doc.addField("content", extracted);

              AuthorizableManager am = session.getAuthorizableManager();
              Authorizable auth = am.findAuthorizable(authId);
              if (auth != null) {
                if (auth.isGroup()) {
                  doc.setField("type", "g");
                } else {
                  doc.setField("type", "u");
                }
              }
              // set the path here so that it's the first path found when rendering to the
              // client. the resource indexing service will add all nodes of the path and
              // we want this one to return first in the result processor.
              doc.setField(FIELD_PATH, authId);

              // set the return to a single value field so we can group it
              doc.setField("returnpath", authId);

              // add the source for the indexing service
              doc.addField(_DOC_SOURCE_OBJECT, content);
              docs.add(doc);
            }
          } catch (TikaException e) {
            LOGGER.warn(e.getMessage(), e);
          } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
          }
        }
      } catch (StorageClientException e) {
        LOGGER.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    }
    LOGGER.debug("Got documents {}", docs);
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
    LOGGER.debug("GetDelete for {} ", event);
    String path = (String) event.getProperty(FIELD_PATH);
    return ImmutableList.of("id:" + ClientUtils.escapeQueryChars(path));
  }

}
