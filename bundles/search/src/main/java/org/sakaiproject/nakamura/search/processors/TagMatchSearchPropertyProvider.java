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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
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
    try {
      javax.jcr.Session session = request.getResourceResolver().adaptTo(javax.jcr.Session.class);

      if (request.getParameter("q") == null) {
        throw new IllegalArgumentException(
            "Must provide 'q' parameter to use for search.");
      }
      String q = request.getParameter("q");
      if (q.endsWith("*")) {
        q = q.substring(0, q.length()-1);
      }
      String statement = "//element(*)MetaData[@sling:resourceType='sakai/tag' and jcr:like(@sakai:tag-name,'%"+q+"%')]";
      QueryManager qm = session.getWorkspace().getQueryManager();
      @SuppressWarnings("deprecation")
      Query query = qm.createQuery(statement, Query.XPATH);
      QueryResult qr = query.execute();
      RowIterator rows = qr.getRows();

      StringBuffer tagClause = new StringBuffer();
      if (rows.getSize() > 0) {
        tagClause.append(" OR taguuid:(");
        String sep = "";
        while(rows.hasNext()) {
          tagClause.append(sep);
          Row row = rows.nextRow();
          Node tagNode = row.getNode();
          tagClause.append(ClientUtils.escapeQueryChars(tagNode.getIdentifier()));
          sep = " OR ";
        }
        tagClause.append(")");
      }

      propertiesMap.put("_taguuids", tagClause.toString());
    } catch (RepositoryException e) {
      logger.error("failed to add search properties for tags", e);
    }
  }
}
