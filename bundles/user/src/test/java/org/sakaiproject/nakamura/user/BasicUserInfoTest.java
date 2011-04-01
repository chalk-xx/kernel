package org.sakaiproject.nakamura.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.ValueMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.user.BasicUserInfo;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class BasicUserInfoTest {
  private BasicUserInfo basicUserInfo;
  
  private Repository repository;
  private Session session;
  
  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
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
    basicUserInfo = new BasicUserInfo();
  }
  
  @After
  public void after() {
  }
  
  @Test
  public void testGetUserInfo() throws Exception {
    String authorizableId = session.getUserId();
    Authorizable a = session.getAuthorizableManager().findAuthorizable(authorizableId);
    Map<String, Object> basicUserInfoMap = basicUserInfo.getProperties(a);
    Set<String> keys = basicUserInfoMap.keySet();
    for (String key : keys) {
      System.out.println("Key: [" + key + "] Value: [" + basicUserInfoMap.get(key) + "]");
    }
    ValueMap basicProfile = (ValueMap) basicUserInfoMap.get(UserConstants.USER_BASIC);
    ValueMap elements = (ValueMap) basicProfile.get("elements");
    ValueMap emailProp = (ValueMap) elements.get(UserConstants.USER_EMAIL_PROPERTY);
    assertEquals("ieb@gmail.com", String.valueOf(emailProp.get("value")));
    return;
  }
}
