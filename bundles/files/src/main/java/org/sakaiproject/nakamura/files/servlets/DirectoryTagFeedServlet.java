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
package org.sakaiproject.nakamura.files.servlets;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.files.search.LiteFileSearchBatchResultProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;import java.util.Iterator;
import java.util.List;import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "DirectoryTagFeedServlet", shortDescription = "Get sample content items for all directory tags.",
    description = {
      "This servlet is able to give all the necessary information about tags.",
      "It's able to give json feeds for the childtags, parent tags or give a dump of the files who are tagged with this tag.",
      "Must specify a selector of children, parents, tagged. tidy, {number} are optional and ineffective by themselves."
    },
    bindings = {
      @ServiceBinding(type = BindingType.TYPE, bindings = { "sakai/directory" },
          extensions = @ServiceExtension(name = "json", description = "This servlet outputs JSON data."),
          selectors = {
            @ServiceSelector(name = "tagged", description = "Will dump all the children of this tag.")
          }
      )
    },
    methods = {
      @ServiceMethod(name = "GET", description = { "This servlet only responds to GET requests." },
          response = {
            @ServiceResponse(code = 200, description = "Succesful request, json can be found in the body"),
            @ServiceResponse(code = 500, description = "Failure to retrieve tags or files, an explanation can be found in the HTMl.")
          }
      )
    }
)
@SlingServlet(extensions = { "json" }, generateComponent = true, generateService = true,
    methods = { "GET" }, resourceTypes = { "sakai/directory" },
    selectors = {"children", "parents", "tagged"}
)
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for file tagging."),
    @Property(name = "service.vendor", value = "The Sakai Foundation")
})
public class DirectoryTagFeedServlet extends SlingSafeMethodsServlet {
  private static final Logger LOG = LoggerFactory.getLogger(DirectoryTagFeedServlet.class);
  private static final long serialVersionUID = -8815248520601921760L;

  @Reference
  protected transient SearchServiceFactory searchServiceFactory;

  @Reference
  protected transient SolrSearchServiceFactory solrSearchServiceFactory;
  
  @Reference
  protected transient Repository sparseRepository;
  
  @Reference
  private ProfileService profileService;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // digest the selectors to determine if we should send a tidy result
    // or if we need to traverse deeper into the tagged node.
    boolean tidy = false;
    int depth = 0;
    String[] selectors = request.getRequestPathInfo().getSelectors();
    String selector = null;
    for (String sel : selectors) {
      if ("tidy".equals(sel)) {
        tidy = true;
      } else if ("infinity".equals(sel)) {
        depth = -1;
      } else {
        // check if the selector is telling us the depth of detail to return
        Integer d = null;
        try { d = Integer.parseInt(sel); } catch (NumberFormatException e) {}
        if (d != null) {
          depth = d;
        } else {
          selector = sel;
        }
      }
    }
    
    request.setAttribute("depth", depth);

    JSONWriter write = new JSONWriter(response.getWriter());
    write.setTidy(tidy);
    Resource directoryResource = request.getResource();
    try {
      write.object();
      if (directoryResource instanceof SparseContentResource) {
        Content contentDirectory = directoryResource.adaptTo(Content.class);
      } else {
        Node directoryNode = directoryResource.adaptTo(Node.class);
        NodeIterator nodeIter = directoryNode.getNodes();
        while (nodeIter.hasNext()) {
          Node child = nodeIter.nextNode();
          write.key(child.getName());
          write.object();
          ExtendedJSONWriter.writeNodeContentsToWriter(write, child);
          write.key("content");
          writeOneTaggedItemForTagUuids(request, getTagUuidsForDirectoryBranch(child), write);
          write.endObject();
        }
      }
      write.endObject();
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
      return;
    }

  }


  private List<String> getTagUuidsForDirectoryBranch(Node branch) throws RepositoryException {
    List<String> rv = new ArrayList<String>();
    if (branch.hasProperty("sling:resourceType") && "sakai/tag".equals(branch.getProperty("sling:resourceType").getString())) {
      rv.add(branch.getIdentifier());
    }

    NodeIterator branchIterator = branch.getNodes();
    while (branchIterator.hasNext()) {
      Node branchChild = branchIterator.nextNode();
      rv.addAll(getTagUuidsForDirectoryBranch(branchChild));
    }
    return rv;
  }
  
  private void writeOneTaggedItemForTagUuids(SlingHttpServletRequest request,
      List<String> tagUuids, JSONWriter write) throws RepositoryException, SearchException, JSONException, SolrSearchException {
    // BL120 KERN-1617 Need to include Content tagged with tag uuid
    final StringBuilder sb = new StringBuilder();
    sb.append("taguuid:(");
    String sep = "";
    for (String tagUuid : tagUuids) {
      sb.append(sep + ClientUtils.escapeQueryChars(tagUuid));
      sep = " ";
    }
    sb.append(")");
    final RequestParameter typeP = request.getRequestParameter("type");
    if (typeP != null) {
      final String type = typeP.getString();
      sb.append(" AND ");
      if ("user".equals(type)) {
        sb.append("type:u");
      } else if ("group".equals(type)) {
        sb.append("type:g");
      } else {
        if ("content".equals(type)) {
          sb.append("resourceType:");
          sb.append(ClientUtils.escapeQueryChars(FilesConstants.POOLED_CONTENT_RT));
        } else {
          LOG.info("Unknown type parameter specified: type={}", type);
          write.endArray();
          return;
        }
      }
    }
    final int random = (int) (Math.random() * 10000);
    String sortRandom = "random_" + String.valueOf(random) + " asc";
    final String queryString = sb.toString();
    org.sakaiproject.nakamura.api.search.solr.Query solrQuery = new org.sakaiproject.nakamura.api.search.solr.Query(
        queryString, ImmutableMap.of("sort", sortRandom));
    final SolrSearchBatchResultProcessor rp = new LiteFileSearchBatchResultProcessor(
        solrSearchServiceFactory, profileService);
    final SolrSearchResultSet srs = rp.getSearchResultSet(request, solrQuery);
    if (srs.getResultSetIterator().hasNext()) {
      rp.writeResults(request, write, selectOneResult(srs.getResultSetIterator()));
    } else {
      // write an empty result
      write.object();
      write.endObject();
    }
  }

  private Iterator<Result> selectOneResult(Iterator<Result> resultSetIterator) {
    Result bestResult = null;
    while(resultSetIterator.hasNext()) {
      Result result = resultSetIterator.next();
      bestResult = result;
      if (isBest(result)) {
        break;
      }
    }
    
    final Result finalResult = bestResult;
    
    return new Iterator<Result>() {
      boolean hasBeenRetrieved = false;

      public boolean hasNext() {
        return !hasBeenRetrieved;
      }

      public Result next() {
        if (hasBeenRetrieved) {
          throw new NoSuchElementException();
        } else {
          hasBeenRetrieved = true;
          return finalResult;
        }
      }

      public void remove() {
        if (hasBeenRetrieved) {
          throw new NoSuchElementException();
        } else {
          hasBeenRetrieved = true;
        }
      }

    };
  }

  private boolean isBest(Result result) {
    return (result.getFirstValue("description") != null );
  }

}
