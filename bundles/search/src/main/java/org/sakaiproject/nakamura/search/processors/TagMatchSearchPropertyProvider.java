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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
        String statement = "//element(*)MetaData[@sling:resourceType='sakai/tag' and jcr:like(@sakai:tag-name,'%" + ISO9075.encode(q) + "%')]";
        QueryResult result = JcrResourceUtil.query(session, statement, "xpath");
        final String[] colNames = result.getColumnNames();
        final RowIterator rows = result.getRows();
        Iterator<Map<String,Object>> resultIter = new Iterator<Map<String, Object>>() {
            public boolean hasNext() {
                return rows.hasNext();
            };

            public Map<String, Object> next() {
                Map<String, Object> result = new HashMap<String, Object>();
                try {
                  Row row = rows.nextRow();
                  result.put("uuid", row.getNode().getIdentifier());
                  Value[] values = row.getValues();
                  for (int i = 0; i < values.length; i++) {
                    Value v = values[i];
                    if (v != null) {
                      result.put(colNames[i],
                          JcrResourceUtil.toJavaObject(values[i]));
                        }
                    }
                } catch (RepositoryException re) {
                    logger.error(
                        "queryResources$next: Problem accessing row values",
                        re);
                }
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
  
        if (resultIter.hasNext()) {
          tagClause.append(" OR taguuid:(");
          String sep = "";
          while(resultIter.hasNext()) {
            tagClause.append(sep);
            Map<String, Object> row = resultIter.next();
            String uuid = (String) row.get("uuid");
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
