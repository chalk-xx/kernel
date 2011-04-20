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
package org.sakaiproject.nakamura.message.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.ResourceIndexingService;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Indexes messages for searching as autonomous items or as part of the user,group
 * searching.
 */
@Component(immediate = true)
public class MessageIndexingHandler implements IndexingHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageIndexingHandler.class);

  private static final Map<String, String> WHITELISTED_PROPS;
  static {
    Builder<String,String> propBuilder = ImmutableMap.builder();
    propBuilder.put("sakai:messagebox", "messagebox");
    propBuilder.put("sakai:type", "type");
    propBuilder.put(Content.CREATED_FIELD, Content.CREATED_FIELD);
    propBuilder.put("sakai:category", "category");
    propBuilder.put("sakai:from", "from");
    propBuilder.put("sakai:to", "to");
    propBuilder.put("sakai:read", "read");
    propBuilder.put("sakai:marker", "marker");
    propBuilder.put("sakai:sendstate", "sendstate");
    propBuilder.put("sakai:initialpost", "initialpost");
    WHITELISTED_PROPS = propBuilder.build();
  }

  private static final Logger logger = LoggerFactory
      .getLogger(MessageIndexingHandler.class);

  private static final String AUTH_SUFFIX = "-auth";

  @Reference(target = "(type=sparse)")
  private ResourceIndexingService resourceIndexingService;

  @Activate
  protected void activate(Map<?, ?> props) {
    resourceIndexingService.addHandler(MessageConstants.SAKAI_MESSAGE_RT, this);
  }

  @Deactivate
  protected void deactivate(Map<?, ?> props) {
    resourceIndexingService.removeHandler(MessageConstants.SAKAI_MESSAGE_RT, this);
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

    List<SolrInputDocument> documents = Lists.newArrayList();
    if (!StringUtils.isBlank(path)) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        ContentManager cm = session.getContentManager();
        Content content = cm.get(path);

        if (content != null) {
          // index as autonomous message
          SolrInputDocument doc = new SolrInputDocument();
          for (String prop : WHITELISTED_PROPS.keySet()) {
            Object value = content.getProperty(prop);
            doc.addField(WHITELISTED_PROPS.get(prop), value);
          }
          doc.addField(_DOC_SOURCE_OBJECT, content);
          documents.add(doc);

          // index for user,group searching
          String authId = PathUtils.getAuthorizableId(content.getPath());
          AuthorizableManager am = session.getAuthorizableManager();
          Authorizable auth = am.findAuthorizable(authId);
          if (auth == null) {
            LOGGER.warn("Unable to find auth (user,group) container for message [{}]; not indexing message for user,group searching", path);
          } else {
            doc = new SolrInputDocument();
            doc.addField("title", content.getProperty("sakai:subject"));
            doc.addField("content", content.getProperty("sakai:body"));

            if (auth.isGroup()) {
              doc.setField("type", "g");
            } else {
              doc.setField("type", "u");
            }
            doc.addField(_DOC_SOURCE_OBJECT, content);

            // set the path here so that it's the first path found when rendering to the
            // client. the resource indexing service will add all nodes of the path and
            // we want this one to return first in the result processor.
            doc.setField(FIELD_PATH, authId);
            doc.addField(FIELD_ID, path + AUTH_SUFFIX);

            // set the return to a single value field so we can group it
            doc.setField("returnpath", authId);
            documents.add(doc);
          }
        }
      } catch (StorageClientException e) {
        logger.warn(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.warn(e.getMessage(), e);
      }
    }
    logger.debug("Got documents {} ", documents);
    return documents;
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
    return ImmutableList.of("id:(" + ClientUtils.escapeQueryChars(path) + " OR " + ClientUtils.escapeQueryChars(path + AUTH_SUFFIX) + ")");
  }

}
