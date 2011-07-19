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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;


@Component(label = "Nakamura :: CountRefreshScheduler",
    description = "Runs scheduled jobs that refresh the authorizables' counts in batches",
    immediate = true, metatype = true)
@Service(value = CountsRefreshScheduler.class)

/**
 * run a periodic job (every poll interval seconds) that will update the batch size of authorizables' counts
 */
public class CountsRefreshScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CountsRefreshScheduler.class);

  @Reference
  protected Repository sparseRepository;

  @Reference
  protected Scheduler scheduler;

  @Reference
  protected SolrServerService solrServerService;
  
  @Reference
  protected CountProvider countProvider;
  
  @Property(longValue = 300, label = "Refresh Interval Seconds",
          description = "How often to wake up and update a batch of authorizables")
  protected static final String PROP_POLL_INTERVAL_SECONDS = "refreshcounts.pollinterval";
  
  @Property(intValue = 500, label = "Maximum Batch Size of Authorizables to Update in one Job",
      description = "Maximum Number of Authorizables to Update in one Job")
  public static final String PROP_UPDATE_BATCH_SIZE = "refreshcounts.batchsize";  

  protected final static String JOB_NAME = "refreshCountsJob";
  
  protected void activate(ComponentContext componentContext) throws Exception {
    Dictionary<?, ?> props = componentContext.getProperties();
    Long pollInterval = (Long) props.get(PROP_POLL_INTERVAL_SECONDS);
    Integer batchSize = (Integer) props.get(PROP_UPDATE_BATCH_SIZE);
    Map<String, Serializable> config = new HashMap<String, Serializable>();
    config.put(PROP_UPDATE_BATCH_SIZE, batchSize);
    final Job countsRefreshJob = new CountsRefreshJob(this.sparseRepository, this.solrServerService, this.countProvider);
    try {
      LOGGER.debug("Activating CountsRefreshJob...");
      this.scheduler.addPeriodicJob(JOB_NAME, countsRefreshJob, config, pollInterval, false);
    } catch (Exception e) {
      LOGGER.error("Failed to add periodic job for CountsRefreshScheduler", e);
    }
  }

  @SuppressWarnings({"UnusedParameters"})
  protected void deactivate(ComponentContext componentContext) throws Exception {
    LOGGER.debug("Removing refreshCountsJob...");
    this.scheduler.removeJob(JOB_NAME);
  }
}
