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
package org.sakaiproject.nakamura.connections.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * <p>Indexing handler for contact connections between two users.</p>
 * <p>The fields that are indexed are:<br/>
 * <ul>
 * <li>connection status: state</li>
 * <li>contact's username: name</li>
 * <li>contact's first name: firstName</li>
 * <li>contact's last name: lastName</li>
 * <li>contact's email: email</li>
 * </ul>
 */
@Component(immediate = true)
public class ConnectionIndexingHandler implements IndexingHandler {

  private static final Logger logger = LoggerFactory
      .getLogger(ConnectionIndexingHandler.class);

  private static final Map<String, String> WHITELISTED_PROPS = ImmutableMap.of(
      "sakai:state", "state");
  private static final Set<String> FLATTENED_PROPS = ImmutableSet.of("name", "firstName",
      "lastName", "email");

  @Reference(target = "(type=sparse)")
  private ResourceIndexingService resourceIndexingService;

  @Activate
  protected void activate(Map<?, ?> props) {
    resourceIndexingService.addHandler(ConnectionConstants.SAKAI_CONTACT_RT, this);
  }

  @Deactivate
  protected void deactivate(Map<?, ?> props) {
    resourceIndexingService.removeHandler(ConnectionConstants.SAKAI_CONTACT_RT, this);
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

    logger.info("Indexing connections at path {}", path);
    List<SolrInputDocument> documents = Lists.newArrayList();
    if (!StringUtils.isBlank(path)) {
      try {
        Session session = repositorySession.adaptTo(Session.class);
        ContentManager cm = session.getContentManager();
        Content content = cm.get(path);

        int lastSlash = path.lastIndexOf('/');
        String contactName = path.substring(lastSlash + 1);
        AuthorizableManager am = session.getAuthorizableManager();
        Authorizable contactAuth = am.findAuthorizable(contactName);

        if (content != null && contactAuth != null) {
          SolrInputDocument doc = new SolrInputDocument();
          for (Entry<String, String> prop: WHITELISTED_PROPS.entrySet()) {
            String key = prop.getKey();
            Object value = content.getProperty(key);
            if ( value != null ) {
              doc.addField(WHITELISTED_PROPS.get(key), value);
            }
          }

          // flatten out the contact so we can search it
          Map<String, Object> contactProps = contactAuth.getSafeProperties();
          if ( contactAuth != null ) {
            for (String prop : FLATTENED_PROPS) {
              Object value = contactProps.get(prop);
              if ( value != null ) {
                doc.addField(prop, value);
              }
            }
          }

          doc.addField(_DOC_SOURCE_OBJECT, content);
          documents.add(doc);
        } else {
          logger.warn("Did not index {}: Content == {}; Contact Auth == {}",
              new Object[] { path, content, contactAuth });
        }
      } catch (StorageClientException e) {
        logger.error(e.getMessage(), e);
      } catch (AccessDeniedException e) {
        logger.error(e.getMessage(), e);
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
    return ImmutableList.of("id:" + ClientUtils.escapeQueryChars(path));
  }

}
