package org.sakaiproject.nakamura.files.pool;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

public class GetPoolStructureServletTest {

  private static final String TESTING_STREAM_DATA = "Testing Stream Data";
  private static final String STRUCTURE_JSON = "{ \"a\" : {" +
  		" \"page\": {" +
  		"     \"under\" : {" +
  		"         \"the\" : {" +
  		"             \"item.jpg\" : {" +
  		"                   \"_ref\" : \"123456\" }}}}" +
  		"}}";
  private GetPoolStructureServlet getPoolStructureServlet;

  @Before
  public void before() {
    getPoolStructureServlet = new GetPoolStructureServlet();
  }
  
  @After
  public void after() {
    
  }
  
  @Test
  public void testAccepts() {
    
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    Resource resource = Mockito.mock(Resource.class);
    Assert.assertFalse(getPoolStructureServlet.accepts(null)); // check for null protection 
    Assert.assertFalse(getPoolStructureServlet.accepts(request)); // 
    Mockito.when(request.getResource()).thenReturn(resource);
    Content content = new Content("23423423423", ImmutableMap.of("structure0",(Object)STRUCTURE_JSON));
    Mockito.when(resource.adaptTo(Content.class)).thenReturn(content);
    RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
    Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/0/a/page/under/the/item.jpg");
    Assert.assertTrue(getPoolStructureServlet.accepts(request));
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/1/a/page/under/the/item.jpg");
    Assert.assertFalse(getPoolStructureServlet.accepts(request));
  }
  
  @Test
  public void testWillVeto() {
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    Resource resource = Mockito.mock(Resource.class);
    Assert.assertFalse(getPoolStructureServlet.willVeto(null));
    Assert.assertFalse(getPoolStructureServlet.willVeto(request));
    Mockito.when(request.getResource()).thenReturn(resource);
    Assert.assertFalse(getPoolStructureServlet.willVeto(request));
    Mockito.when(resource.getResourceType()).thenReturn(FilesConstants.POOLED_CONTENT_RT);
    Assert.assertTrue(getPoolStructureServlet.willVeto(request));
    
  }
  
  @Test 
  public void testSafeToStream() throws StorageClientException, AccessDeniedException {
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    Resource resource = Mockito.mock(Resource.class);
    Assert.assertTrue(getPoolStructureServlet.safeToStream(null));
    Assert.assertTrue(getPoolStructureServlet.safeToStream(request));
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getResourceType()).thenReturn(FilesConstants.POOLED_CONTENT_RT);
    Assert.assertTrue(getPoolStructureServlet.safeToStream(request));
    // accepts
    Content content = new Content("23423423423", ImmutableMap.of("structure0",(Object)STRUCTURE_JSON));
    
    Mockito.when(resource.adaptTo(Content.class)).thenReturn(content);
    RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
    Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/1/a/page/under/the/item.jpg");
    Assert.assertTrue(getPoolStructureServlet.safeToStream(request)); // safe because this is not a structure path
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/0/a/page/under/the/item.jpg");

    ContentManager contentManager = Mockito.mock(ContentManager.class);
    Mockito.when(resource.adaptTo(ContentManager.class)).thenReturn(contentManager);
    Assert.assertTrue(getPoolStructureServlet.safeToStream(request)); // safe because this has no body

    Mockito.when(contentManager.hasBody("23423423423/123456", null)).thenReturn(true);
    Assert.assertFalse(getPoolStructureServlet.safeToStream(request)); // safe because this has no body
  }
  
  
  @Test
  public void testGetBody() throws StorageClientException, AccessDeniedException, ServletException, IOException {
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
    Resource resource = Mockito.mock(Resource.class);
    ContentManager contentManager = Mockito.mock(ContentManager.class);
    RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    
    Content content = new Content("23423423423", ImmutableMap.of("structure0",(Object)STRUCTURE_JSON));
    Content bodyContent = new Content("23423423423/123456", ImmutableMap.of(Content.LENGTH_FIELD,(Object)TESTING_STREAM_DATA.length(), Content.ENCODING_FIELD, "utf-8"));
    
    Mockito.when(request.getResource()).thenReturn(resource);    
    Mockito.when(resource.adaptTo(Content.class)).thenReturn(content);
    Mockito.when(resource.adaptTo(ContentManager.class)).thenReturn(contentManager);
    Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/0/a/page/under/the/item.jpg");
    Mockito.when(contentManager.hasBody("23423423423/123456", null)).thenReturn(true);
    Mockito.when(contentManager.get("23423423423/123456")).thenReturn(bodyContent);
    InputStream body = new ByteArrayInputStream(TESTING_STREAM_DATA.getBytes("UTF-8"));
    Mockito.when(contentManager.getInputStream("23423423423/123456", null)).thenReturn(body);
    
    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getMimeType("/p/23423423423/0/a/page/under/the/item.jpg")).thenReturn("image/jpg");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ServletOutputStream servletOutputStream = new ServletOutputStream() {
      
      @Override
      public void write(int arg0) throws IOException {
        outputStream.write(arg0);
      }
    };
    Mockito.when(response.getOutputStream()).thenReturn(servletOutputStream);
    Mockito.when(response.getWriter()).thenReturn(new PrintWriter(outputStream));
    
    
    getPoolStructureServlet.init(servletConfig);
    
    getPoolStructureServlet.doGet(request, response);
    // 200 didnt set the response code.
    ArgumentCaptor<Integer> responseCode = ArgumentCaptor.forClass(Integer.class);
    Mockito.verify(response,Mockito.never()).sendError(responseCode.capture());
    outputStream.flush();
    Assert.assertEquals(TESTING_STREAM_DATA,outputStream.toString("UTF-8"));
    ArgumentCaptor<Integer> contentLength = ArgumentCaptor.forClass(Integer.class);
    Mockito.verify(response).setContentLength(contentLength.capture());
    Assert.assertEquals(TESTING_STREAM_DATA.length(),contentLength.getValue().intValue());
  }

  @Test
  public void testGetProperties() throws StorageClientException, AccessDeniedException, ServletException, IOException {
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
    Resource resource = Mockito.mock(Resource.class);
    ContentManager contentManager = Mockito.mock(ContentManager.class);
    RequestPathInfo requestPathInfo = Mockito.mock(RequestPathInfo.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    
    Content content = new Content("23423423423", ImmutableMap.of("structure0",(Object)STRUCTURE_JSON));
    Content bodyContent = new Content("23423423423/123456", ImmutableMap.of(Content.LENGTH_FIELD,(Object)TESTING_STREAM_DATA.length(), "something", "else"));
    
    Mockito.when(request.getResource()).thenReturn(resource);    
    Mockito.when(resource.adaptTo(Content.class)).thenReturn(content);
    Mockito.when(resource.adaptTo(ContentManager.class)).thenReturn(contentManager);
    Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getResourcePath()).thenReturn("/p/23423423423/0/a/page/under/the/item.jpg");
    Mockito.when(contentManager.hasBody("23423423423/123456", null)).thenReturn(false);
    Mockito.when(contentManager.get("23423423423/123456")).thenReturn(bodyContent);
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(outputStream);
    Mockito.when(response.getWriter()).thenReturn(printWriter);
    
    
    getPoolStructureServlet.init(servletConfig);
    
    getPoolStructureServlet.doGet(request, response);
    // 200 didnt set the response code.
    ArgumentCaptor<Integer> responseCode = ArgumentCaptor.forClass(Integer.class);
    Mockito.verify(response,Mockito.never()).sendError(responseCode.capture());
  }

}
