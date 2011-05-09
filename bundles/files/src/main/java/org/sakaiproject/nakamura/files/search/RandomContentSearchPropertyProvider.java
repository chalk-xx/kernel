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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <pre>
 * Create feed that lists random content. However, preference should be given 
 * to items that have a thumb-nail (page1-small.jpg), a description, tags and 
 * comments, but should still come back with different results every time you 
 * run the feed.
 * </pre>
 */
@Component(label = "RandomContentSearchPropertyProvider", description = "Property provider for random content searches")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "RandomContentSearchPropertyProvider") })
public class RandomContentSearchPropertyProvider extends
    MeManagerViewerSearchPropertyProvider implements SolrSearchPropertyProvider {

  private static final Logger LOG = LoggerFactory
      .getLogger(RandomContentSearchPropertyProvider.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.files.search.MeManagerViewerSearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(final SlingHttpServletRequest request,
      final Map<String, String> propertiesMap) {
    LOG.debug(
        "loadUserProperties(SlingHttpServletRequest request, final Map<String, String> {})",
        propertiesMap);

    // random solr sorting requires a seed for the dynamic random_* field
    final int random = (int) (Math.random() * 10000);
    propertiesMap.put("randomSeed", String.valueOf(random));
  }

}
