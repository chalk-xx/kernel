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

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;

import java.util.Map;
import java.util.Set;

/**
 * Adds tag and category terms as a search single search term. The resulting term can be
 * influenced by setting the optional "type" request parameter. By default, it is assumed
 * that all known types will be returned.
 * <p>
 * Expected types are (any combination; comma separated):
 * <table>
 * <tr>
 * <th>
 * <td>url param</td>
 * <td>description</td>
 * </th>
 * </tr>
 * <tr>
 * <td>u</td><td>user</td>
 * <td>g</td><td>group</td>
 * </tr>c</td><td>content</td>
 * </ul>
 */
@Component
@Service
@Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "ByTag")
public class ByTagSearchPropertyProvider implements SolrSearchPropertyProvider {
  protected static final String PROP_RESOURCE_TYPE = "_resourceType";
  protected static final String PROP_TYPE = "_type";
  protected static final String DEFAULT_FILTER_QUERY = "(authorizable OR sakai/pooled-content)";

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    // see what types have been requested
    String rpType = request.getParameter("type");

    // set the defaults
    String resourceType = DEFAULT_FILTER_QUERY;
    String type = "";

    if (rpType != null && !StringUtils.isBlank(rpType)) {
      // at least one type was specified. split on comma to check for multiples
      Set<String> types = Sets.newHashSet(StringUtils.split(rpType, ','));
      if (types.contains("u") || types.contains("g")) {
        if (!types.contains("c")) {
          // found user or group but not content
          resourceType = "authorizable";
        }
        // filter futher if only one type of authorizable was requested. the default is
        // to not include a type filter which will return all authorizables
        if (rpType.contains("u") && !rpType.contains("g")) {
          type = " AND type:u";
        } else if (!rpType.contains("u") && rpType.contains("g")) {
          type = " AND type:g";
        }
      } else if (types.contains("c")) {
        resourceType = "sakai/pooled-content";
      }
    }

    // add the props to the map for consumption by the query template
    propertiesMap.put(PROP_RESOURCE_TYPE, resourceType);
    propertiesMap.put(PROP_TYPE, type);
  }
}
