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
package org.sakaiproject.nakamura.chat;

import org.apache.commons.lang.time.DateUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

@Component(label = "MessageSearchPropertyProvider", description = "Provides properties to process the chat message searches.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "ChatMessage"),
    @Property(name = "service.description", value = "Provides properties to process the chat message searches.") })
public class ChatMessageSearchPropertyProvider implements SolrSearchPropertyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ChatMessageSearchPropertyProvider.class);

  @Reference
  protected transient LiteMessagingService messagingService;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      final String user = request.getRemoteUser();
      final Session session = StorageClientUtils.adaptToSession(request
          .getResourceResolver().adaptTo(javax.jcr.Session.class));
      final String fullPathToStore = ClientUtils.escapeQueryChars(messagingService
          .getFullPathToStore(user, session));
      propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, fullPathToStore + "*");

      final RequestParameter usersParam = request.getRequestParameter("_from");
      if (usersParam != null && !usersParam.getString().equals("")) {
        final StringBuilder solr = new StringBuilder(" AND (");
        final String[] users = StringUtils.split(usersParam.getString(), ',');

        solr.append("from:(");
        for (final String u : users) {
          if("*".equals(u)) continue;
          solr.append(ClientUtils.escapeQueryChars(u)).append(" OR ");
          // sql.append("@sakai:from=\"").append(escapeString(u, Query.XPATH))
          // .append("\" or ");
        }
        solr.append(ClientUtils.escapeQueryChars(user));
        // sql.append("@sakai:from=\"").append(escapeString(user, Query.XPATH))
        // .append("\") or (");
        solr.append(")"); // close from:

        solr.append(" AND to:(");
        for (final String u : users) {
          if("*".equals(u)) continue;
          solr.append(ClientUtils.escapeQueryChars(u)).append(" OR ");
          // sql.append("@sakai:to=\"").append(escapeString(u, Query.XPATH))
          // .append("\" or ");
        }
        solr.append(ClientUtils.escapeQueryChars(user));
        // sql.append("@sakai:to=\"").append(escapeString(user,
        // Query.XPATH)).append("\"))");
        solr.append(")"); // close to:

        solr.append(")"); // close AND
        propertiesMap.put("_from", solr.toString());
      }
      
      // convert iso8601jcr to Long for solr query parser
      final RequestParameter t = request.getRequestParameter("t");
      if (t != null && !"".equals(t.getString())) {
        final Date date = DateUtils.parseDate(t.getString(),
            new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSSZZ" });
        propertiesMap.put("t", String.valueOf(date.getTime()));
      }
    } catch (MessagingException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (ParseException e) {
      LOG.warn(e.getLocalizedMessage(), e);
    }
  }
}
