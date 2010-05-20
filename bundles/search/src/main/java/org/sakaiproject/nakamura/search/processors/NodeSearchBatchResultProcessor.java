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

import static org.sakaiproject.nakamura.api.search.SearchConstants.DEFAULT_PAGED_ITEMS;
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_ITEMS_PER_PAGE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@Component(immediate = true, label = "NodeSearchBatchResultProcessor", description = "Formatter for batch search results.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "Node") })
@Service(value = SearchBatchResultProcessor.class)
public class NodeSearchBatchResultProcessor implements
    SearchBatchResultProcessor {

  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {

    Session session = request.getResourceResolver().adaptTo(Session.class);

    // TODO Get size from somewhere else.
    long total = iterator.getSize();
    long start = SearchUtil.getPaging(request, total);
    
    long nitems = SearchUtil.intRequestParameter(request,
        PARAMS_ITEMS_PER_PAGE, DEFAULT_PAGED_ITEMS);

    iterator.skip(start);

    for (long i = 0; i < nitems && iterator.hasNext(); i++) {
      Row row = iterator.nextRow();
      String path = row.getValue("jcr:path").getString();
      Node node = (Node) session.getItem(path);
      if (aggregator != null) {
        aggregator.add(node);
      }
      ExtendedJSONWriter.writeNodeToWriter(write, node);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return SearchUtil.getSearchResultSet(request, query);
  }

}
