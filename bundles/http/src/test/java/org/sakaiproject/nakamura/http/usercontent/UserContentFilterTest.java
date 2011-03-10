package org.sakaiproject.nakamura.http.usercontent;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

public class UserContentFilterTest {

  private UserContentFilter userContentFilter;
  @Mock
  private ServerProtectionService serverPotectionService;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private FilterChain chain;
  
  public UserContentFilterTest() {
   MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setup() throws ServletException {
    userContentFilter = new UserContentFilter();
    userContentFilter.serverProtectionService = serverPotectionService;
  }

  @Test
  public void testDoFilter() throws IOException, ServletException {
    Mockito.when(serverPotectionService.isRequestSafe(request, response)).thenReturn(true);
    userContentFilter.doFilter(request, response, chain);
    Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);
  }
  @Test
  public void testDoNoFilter() throws IOException, ServletException {
    Mockito.when(serverPotectionService.isRequestSafe(request, response)).thenReturn(false);
    Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer());
    userContentFilter.doFilter(request, response, chain);
    Mockito.verify(chain, Mockito.times(0)).doFilter(request, response);
  }
}
