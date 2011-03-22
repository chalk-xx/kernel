package org.sakaiproject.nakamura.http.usercontent;

import org.apache.felix.http.api.ExtHttpService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SafeHostFilterTest {

  private SafeHostFilter safeHostFilter;
  @Mock
  private ExtHttpService extHttpService;
  @Mock
  private ServerProtectionService serverPotectionService;
  @Mock
  private ComponentContext componentContext;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain chain;
  
  public SafeHostFilterTest() {
   MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setup() throws ServletException {
    safeHostFilter = new SafeHostFilter();
    safeHostFilter.extHttpService = extHttpService;
    safeHostFilter.serverProtectionService = serverPotectionService;
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    Mockito.when(componentContext.getProperties()).thenReturn(properties);
    safeHostFilter.activate(componentContext);    
  }

  @Test
  public void testDoFilter() throws IOException, ServletException {
    Mockito.when(serverPotectionService.isMethodSafe(request, response)).thenReturn(true);
    safeHostFilter.doFilter(request, response, chain);
    Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);
  }
  @Test
  public void testDoNoFilter() throws IOException, ServletException {
    Mockito.when(serverPotectionService.isMethodSafe(request, response)).thenReturn(false);
    Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer());
    safeHostFilter.doFilter(request, response, chain);
    Mockito.verify(chain, Mockito.times(0)).doFilter(request, response);
  }
}
