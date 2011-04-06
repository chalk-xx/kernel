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

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.RowUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

//@Component(immediate = true, label = "TagCloudResultProcessor", description = "Formatter for tag cloud")
//@Service(value = SearchBatchResultProcessor.class)
//@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
//    @Property(name = "sakai.search.batchprocessor", value = "TagCloud") })
public class TagCloudSearchBatchResultProcessor implements SearchBatchResultProcessor {

  @Reference
  private SearchServiceFactory searchServiceFactory;

  @Reference
  protected SlingRepository slingRepository;

  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {
    List<Tag> tags = new ArrayList<Tag>();
    Session session = request.getResourceResolver().adaptTo(Session.class);

    // count all the tags
    while (iterator.hasNext()) {
      Row row = iterator.nextRow();
      Node node = RowUtils.getNode(row, session);
      if (node
          .hasProperty(org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS)) {
        // each node that has been tagged has one or more tag UUIDs riding with it
        Value[] tagUuids = JcrUtils.getValues(node,
            org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS);
        for (Value uuidValue : tagUuids) {
          String uuid = uuidValue.getString();
          if (!tags.contains(new Tag(uuid))) {
            tags.add(new Tag(uuid, 0));
          }
          // increment the count for this particular tag UUID.
          tags.get(tags.indexOf(new Tag(uuid))).frequency++;
        }
      }
    }

    // write the most-used tags to the JSONWriter
    Collections.sort(tags, Collections.reverseOrder());
    write.object();
    write.key("tags");
    write.array();
    for (Tag tag : tags) {
      Node tagNode = session.getNodeByIdentifier(tag.id);
      if (tagNode != null
          && tagNode
              .hasProperty(org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_NAME)) {
        tag.name = tagNode.getProperty(
            org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_NAME)
            .getString();
        write.object();
        write.key("name");
        write.value(tag.name);
        write.key("count");
        write.value(Long.valueOf(tag.frequency));
        write.endObject();
      }
    }
    write.endArray();
    write.endObject();
  }

  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    try {
      // Perform the query
      QueryResult qr = query.execute();
      RowIterator iterator = qr.getRows();

      // Return the result set.
      return searchServiceFactory.getSearchResultSet(iterator);
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to execute query.");
    }
  }

}

class JcrTag implements Comparable<Tag> {
  public String id;
  public String name;
  public int frequency;

  public JcrTag(String id) {
    this.id = id;
  }

  public JcrTag(String id, int frequency) {
    this.id = id;
    this.frequency = frequency;
  }

  public int compareTo(Tag other) {
    return Integer.valueOf(this.frequency).compareTo(Integer.valueOf(other.frequency));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Tag))
      return false;
    Tag other = (Tag) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return name + " (" + id + "):" + frequency;
  }
}
