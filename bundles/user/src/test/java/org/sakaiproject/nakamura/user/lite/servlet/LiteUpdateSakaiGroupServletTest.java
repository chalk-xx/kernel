package org.sakaiproject.nakamura.user.lite.servlet;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResource;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;
import org.sakaiproject.nakamura.user.postprocessors.DefaultPostProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

public class LiteUpdateSakaiGroupServletTest  {
  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;

  @SuppressWarnings("unused")
  @Mock
  private SlingHttpServletResponse httpResponse;

  @SuppressWarnings("unused")
  @Mock
  private Resource resource;

  private Repository repository;
  
  private Session session;
  private LiteUpdateSakaiGroupServlet servlet;



  public LiteUpdateSakaiGroupServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
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
    Session adminSession = repository.loginAdministrative();
    adminSession.getAccessControlManager().setAcl(Security.ZONE_AUTHORIZABLES, "g-course101", new AclModification[] { new AclModification(
        AclModification.grantKey("ieb"), Permissions.ALL.getPermission(),
        Operation.OP_REPLACE)});

    
    
    servlet = new LiteUpdateSakaiGroupServlet();
    
    LiteAuthorizablePostProcessServiceImpl authorizablePostProcessService = new LiteAuthorizablePostProcessServiceImpl();
    authorizablePostProcessService.repository = repository;
    ComponentContext componentContext = Mockito.mock(ComponentContext.class);
    when(componentContext.getProperties()).thenReturn(new Hashtable<String, Object>());
    authorizablePostProcessService.defaultPostProcessor = new DefaultPostProcessor();
 
    servlet.eventAdmin = Mockito.mock(EventAdmin.class);
    servlet.postProcessorService = authorizablePostProcessService;

  }

  @Test
  public void testHandleOperation() throws Exception {

    ArrayList<Modification> changes = new ArrayList<Modification>();


    Vector<String> params = new Vector<String>();
    HashMap<String, RequestParameter[]> rpm = new HashMap<String, RequestParameter[]>();

    RequestParameterMap requestParameterMap = Mockito.mock(RequestParameterMap.class);
    when(requestParameterMap.entrySet()).thenReturn(rpm.entrySet());

    when(request.getParameterNames()).thenReturn(params.elements());
    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    when(request.getParameterValues(":member@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":member")).thenReturn(new String[] {});
    when(request.getParameterValues(":manager@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":manager")).thenReturn(new String[] {});
    when(request.getParameterValues(":viewer@Delete")).thenReturn(new String[] {});
    when(request.getParameterValues(":viewer")).thenReturn(new String[] {});
    
    Group group = (Group) session.getAuthorizableManager().findAuthorizable("g-course101");
    Resource resource = new LiteAuthorizableResource(group, resourceResolver, LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX+group.getId());
    when(request.getResource()).thenReturn(resource);


    HtmlResponse response = new HtmlResponse();


    servlet.handleOperation(request, response, changes);

  }
}
