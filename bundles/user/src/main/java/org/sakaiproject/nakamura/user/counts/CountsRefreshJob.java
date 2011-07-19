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
package org.sakaiproject.nakamura.user.counts;

import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountsRefreshJob implements Job {
  private static final Logger LOGGER = LoggerFactory.getLogger(CountsRefreshJob.class);

  Repository sparseRepository;
  SolrServerService solrServerService;
  CountProvider countProvider;

  public CountsRefreshJob(Repository sparseRepository,
      SolrServerService solrServerService, CountProvider countProvider) {
    this.sparseRepository = sparseRepository;
    this.solrServerService = solrServerService;
    this.countProvider = countProvider;
  }

  /**
   * update the batch size number of Authorizables whose countLastUpdate is null (never
   * updated) or whose countLastUpdate is more than the update interval minutes ago
   * {@inheritDoc}
   * 
   * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
   */
  public void execute(JobContext context) {
    Session adminSession = null;
    Integer batchSize = (Integer) context.getConfiguration().get(
        CountsRefreshScheduler.PROP_UPDATE_BATCH_SIZE);
    try {
      adminSession = this.sparseRepository.loginAdministrative();
      AuthorizableManager authManager = adminSession.getAuthorizableManager();
      SolrServer solrServer = solrServerService.getServer();
      long nowTicks = System.currentTimeMillis();
      long updateIntervalTicks = this.countProvider.getUpdateIntervalMinutes() * 60 * 1000;
      long updateTicks = nowTicks - updateIntervalTicks;
      // find all the authorizables have not been updated in the update interval
      // or who have never been updated
      StringBuilder querySB = new StringBuilder("+resourceType:authorizable AND -countLastUpdate:[")
                              .append(updateTicks).append(" TO * ]");
      String queryString = querySB.toString();
      SolrQuery solrQuery = new SolrQuery(queryString).setStart(0).setRows(batchSize);
      QueryResponse response;
      try {
        response = solrServer.query(solrQuery);
        SolrDocumentList results = response.getResults();
        long numResults = results.getNumFound();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("with query {}, found {} results", new Object[] { queryString,
            numResults });
        if (numResults > 0) {
          batchSize = (int) (batchSize < numResults ? batchSize : numResults);
          LOGGER.info("will update counts on max of {} authorizables",
              new Object[] { batchSize });
          long startTicks = System.currentTimeMillis();
          int count = 0;
          for (SolrDocument solrDocument : results) {
            String authorizableId = (String) solrDocument.getFieldValue("id");
            Authorizable authorizable = authManager.findAuthorizable(authorizableId);
            if (authorizable != null) {
              if (authorizable.getId() != null) {
                this.countProvider.update(authorizable, adminSession);
                count++;              
              } else {
                LOGGER.debug(
                    "found authorizable with id {} in Solr index but with NULL id in Sparse, not updating", 
                    new Object[]{ authorizableId });
              }
            } else {
              LOGGER.debug(
                      "found authorizable with id {} in Solr index but couldn't find authorizable in Sparse, not updating",
                      new Object[] { authorizableId });
            }
          }
          long endTicks = System.currentTimeMillis();
          LOGGER.info("updated {} authorizables in {} seconds", new Object[] { count,
              (endTicks - startTicks) / 1000 });
        } else {
          LOGGER.info("All authorizables have up to date counts");
        }
      } catch (SolrServerException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (adminSession != null)
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
    }
  }
}
