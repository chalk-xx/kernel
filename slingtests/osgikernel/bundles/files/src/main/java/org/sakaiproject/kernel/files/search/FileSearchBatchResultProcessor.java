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

import org.apache.jackrabbit.JcrConstants;
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

/**
 * Formats the files search results.
 * 
 * @scr.component immediate="true" label="FileSearchBatchResultProcessor"
 *                description="Formatter for file searches"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.batchprocessor" value="Files"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchBatchResultProcessor"
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 */
public class FileSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(FileSearchBatchResultProcessor.class);

  private SiteService siteService;

  public void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

  public void writeNodeIterator(JSONWriter write, NodeIterator nodeIterator, long start,
      long end) throws JSONException, RepositoryException {

    List<String> processedNodes = new ArrayList<String>();
    for (long i = start; i < end && nodeIterator.hasNext(); i++) {
      Node node = nodeIterator.nextNode();
      // Every other file..
      if (node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(
          JcrConstants.NT_RESOURCE)) {
        node = node.getParent();
      }

      // We hide the .files
      String name = node.getName();
      if (name.startsWith(".")) {
        i--;
        continue;
      }

      // Check that we didn't handle this file already.
      String path = node.getPath();
      if (!processedNodes.contains(path)) {
        processedNodes.add(path);

        Session session = node.getSession();
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
}
