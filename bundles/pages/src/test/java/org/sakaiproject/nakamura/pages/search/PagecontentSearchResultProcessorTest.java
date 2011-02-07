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
package org.sakaiproject.nakamura.pages.search;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.StringWriter;

import javax.jcr.RepositoryException;

/**
 *
 */
public class PagecontentSearchResultProcessorTest extends AbstractEasyMockTest {


  @Test
  public void test() throws SearchException, JSONException, RepositoryException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Result result = createNiceMock(Result.class);
    EasyMock.expect(result.getPath()).andReturn("/test/path");
    final Content content = new Content("/test/path", null);
    Resource parentResource = createNiceMock(Resource.class);
    final Content parentContent = new Content("/test", null);
    parentContent.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "sakai/page");
    EasyMock.expect(parentResource.adaptTo(Content.class)).andReturn(parentContent);
    EasyMock.expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    EasyMock.expect(resourceResolver.getResource("/test")).andReturn(parentResource);

    RequestPathInfo pathInfo = createNiceMock(RequestPathInfo.class);
    EasyMock.expect(request.getRequestPathInfo()).andReturn(pathInfo);

    StringWriter stringWriter = new StringWriter();
    JSONWriter write = new JSONWriter(stringWriter);

    SolrSearchServiceFactory fact = createNiceMock(SolrSearchServiceFactory.class);
    SolrSearchResultProcessor proc = new SolrSearchResultProcessor() {
      public void writeResult(SlingHttpServletRequest request, JSONWriter write,
          Result result) throws JSONException {
        int maxTraversalDepth = SearchUtil.getTraversalDepth(request);
        ExtendedJSONWriter.writeContentTreeToWriter(write, content, maxTraversalDepth);
      }

      public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
          String query) throws SolrSearchException {
        return null;
      }
    };

    replay();

    PagecontentSearchResultProcessor pagecontentSearchResultProcessor = new PagecontentSearchResultProcessor(fact, proc);
    pagecontentSearchResultProcessor.writeResult(request, write, result);

    String output = stringWriter.toString();
    Assert.assertTrue(output.length() > 0);
    // Make sure that the output is valid JSON.
    new JSONObject(output);

    verify();
  }
}
