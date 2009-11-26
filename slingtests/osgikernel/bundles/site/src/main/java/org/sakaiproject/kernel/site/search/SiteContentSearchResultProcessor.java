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
package org.sakaiproject.kernel.site.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component(immediate = true, name = "SiteContentSearchResultProcessor", label = "SiteContentSearchResultProcessor")
@Properties(value = { 
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Formats search results for content nodes in sites."),
    @Property(name = "sakai.search.processor", value = "Sitecontent")
})
@Service(value = SearchResultProcessor.class)
public class SiteContentSearchResultProcessor implements SearchResultProcessor {

  private SearchResultProcessor defaultProcessor = new SearchResultProcessor() {

    public void writeNode(JSONWriter write, Node node) throws JSONException,
        RepositoryException {
      write.object();
      write.key("path");
      write.value(node.getPath());
      ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
      write.endObject();
    }
  };
  private SearchResultProcessorTracker tracker;

  public void writeNode(JSONWriter write, Node node) throws JSONException,
      RepositoryException {
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      String type = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();

      // From looking at the type we determine how we should represent this node.
      SearchResultProcessor processor = tracker.getSearchResultProcessorByType(type);
      if (type == null) {
        processor = defaultProcessor;
      }
      processor.writeNode(write, node);

    } else {
      // No type, just dump the properties
      defaultProcessor.writeNode(write, node);
    }
  }

  public void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    tracker = new SearchResultProcessorTracker(bundleContext);
    tracker.open();
  }

  public void deactivate() {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

}
