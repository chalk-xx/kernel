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
package org.sakaiproject.nakamura.activity.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;

import java.util.Date;
import java.util.Map;

@Component(label = "MostActiveContentPropertyProvider", description = "Property provider for most active content")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "MostActiveContentPropertyProvider") })
public class MostActiveContentPropertyProvider implements SolrSearchPropertyProvider {

  public static final int DEFAULT_DAYS = 30;
  public static final long DEFAULT_DAYS_MS = DEFAULT_DAYS * 24 * 60 * 60 * 1000L;
  public static final int MAXIMUM_DAYS = 90;
  public static final long MAXIMUM_DAYS_MS = MAXIMUM_DAYS * 24 * 60 * 60 * 1000L;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    final String then = String.valueOf(deriveThen(request));
    propertiesMap.put("then", then);
  }

  protected long deriveThen(final SlingHttpServletRequest request) {
    final RequestParameter thenParam = request.getRequestParameter("then");
    final long now = new Date().getTime();
    long then = now - DEFAULT_DAYS_MS;
    if (thenParam != null) {
      final long when = Long.valueOf(thenParam.getString());
      then = (when > (now - MAXIMUM_DAYS_MS)) ? when : then;
    }
    return then;
  }

}
