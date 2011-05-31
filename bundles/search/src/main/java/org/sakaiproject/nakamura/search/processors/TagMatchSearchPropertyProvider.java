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
package org.sakaiproject.nakamura.search.processors;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
@Component
@Service
@Properties({
  @Property(name = "service.vendor", value = "The Sakai Foundation"),
  @Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "TagMatch") })
public class TagMatchSearchPropertyProvider implements SolrSearchPropertyProvider {
  private static final Logger logger = LoggerFactory
      .getLogger(TagMatchSearchPropertyProvider.class);

  @Reference
  protected transient SlingRepository repository;

  @Reference
  protected transient SearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
      javax.jcr.Session session = null;
    try {
//      javax.jcr.Session session = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
        session = repository.loginAdministrative(null);

      StringBuilder tagClause = new StringBuilder();
      String q = request.getParameter("q");
      if (!StringUtils.isBlank(q)) {
        if (q.endsWith("*")) {
          q = q.substring(0, q.length()-1);
        }

        String statement = "//element(*)MetaData[@sling:resourceType='sakai/tag'";
        // KERN-1917, KERN-1918
        if (!StringUtils.isBlank(q)) {
          tagClause.append(" OR tag:(").append(ClientUtils.escapeQueryChars(q)).append(")");
          statement += " and jcr:like(@sakai:tag-name,'%" + ISO9075.encode(q) + "%')";
        }
        statement += "]";

        QueryResult result = JcrResourceUtil.query(session, statement, "xpath");
        RowIterator rows = result.getRows();
  
        if (rows.hasNext()) {
          tagClause.append(" OR taguuid:(");
          String sep = "";
          while(rows.hasNext()) {
            tagClause.append(sep);
            Row row = rows.nextRow();
            String uuid = row.getNode().getIdentifier();
            tagClause.append(ClientUtils.escapeQueryChars(uuid));
            sep = " OR ";
          }
          tagClause.append(")");
        }
      }

      propertiesMap.put("_taguuids", tagClause.toString());
    } catch (RepositoryException e) {
      logger.error("failed to add search properties for tags", e);
    } finally {
       if (session != null) {
         session.logout();
       }
    }
  }
}
