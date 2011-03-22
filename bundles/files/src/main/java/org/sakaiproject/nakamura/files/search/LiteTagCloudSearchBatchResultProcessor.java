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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

@Component(immediate = true, label = "TagCloudResultProcessor", description = "Formatter for tag cloud")
@Service(value = SolrSearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "TagCloud") })
public class LiteTagCloudSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  @Reference
  protected SlingRepository slingRepository;

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {
    final Map<String, Tag> tags = new HashMap<String, Tag>();
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    final javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(
        javax.jcr.Session.class);

    // count all the tags
    while (iterator.hasNext()) {
      try {
        final Result result = iterator.next();
        final String path = result.getPath();
        final Content node = session.getContentManager().get(path);
        if (node
            .hasProperty(org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS)) {
          // each node that has been tagged has one or more tag UUIDs riding with it
          final String[] tagUuids = (String[]) node
              .getProperty(org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS);
          for (final String uuid : tagUuids) {
            if (!tags.containsKey(uuid)) {
              tags.put(uuid, new Tag(uuid, 0));
            }
            // increment the count for this particular tag UUID.
            tags.get(uuid).frequency++;
          }
        }
      } catch (StorageClientException e) {
        // do nothing
      } catch (AccessDeniedException e) {
        // do nothing
      }
    }

    // write the most-used tags to the JSONWriter
    final List<Tag> foundTags = new ArrayList<Tag>(tags.values());
    Collections.sort(foundTags, Collections.reverseOrder());
    write.object();
    write.key(SolrSearchConstants.TOTAL);
    write.value(foundTags.size());
    write.key("tags");
    write.array();
    try {
      for (Tag tag : foundTags) {
        Node tagNode = jcrSession.getNodeByIdentifier(tag.id);
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
    } catch (ItemNotFoundException e) {
      // do nothing
    } catch (ValueFormatException e) {
      // do nothing
    } catch (PathNotFoundException e) {
      // do nothing
    } catch (RepositoryException e) {
      // do nothing
    }
    write.endArray();
    write.endObject();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.sakaiproject.nakamura.api.search.solr.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {

    return searchServiceFactory.getSearchResultSet(request, query);
  }

}

final class Tag implements Comparable<Tag> {
  public final String id;
  public String name;
  public int frequency;

  public Tag(String id) {
    this.id = id;
  }

  public Tag(String id, int frequency) {
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
