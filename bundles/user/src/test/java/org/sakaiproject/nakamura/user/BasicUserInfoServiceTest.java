package org.sakaiproject.nakamura.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.user.counts.CountProviderImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class BasicUserInfoServiceTest {
  private BasicUserInfoServiceImpl basicUserInfoService;
  
  private Repository repository;
  private Session session;
  
  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, SolrServerException, IOException {
    System.out.println(System.getProperty("java.version"));
    repository = (Repository) new BaseMemoryRepository().getRepository();
    final Session adminSession = repository.loginAdministrative();
    final AuthorizableManager aam = adminSession.getAuthorizableManager();
    Map<String, Object> userProps = new HashMap<String, Object>();
    userProps.put("firstName", "Ian");
    userProps.put("lastName", "Boston");
    userProps.put("email", "ieb@gmail.com");
    assertTrue(aam.createUser("ieb", "Ian Boston", "password", userProps));
    adminSession.logout();
    session = repository.loginAdministrative("ieb");
    basicUserInfoService = new BasicUserInfoServiceImpl();
    final SolrServerService sss = Mockito.mock(SolrServerService.class);
    SolrServer solrServer = Mockito.mock(SolrServer.class);
    Mockito.when(sss.getServer()).thenReturn(solrServer);
    QueryResponse response = Mockito.mock(QueryResponse.class);
    Mockito.when(solrServer.query(Mockito.any(SolrParams.class))).thenReturn(response);
    SolrDocumentList results = Mockito.mock(SolrDocumentList.class);
    Mockito.when(response.getResults()).thenReturn(results);
    Mockito.when(results.getNumFound()).thenReturn(10L);
    CountProviderImpl countProviderImpl = new CountProviderImpl() {
        @Override
        public void modify(Map<String, Object> properties) throws StorageClientException,
            AccessDeniedException {
          repository = BasicUserInfoServiceTest.this.repository;
          solrSearchService = sss;
          System.err.println("Setting Solr search service "+solrSearchService);
          super.modify(properties);
        }
    };
    Map<String, Object> m = ImmutableMap.of();
    countProviderImpl.modify(m);
    basicUserInfoService.countProvider = countProviderImpl;
  }
  
  @After
  public void after() {
  }
  
  @Test
  public void testGetUserInfo() throws Exception {
    String authorizableId = session.getUserId();
    Authorizable a = session.getAuthorizableManager().findAuthorizable(authorizableId);
    Map<String, Object> basicUserInfoMap = basicUserInfoService.getProperties(a);
    Set<String> keys = basicUserInfoMap.keySet();
    for (String key : keys) {
      System.out.println("Key: [" + key + "] Value: [" + basicUserInfoMap.get(key) + "]");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> basicProfile = (Map<String, Object>) basicUserInfoMap.get(UserConstants.USER_BASIC);
    @SuppressWarnings("unchecked")
    Map<String, Object> elements = (Map<String, Object>) basicProfile.get("elements");
    @SuppressWarnings("unchecked")
    Map<String, Object> emailProp =  (Map<String, Object>) elements.get(UserConstants.USER_EMAIL_PROPERTY);
    assertEquals("ieb@gmail.com", String.valueOf(emailProp.get("value")));
    return;
  }
}
