/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SakaiAuthorizableServiceTest {
  private SakaiAuthorizableServiceImpl sakaiAuthorizableService;
  @Mock
  private JackrabbitSession session;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private Value pathValue;
  @Mock
  private UserManager userManager;
  @Mock
  private User user;
  @Mock
  private Group group;
  @Mock
  private Group managersGroup;

  @Before
  public void setUp() throws RepositoryException {
    sakaiAuthorizableService = new SakaiAuthorizableServiceImpl();
    when(session.getUserManager()).thenReturn(userManager);
    when(session.getValueFactory()).thenReturn(valueFactory);
  }

  private void setUpUserCreation() throws RepositoryException {
    when(user.getID()).thenReturn("joe");
    ItemBasedPrincipal principal = mock(ItemBasedPrincipal.class);
    when(principal.getPath()).thenReturn(UserConstants.USER_REPO_LOCATION + "/joes");
    when(user.getPrincipal()).thenReturn(principal);
    when(valueFactory.createValue("/joes")).thenReturn(pathValue);
  }

  private void setUpGroupCreation() throws RepositoryException {
    when(group.getID()).thenReturn("dept");
    ItemBasedPrincipal principal = mock(ItemBasedPrincipal.class);
    when(principal.getPath()).thenReturn(UserConstants.GROUP_REPO_LOCATION + "/dept");
    when(group.getPrincipal()).thenReturn(principal);
    when(valueFactory.createValue("/dept")).thenReturn(pathValue);
  }

  @Test
  public void userCreated() throws RepositoryException {
    setUpUserCreation();
    when(userManager.createUser("joe", "")).thenReturn(user);
    User returned = sakaiAuthorizableService.createProcessedUser("joe", "", session);
    assertEquals(user, returned);
    verify(userManager).createUser("joe", "");
    verify(user).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

  @Test
  public void noPostProcessingAfterFailedCreation() throws Exception {
    AuthorizablePostProcessService postProcessService = mock(AuthorizablePostProcessService.class);
    when(userManager.createUser("joe", "")).thenThrow(new AuthorizableExistsException("Hey Joe"));
    try {
      sakaiAuthorizableService.createProcessedUser("joe", "", session);
    } catch (AuthorizableExistsException e) {
      verify(postProcessService, never()).process(any(Authorizable.class), any(Session.class), any(Modification.class));
      return;
    }
    fail();
  }

  @Test
  public void managersGroupCreated() throws Exception {
    setUpGroupCreation();
    Answer<Group> groupCreateAnswer = new Answer<Group>() {
      public Group answer(InvocationOnMock invocation) throws Throwable {
        Principal principal = (Principal) invocation.getArguments()[0];
        if ("dept".equals(principal.getName())) {
          return group;
        } else {
          return managersGroup;
        }
      }
    };
    when(userManager.createGroup(any(Principal.class))).thenAnswer(groupCreateAnswer);
    Group returned = sakaiAuthorizableService.createGroup("dept", session);
    assertEquals(group, returned);
    verify(userManager, times(2)).createGroup(any(Principal.class));
    verify(group).setProperty(eq(UserConstants.PROP_GROUP_MANAGERS), any(Value[].class));
    verify(group).setProperty(eq(UserConstants.PROP_MANAGERS_GROUP), any(Value.class));
    verify(group).addMember(managersGroup);
    verify(managersGroup).setProperty(eq(UserConstants.PROP_GROUP_MANAGERS), any(Value[].class));
    verify(managersGroup).setProperty(eq(UserConstants.PROP_GROUP_VIEWERS), any(Value[].class));
    verify(managersGroup).setProperty(eq(UserConstants.PROP_MANAGED_GROUP), any(Value.class));
  }
}
