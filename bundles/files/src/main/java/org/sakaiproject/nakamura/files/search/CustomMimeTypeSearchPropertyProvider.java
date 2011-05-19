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
 * enable searching pooled content on custom-mimetype using the mimetype request parameter
 * used by template /org.sakaiproject.nakamura.files/src/main/resources/SLING-INF/content/var/search/pool/all.json
 */
@Component(label = "CustomMimeTypeSearchPropertyProvider", description = "Property provider for searching for custom-mimtype")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "CustomMimeTypeSearchPropertyProvider") })
public class CustomMimeTypeSearchPropertyProvider implements SolrSearchPropertyProvider {

  private static final Logger LOG = LoggerFactory
      .getLogger(CustomMimeTypeSearchPropertyProvider.class);

  /**
   * {@inheritDoc}
   * add the mimetype value or empty string for the template
   * 
   */
  public void loadUserProperties(final SlingHttpServletRequest request,
      final Map<String, String> propertiesMap) {
    LOG.debug(
        "loadUserProperties(SlingHttpServletRequest request, final Map<String, String> {})",
        propertiesMap);
    String mimeType = request.getParameter("mimetype");
    if (mimeType != null) {
      propertiesMap.put("mimetype", mimeType);
    } else {
      propertiesMap.put("mimetype", "");
    }
  }
}
