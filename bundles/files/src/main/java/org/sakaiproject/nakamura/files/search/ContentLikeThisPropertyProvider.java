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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.sling.api.request.RequestParameter;

import java.util.Map;

/**
 * <pre>
 * Find content that is similar to a given content item.  Similarity is based on
 * title, tags, viewers, managers and directory location.
 * </pre>
 */
@Component(label = "ContentLikeThisPropertyProvider", description = "Property provider for content like this searches")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "ContentLikeThisPropertyProvider") })
public class ContentLikeThisPropertyProvider  implements SolrSearchPropertyProvider {

  private static final Logger LOGGER = LoggerFactory
    .getLogger(ContentLikeThisPropertyProvider.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;


  public ContentLikeThisPropertyProvider() {}


  // Just here to support the unit tests.
  public ContentLikeThisPropertyProvider(SolrSearchServiceFactory searchServiceFactory) {
    this.searchServiceFactory = searchServiceFactory;
  }


  /**
   * {@inheritDoc}
   */
  public void loadUserProperties(final SlingHttpServletRequest request,
                                 final Map<String, String> propertiesMap) {

    try {
      String user = request.getRemoteUser();

      RequestParameter contentPathParam = request.getRequestParameter("contentPath");

      String suggestedIds = null;

      if (contentPathParam != null) {
        String contentPath = contentPathParam.getString();
        String contentId = contentPath.substring (contentPath.lastIndexOf("/") + 1);

        LOGGER.debug("Suggesting content similar to '{}'", contentId);

        suggestedIds =
          SolrSearchUtil.getMoreLikeThis(request,
                                         searchServiceFactory,
                                         String.format("id:\"%s\"",
                                                       ClientUtils.escapeQueryChars(contentId)),
                                         "fl", "*,score",
                                         "rows", "10",
                                         "mlt", "true",
                                         "mlt.fl", "resourceType,manager,viewer,title,name,taguuid",
                                         "mlt.count", "10",
                                         "mlt.maxntp", "0",
                                         "mlt.mintf", "1",
                                         "mlt.mindf", "1",
                                         "mlt.boost", "true",
                                         "mlt.qf", "resourceType^100 manager^4 viewer^3 name^2 taguuid^1 title^1");

      }

      if (suggestedIds != null) {
        propertiesMap.put("_contentQuery", String.format(" AND %s", suggestedIds));
      } else {
        propertiesMap.put("_contentQuery", "");
      }

      LOGGER.debug("Query: " + propertiesMap.get("_contentQuery"));
    } catch (SolrSearchException e) {
      LOGGER.error(e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }
}
