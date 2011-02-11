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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.StringWriter;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PagecontentSearchResultProcessorTest {
  @Mock
  SlingHttpServletRequest request;

  @Mock
  ResourceResolver resolver;

  @Mock
  ContentManager cm;

  @Mock
  Session session;

  @Mock
  Result result;

  @Mock
  Resource parentResource;

  @Mock
  RequestPathInfo pathInfo;

  @Mock
  SolrSearchServiceFactory fact;

  @Test
  public void test() throws Exception {
    when(request.getResourceResolver()).thenReturn(resolver);
    Object hybridSession = mock(javax.jcr.Session.class,
        withSettings().extraInterfaces(SessionAdaptable.class));
    when(resolver.adaptTo(javax.jcr.Session.class)).thenReturn(
        (javax.jcr.Session) hybridSession);
    when(((SessionAdaptable) hybridSession).getSession()).thenReturn(session);
    when(result.getPath()).thenReturn("/test/path");

    when(session.getContentManager()).thenReturn(cm);

    final Content content = new Content("/test/path", null);
    final Content parentContent = new Content("/test", null);
    parentContent.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "sakai/page");
    when(parentResource.adaptTo(Content.class)).thenReturn(parentContent);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(cm.get("/test")).thenReturn(parentContent);

    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    StringWriter stringWriter = new StringWriter();
    JSONWriter write = new JSONWriter(stringWriter);

    SolrSearchResultProcessor proc = new SolrSearchResultProcessor() {
      public void writeResult(SlingHttpServletRequest request, JSONWriter write,
          Result result) throws JSONException {
        int maxTraversalDepth = SearchUtil.getTraversalDepth(request);
        ExtendedJSONWriter.writeContentTreeToWriter(write, content, maxTraversalDepth);
      }

      public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
          Query query) throws SolrSearchException {
        return null;
      }
    };

    PagecontentSearchResultProcessor pagecontentSearchResultProcessor = new PagecontentSearchResultProcessor(fact, proc);
    pagecontentSearchResultProcessor.writeResult(request, write, result);

    String output = stringWriter.toString();
    Assert.assertTrue(output.length() > 0);
    // Make sure that the output is valid JSON.
    new JSONObject(output);
  }
}
