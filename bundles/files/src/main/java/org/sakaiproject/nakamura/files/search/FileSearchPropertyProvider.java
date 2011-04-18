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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides properties to process the search
 *
 */
@Component(label = "FileSearchPropertyProvider", description = "Property provider for file searches")
@Service
@Properties({
  @Property(name = "service.vendor", value = "The Sakai Foundation"),
  @Property(name = "sakai.search.provider", value = "Files") })
public class FileSearchPropertyProvider implements SolrSearchPropertyProvider {

  @Reference
  protected ConnectionManager connectionManager;

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    String user = request.getRemoteUser();

    // Set the userid.
    propertiesMap.put("_me", ClientUtils.escapeQueryChars(user));

    // Set the public space.
    propertiesMap.put("_mySpace",
        ClientUtils.escapeQueryChars(LitePersonalUtils.getPublicPath(user)));

    // Set the contacts.
    propertiesMap.put("_mycontacts", getMyContacts(request, user));

    // Filter by links.
    String usedinClause = doUsedIn(request);
    String tags = doTags(request);
    String tagsAndUsedIn = "";
    if (tags.length() > 0) {
      propertiesMap.put("_usedin", " AND (" + tags + ")");
      tagsAndUsedIn = tags;
    }

    if (usedinClause.length() > 0) {
      propertiesMap.put("_usedin", " AND (" + usedinClause + ")");
      tagsAndUsedIn = "(" + tagsAndUsedIn + ") AND (" + usedinClause + ")";

    }
    propertiesMap.put("_tags_and_usedin", tagsAndUsedIn);


  }

  /**
   * Filter files by looking up where they are used.
   *
   * @param request
   * @return
   */
  protected String doUsedIn(SlingHttpServletRequest request) {
    String usedin[] = request.getParameterValues("usedin");
    StringBuilder sb = new StringBuilder();

    if (usedin != null && usedin.length > 0) {
      sb.append("linkpaths:(");
      for (int i = 0; i < usedin.length; i++) {
        sb.append("\"").append(usedin[i]).append("\"");

        if (i < usedin.length - 1) {
          sb.append(" OR ");
        }
      }
      sb.append(")");
    }

    return sb.toString();
  }

  /**
   * Gets a clause for a query by looking at the sakai:tags request parameter.
   *
   * @param request
   * @return
   */
  protected String doTags(SlingHttpServletRequest request) {
    String[] tags = request.getParameterValues(FilesConstants.SAKAI_TAGS);
    StringBuilder tag = new StringBuilder();

    if (tags != null) {
      tag.append("(tag:(");
      StringBuilder ngram = new StringBuilder("ngram:(");
      StringBuilder edgeNgram = new StringBuilder("edgengram:(");

      for (int i = 0; i < tags.length; i++) {
        String term = ClientUtils.escapeQueryChars(tags[i]);
        tag.append("\"").append(term).append("\"");
        ngram.append("\"").append(term).append("\"");
        edgeNgram.append("\"").append(term).append("\"");

        if (i < tags.length - 1) {
          tag.append(" AND ");
          ngram.append(" AND ");
          edgeNgram.append(" AND ");
        }
      }
      tag.append(") OR ").append(ngram).append(") OR ").append(edgeNgram).append("))");
    }
    return tag.toString();
  }

  /**
   * Get a string of all the connected users.
   * @param request 
   *
   * @param user
   *          The user to get the contacts for.
   * @return "AND (createdBy:(\"simon\" OR \"ieb\")"
   */
  @SuppressWarnings(justification = "connectionManager is OSGi managed", value = {
      "NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD" })
  protected String getMyContacts(SlingHttpServletRequest request, String user) {
    List<String> connectedUsers = connectionManager.getConnectedUsers(request, user,
        ConnectionState.ACCEPTED);
    StringBuilder sb = new StringBuilder();

    if (connectedUsers.size() > 0) {
      sb.append("AND _createdBy:(");
      Iterator<String> users = connectedUsers.iterator();
      while (users.hasNext()) {
        String u = users.next();
        sb.append("\"").append(ClientUtils.escapeQueryChars(u)).append("\"");

        if (users.hasNext()) {
          sb.append(" OR ");
        }
      }
      sb.append(")");
    }

    return sb.toString();
  }

}
