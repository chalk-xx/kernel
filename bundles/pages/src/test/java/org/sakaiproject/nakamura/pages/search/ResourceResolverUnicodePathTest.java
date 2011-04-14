package org.sakaiproject.nakamura.pages.search;

import junit.framework.Assert;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverUnicodePathTest {
  static final String utf8Path = "/utf-βετα-5/pages/_pages/";
  static final String ISO88591Path = "/~utf-Î²ÎµÏÎ±-5/pages/_pages/";
  
  @Mock
  SlingHttpServletRequest request;
  
  @Mock
  RequestParameter requestParameter;

  @Mock
  ResourceResolver resourceResolver;

  @Mock
  Resource pagesResource;
  
  @Test
  /**
   * see KERN-1759
   */
  public void test() throws Exception {
    when(request.getRequestParameter("path")).thenReturn(requestParameter);
    when(requestParameter.getString("UTF-8")).thenReturn(utf8Path);
    when(requestParameter.getString()).thenReturn(ISO88591Path);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.getResource(utf8Path)).thenReturn(pagesResource);
    when(resourceResolver.getResource(ISO88591Path)).thenReturn(null);
    
    requestParameter = request.getRequestParameter("path");
    resourceResolver = request.getResourceResolver();
    String goodPath = requestParameter.getString("UTF-8");
    String badPath = requestParameter.getString();
    
    pagesResource = resourceResolver.getResource(goodPath);
    Assert.assertNotNull(pagesResource);
    
    pagesResource = resourceResolver.getResource(badPath);
    Assert.assertNull(pagesResource);
  }
}
