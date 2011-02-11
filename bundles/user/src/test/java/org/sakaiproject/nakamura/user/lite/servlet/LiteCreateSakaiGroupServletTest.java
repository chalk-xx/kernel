package org.sakaiproject.nakamura.user.lite.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;
import org.sakaiproject.nakamura.user.postprocessors.DefaultPostProcessor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Vector;


public class LiteCreateSakaiGroupServletTest  {

  
  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;


  private Repository repository;
  
  private Session session;



  public LiteCreateSakaiGroupServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
    repository = RepositoryHelper.getRepository(new String[]{ "ieb","jeff","joe"}, new String[]{"g-course101", } );
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException {
    
    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    session = repository.loginAdministrative("ieb");
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(session);
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);
    when(request.getRemoteUser()).thenReturn("ieb");
    when(request.getResourceResolver()).thenReturn(resourceResolver);

  }

  @Test
  public void testNullGroupName() throws AuthorizableExistsException, ClientPoolException, StorageClientException, AccessDeniedException {
    handleBadGroupName(null, "Group name was not submitted");
  }

  private void handleBadGroupName(String name, String expectedMessage) throws AuthorizableExistsException, ClientPoolException, StorageClientException, AccessDeniedException {
    LiteCreateSakaiGroupServlet csgs = new LiteCreateSakaiGroupServlet();

    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(User.ADMIN_USER);

    when(request.getParameter(SlingPostConstants.RP_NODE_NAME)).thenReturn(name);

    HtmlResponse response = new HtmlResponse();

    try {
      csgs.handleOperation(request, response, null);
      fail();
    } catch ( IllegalArgumentException e) {
      
    }
  }

  @Test
  public void testNoSession() throws AuthorizableExistsException, ClientPoolException, StorageClientException, AccessDeniedException  {
    LiteCreateSakaiGroupServlet csgs = new LiteCreateSakaiGroupServlet();
    SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(User.ANON_USER);

    HtmlResponse response = new HtmlResponse();

    csgs.handleOperation(request, response, null);
    assertEquals(403, response.getStatusCode());
  }

  @Test
  public void testPrincipalExists() throws AuthorizableExistsException, ClientPoolException, StorageClientException, AccessDeniedException  {
    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    Session session = repository.loginAdministrative("admin");
    session.getAuthorizableManager().createGroup("g-course101-missing", "g-course101-missing", null);
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(session);
    ResourceResolver rr = Mockito.mock(ResourceResolver.class);
    Mockito.when(rr.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);
    request = Mockito.mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("admin");
    when(request.getResourceResolver()).thenReturn(rr);
    when(request.getParameterNames()).thenReturn(new Enumeration<String>() {

      public boolean hasMoreElements() {
        return false;
      }

      public String nextElement() {
        throw new NoSuchElementException();
      }});
    RequestParameterMap requestParameterMap = Mockito.mock(RequestParameterMap.class);
    when(requestParameterMap.entrySet()).thenReturn(
        new HashSet<Entry<String, RequestParameter[]>>());
    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    LiteCreateSakaiGroupServlet csgs = new LiteCreateSakaiGroupServlet();

    csgs.repository = repository;
    when(request.getParameter(SlingPostConstants.RP_NODE_NAME)).thenReturn("g-course101-missing");
    HtmlResponse response = new HtmlResponse();
    csgs.handleOperation(request, response, new ArrayList<Modification>());
    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testPrincipalNotExists() throws Exception {
    LiteCreateSakaiGroupServlet csgs = new LiteCreateSakaiGroupServlet();
    csgs.repository = repository;
    when(request.getRemoteUser()).thenReturn(User.ADMIN_USER);



    when(resourceResolver.map("/system/userManager/group/g-foo")).thenReturn("");
    when(resourceResolver.map("/system/userManager/group")).thenReturn("");

    Vector<RequestParameter> parameters = new Vector<RequestParameter>();
    RequestParameterMap requestParameterMap = Mockito.mock(RequestParameterMap.class);
    when(requestParameterMap.entrySet()).thenReturn(
        new HashSet<Entry<String, RequestParameter[]>>());

    when(request.getParameter(SlingPostConstants.RP_NODE_NAME)).thenReturn("g-foo");
    when(request.getParameterNames()).thenReturn(parameters.elements());
    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    when(request.getAttribute("javax.servlet.include.context_path")).thenReturn("");
    when(request.getParameter(":displayExtension")).thenReturn("");
    when(request.getResource()).thenReturn(null);
    when(request.getParameterValues(":member@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":member")).thenReturn(new String[] {});
    when(request.getParameterValues(":manager@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":manager")).thenReturn(new String[] {});
    when(request.getParameterValues(":viewer@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":viewer")).thenReturn(new String[] {});


    LiteAuthorizablePostProcessServiceImpl authorizablePostProcessService = new LiteAuthorizablePostProcessServiceImpl();
    authorizablePostProcessService.repository = repository;
    ComponentContext componentContext = Mockito.mock(ComponentContext.class);
    when(componentContext.getProperties()).thenReturn(new Hashtable<String, Object>());
    authorizablePostProcessService.defaultPostProcessor = new DefaultPostProcessor();

    List<Modification> changes = new ArrayList<Modification>();
    HtmlResponse response = new HtmlResponse();


    csgs.postProcessorService = authorizablePostProcessService;

    csgs.handleOperation(request, response, changes);
  }
}
