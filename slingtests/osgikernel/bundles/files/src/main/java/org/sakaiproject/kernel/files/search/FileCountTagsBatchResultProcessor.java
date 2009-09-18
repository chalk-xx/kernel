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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.sakaiproject.kernel.util.JcrUtils;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Formats file tag count search results
 * 
 * @scr.component immediate="true" label="FileCountTagsBatchResultProcessor"
 *                description="Formatter for file tag counting"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.batchprocessor" value="FilesCountTags"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchBatchResultProcessor"
 */
public class FileCountTagsBatchResultProcessor implements SearchBatchResultProcessor{

  public void writeNodeIterator(JSONWriter write, NodeIterator nodeIterator)
      throws JSONException, RepositoryException {
    
    Map<String, Integer> map = new HashMap<String, Integer>();
            
    // Count all the tags
    int total = 0;
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      if (node.hasProperty(FilesConstants.SAKAI_TAGS)) {
        Value[] tags = JcrUtils.getValues(node, FilesConstants.SAKAI_TAGS);
        for (Value tag : tags) {
          int i = 1;
          String tagName = tag.getString();
          if (map.containsKey(tagName)) {
            i = map.get(tagName) + 1;
          }
          else {
            total++;
          }
          map.put(tagName, i);
        }
      }
    }
    
    // Output the counts
    write.object();
    write.key("tags");
    write.array();
    for (String tag : map.keySet()) {
      write.object();
      write.key("name");
      write.value(tag);
      write.key("count");
      write.value(map.get(tag));
      write.endObject();
    }
    write.endArray();
    write.key("total");
    write.value(total);
    write.endObject();
    
    
  }

}
