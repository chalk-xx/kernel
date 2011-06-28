package org.sakaiproject.nakamura.user.lite.servlet;

import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResource;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

public class LiteGroupGetServletTest {
  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;
  
  
  private Repository repository;
  
  private Session session;
  private LiteGroupGetServlet servlet;



  public LiteGroupGetServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, IOException {
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

    
    
    servlet = new LiteGroupGetServlet();

  }

  @Test
  public void testNullAuthorizable() throws Exception {
    badAuthorizable(null);
  }

  @Test
  public void testNonGroupAuthorizable() throws Exception {
    User user = new User(ImmutableMap.of(User.ID_FIELD, (Object)"test"));
    badAuthorizable(user);
  }

  private void badAuthorizable(Authorizable authorizable) throws IOException,
      ServletException {

    Resource resource = Mockito.mock(Resource.class);
    when(resource.adaptTo(Authorizable.class)).thenReturn(authorizable);

    when(request.getResource()).thenReturn(resource);

    SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
    servlet.doGet(request, response);
  }


  @Test
  public void testGoodRequest() throws Exception {
    Group group = (Group) session.getAuthorizableManager().findAuthorizable("g-course101");
    Resource resource = new LiteAuthorizableResource(group, resourceResolver, LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX+group.getId());
    when(request.getResource()).thenReturn(resource);
    PrintWriter write = new PrintWriter(System.out);

    SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
    when(response.getWriter()).thenReturn(write);

    servlet.doGet(request, response);
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    Group group = (Group) session.getAuthorizableManager().findAuthorizable("g-course101");
    Resource resource = new LiteAuthorizableResource(group, resourceResolver, LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX+group.getId());
    when(request.getResource()).thenReturn(resource);

    PrintWriter write = new PrintWriter(System.out);

    SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
    when(response.getWriter()).thenReturn(write);

    servlet.doGet(request, response);
  }
}
