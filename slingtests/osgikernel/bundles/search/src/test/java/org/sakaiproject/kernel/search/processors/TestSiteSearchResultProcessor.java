package org.sakaiproject.kernel.search.processors;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sling.commons.json.JSONException;
import org.junit.Test;
import org.sakaiproject.kernel.api.site.SiteService;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class TestSiteSearchResultProcessor extends AbstractSearchResultProcessorTest {

  @Test
  public void testResultCountLimit() throws RepositoryException, JSONException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    expect(siteService.isSite(isA(Item.class))).andReturn(true).anyTimes();
    expect(siteService.getMemberCount(isA(Node.class))).andReturn(20).anyTimes();
    simpleResultCountCheck(siteSearchResultProcessor);
  }

  @Test
  public void testNonSiteNode() throws RepositoryException, JSONException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    expect(siteService.isSite(isA(Item.class))).andReturn(false);
    Node resultNode = createMock(Node.class);
    expect(resultNode.getPath()).andReturn("");
    replay();
    try {
      siteSearchResultProcessor.writeNode(null, resultNode);
      fail();
    } catch (JSONException e) {
      assertEquals("Unable to write non-site node result", e.getMessage());
    }
  }

}
