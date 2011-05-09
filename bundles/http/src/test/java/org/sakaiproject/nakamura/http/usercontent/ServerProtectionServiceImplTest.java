package org.sakaiproject.nakamura.http.usercontent;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

public class ServerProtectionServiceImplTest {

  private ServerProtectionServiceImpl serverProtectionService;
  @Mock
  private SlingHttpServletRequest hrequest;
  @Mock
  private SlingHttpServletResponse hresponse;
  @Mock
  private ComponentContext componentContext;
  @Mock
  private BundleContext bundleContext;
  
  public ServerProtectionServiceImplTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void setup() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidSyntaxException {
    serverProtectionService = new ServerProtectionServiceImpl();
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    Mockito.when(componentContext.getProperties()).thenReturn(properties);
    Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
    serverProtectionService.activate(componentContext);
  }
  
  @Test
  public void testMethodSafe() throws IOException {
    Mockito.when(hrequest.getRequestURI()).thenReturn("/some/url");
    Mockito.when(hrequest.getMethod()).thenReturn("GET");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    Assert.assertTrue(serverProtectionService.isMethodSafe(hrequest, hresponse));
    
    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    int n400 = 1;
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    Vector<String> referers = new Vector<String>();
    referers.add("a");
    referers.add("hacker");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("http://localhost:8082/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertTrue(serverProtectionService.isMethodSafe(hrequest, hresponse));
    
    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("http://localhost:8080/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertTrue(serverProtectionService.isMethodSafe(hrequest, hresponse));

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8082);
    referers.clear();
    referers.add("http://localhost:8080/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("other");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("http://localhost:8080/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("https");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    referers.clear();
    referers.add("http://localhost:8080/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertFalse(serverProtectionService.isMethodSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());


  }
  
  @Test
  public void testRequestSafe() throws UnsupportedEncodingException, IOException {
    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    Mockito.when(hrequest.getRequestURI()).thenReturn("/some/url");
    Assert.assertFalse(serverProtectionService.isRequestSafe(hrequest, hresponse));
    int n400 = 1;
    Mockito.verify(hresponse, Mockito.times(n400++)).sendError(Mockito.eq(400), Mockito.anyString());

   
  }

  @Test
  public void testUnsafeAnonRequest() throws UnsupportedEncodingException, IOException {
    Mockito.when(hrequest.getMethod()).thenReturn("GET");
    Mockito.when(hrequest.getScheme()).thenReturn("http");
    Mockito.when(hrequest.getServerName()).thenReturn("localhost");
    Mockito.when(hrequest.getServerPort()).thenReturn(8080);
    Mockito.when(hrequest.getRequestURI()).thenReturn("/p/sdsdfsdfs");
    Mockito.when(hrequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/p/sdsdfsdfs"));
    Mockito.when(hrequest.getQueryString()).thenReturn("x=1&y=2");
    Mockito.when(hrequest.getRemoteUser()).thenReturn(null);
    RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
    Mockito.when(hrequest.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getExtension()).thenReturn(null);
    Assert.assertFalse(serverProtectionService.isRequestSafe(hrequest, hresponse));
    Mockito.verify(hresponse, Mockito.times(0)).sendError(Mockito.eq(400), Mockito.anyString());
    Mockito.verify(hresponse, Mockito.times(1)).sendRedirect("http://localhost:8082/p/sdsdfsdfs?x=1&y=2");

  }
  
  @Test
  public void testUnsafeUserRequest() throws UnsupportedEncodingException, IOException {
    for ( int i = 0; i < 100; i++ ) {
      SlingHttpServletRequest trequest = Mockito.mock(SlingHttpServletRequest.class);
      SlingHttpServletResponse tresponse = Mockito.mock(SlingHttpServletResponse.class);
      Mockito.when(trequest.getMethod()).thenReturn("GET");
      Mockito.when(trequest.getScheme()).thenReturn("http");
      Mockito.when(trequest.getServerName()).thenReturn("localhost");
      Mockito.when(trequest.getServerPort()).thenReturn(8080);
      Mockito.when(trequest.getRequestURI()).thenReturn("/p/sdsdfsdfs");
      Mockito.when(trequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/p/sdsdfsdfs"));
      Mockito.when(trequest.getQueryString()).thenReturn("x=1&y=2");
      Mockito.when(trequest.getRemoteUser()).thenReturn("ieb");
      RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
      Mockito.when(trequest.getRequestPathInfo()).thenReturn(requestPathInfo);
      Mockito.when(requestPathInfo.getExtension()).thenReturn(null);
      Assert.assertFalse(serverProtectionService.isRequestSafe(trequest, tresponse));
      Mockito.verify(tresponse, Mockito.times(0)).sendError(Mockito.eq(400), Mockito.anyString());
      ArgumentCaptor<String> urlCapture = ArgumentCaptor.forClass(String.class);
      Mockito.verify(tresponse, Mockito.times(1)).sendRedirect(urlCapture.capture());
      String url = urlCapture.getValue();
      Assert.assertTrue(url.startsWith("http://localhost:8082/p/sdsdfsdfs?x=1&y=2&:hmac="));
      String hmac = url.substring("http://localhost:8082/p/sdsdfsdfs?x=1&y=2&:hmac=".length());
      String queryString = url.substring("http://localhost:8082/p/sdsdfsdfs?".length());
      
      Mockito.when(trequest.getMethod()).thenReturn("GET");
      Mockito.when(trequest.getScheme()).thenReturn("http");
      Mockito.when(trequest.getServerName()).thenReturn("localhost");
      Mockito.when(trequest.getServerPort()).thenReturn(8082);
      Mockito.when(trequest.getParameter(":hmac")).thenReturn(URLDecoder.decode(hmac, "UTF-8"));
      Mockito.when(trequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8082/p/sdsdfsdfs"));
      Mockito.when(trequest.getQueryString()).thenReturn(queryString);
      String userId = serverProtectionService.getTransferUserId(trequest);
      Assert.assertEquals("ieb", userId);
    }

  }

  @Test
  public void testSSLTrustedHost() throws NoSuchAlgorithmException, InvalidSyntaxException, IOException {
    // test the special treatment of ssl referers
    serverProtectionService = new ServerProtectionServiceImpl();
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(ServerProtectionServiceImpl.TRUSTED_HOSTS_CONF, new String[]{
            "https://www.somehost.edu",
            "https://www.somehost.edu:443"
    });
    properties.put(ServerProtectionServiceImpl.TRUSTED_REFERER_CONF, new String[]{
            "https://www.somehost.edu/somewhereelse/index.html",
            "https://www.somehost.edu:443/somewhereelse/index.html"
    });
    Mockito.when(componentContext.getProperties()).thenReturn(properties);
    Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
    serverProtectionService.activate(componentContext);

    Mockito.when(hrequest.getMethod()).thenReturn("POST");
    Mockito.when(hrequest.getScheme()).thenReturn("https");
    Mockito.when(hrequest.getServerName()).thenReturn("www.somehost.edu");
    Mockito.when(hrequest.getServerPort()).thenReturn(443);
    Mockito.when(hrequest.getRequestURI()).thenReturn("/some/url");
    Vector<String> referers = new Vector<String>();
    referers.add("https://www.somehost.edu/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertTrue(serverProtectionService.isMethodSafe(hrequest, hresponse));

    referers.clear();
    referers.add("https://www.somehost.edu:443/somewhereelse/index.html");
    Mockito.when(hrequest.getHeaders("Referer")).thenReturn(referers.elements());
    Assert.assertTrue(serverProtectionService.isMethodSafe(hrequest, hresponse));
  }
}
