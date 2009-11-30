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
package org.sakaiproject.kernel.files.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Formats the files search results.
 * 
 */

@Component(immediate = true, label = "FileSearchBatchResultProcessor", description = "Formatter for file searches")
@Service(value = SearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "Files") })
public class FileSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(FileSearchBatchResultProcessor.class);

  @Reference
  private SiteService siteService;

  private List<String> processedNodes = new ArrayList<String>();

  /**
   * @param siteService
   */
  public FileSearchBatchResultProcessor(SiteService siteService) {
    this.siteService = siteService;
  }

  public FileSearchBatchResultProcessor() {
  }

  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      RowIterator iterator, long start, long end) throws JSONException,
      RepositoryException {
    processedNodes = new ArrayList<String>();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    iterator.skip(start);
    for (long i = start; i < end && iterator.hasNext(); i++) {
      Row row = iterator.nextRow();
      String path = row.getValue("jcr:path").getString();
      Node node = (Node) session.getItem(path);

      if (!handleNode(node, path, session, write)) {
        i--;
      }
    }
  }

  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      NodeIterator iterator, int start, long end) throws RepositoryException,
      JSONException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    iterator.skip(start);
    for (long i = start; i < end && iterator.hasNext(); i++) {
      Node node = iterator.nextNode();

      if (!handleNode(node, node.getPath(), session, write)) {
        i--;
      }
    }
  }

  private void writeNormalFile(JSONWriter write, Node node) throws JSONException,
      RepositoryException {
    write.object();

    // dump all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    write.key("path");
    write.value(node.getPath());
    write.key("name");
    write.value(node.getName());
    if (node.hasNode("jcr:content")) {
      Node contentNode = node.getNode("jcr:content");
      write.key(FilesConstants.SAKAI_MIMETYPE);
      write.value(contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString());
      write.key(JcrConstants.JCR_LASTMODIFIED);
      Calendar cal = contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
      write.value(FilesConstants.DATEFORMAT.format(cal));
    }
    write.endObject();
  }

  private boolean handleNode(Node node, String path, Session session, JSONWriter write)
      throws RepositoryException, JSONException {
    // Every other file..
    if (node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(
        JcrConstants.NT_RESOURCE)) {
      node = node.getParent();
    }

    // We hide the .files
    String name = node.getName();
    if (name.startsWith(".")) {
      return false;
    }

    // Check that we didn't handle this file already.
    if (!processedNodes.contains(path)) {
      processedNodes.add(path);

      String type = "";
      if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
        type = node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            .getString();
      }

      // If it is a file node we provide some extra properties.
      if (FilesConstants.RT_SAKAI_FILE.equals(type)) {
        FileUtils.writeFileNode(node, session, write, siteService);
      } else if (FilesConstants.RT_SAKAI_LINK.equals(type)) {
        // This is a linked file.
        FileUtils.writeLinkNode(node, session, write, siteService);
      }
      // Every other file..
      else {
        writeNormalFile(write, node);
      }
    }
    return true;
  }
}
