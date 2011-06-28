package org.sakaiproject.nakamura.user.lite.servlet;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
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
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;
import org.sakaiproject.nakamura.user.postprocessors.DefaultPostProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class LiteDeleteSakaiAuthorizableServletTest  {
  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;

  private Repository repository;
  
  private Session session;
  private LiteDeleteSakaiAuthorizableServlet servlet;



  public LiteDeleteSakaiAuthorizableServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, IOException {
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

    
    
    servlet = new LiteDeleteSakaiAuthorizableServlet();
    ComponentContext componentContext = Mockito.mock(ComponentContext.class);
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put("password.digest.algorithm", "sha1");
    props.put("servlet.post.dateFormats", new String[]{"yyyy-MM"});
    when(componentContext.getProperties()).thenReturn(props);
    servlet.activate(componentContext);

  }

  @Test
  public void testHandleOperation() throws Exception {

    when(request.getParameterValues(":applyTo")).thenReturn(new String[] {});

    List<Modification> changes = new ArrayList<Modification>();

    LiteAuthorizablePostProcessServiceImpl authorizablePostProcessService = new LiteAuthorizablePostProcessServiceImpl();
    authorizablePostProcessService.repository = repository;
    ComponentContext componentContext = Mockito.mock(ComponentContext.class);
    when(componentContext.getProperties()).thenReturn(new Hashtable<String, Object>());
    authorizablePostProcessService.defaultPostProcessor = new DefaultPostProcessor();
    
    HtmlResponse response = new HtmlResponse();


    servlet.postProcessorService = authorizablePostProcessService;
    servlet.handleOperation(request, response, changes);
  }
}
