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
package org.sakaiproject.nakamura.profile;

import junit.framework.Assert;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProfileService;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 *
 */
public class ProfileServiceImplTest {

  @Mock
  private Node baseNode;
  @Mock
  private NodeIterator nodeIterator;
  @Mock
  private Node normal;
  @Mock
  private Node external;
  @Mock
  private NodeIterator emptyNodeIterator;
  @Mock
  private ProfileProvider profileProvider;
  @Mock
  protected Future<Map<String, Object>> future;
  @Mock
  private Node normal2;

  public ProfileServiceImplTest() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLoadProfile() throws Exception {

    ProfileService ps = setupProfileService();
    StringWriter w = new StringWriter();
    ps.writeProfileMap(getBaseNode(), w);

    checkResponse(w);
  }

  /**
   * @return
   */
  public Node getBaseNode() {
    return baseNode;
  }

  /**
   * @param w
   * @throws JSONException 
   */
  public void checkResponse(StringWriter w) throws JSONException {
    String response = w.toString();
    JSONObject jo = new JSONObject(response);
    Assert.assertEquals(jo.toString(4),4, jo.length());
    Assert.assertEquals(jo.toString(4),"baseNodePropertyValue",jo.get("baseNodePropertyName"));
    JSONObject normal = jo.getJSONObject("normal");
    Assert.assertEquals(normal.toString(4),1, normal.length());
    Assert.assertEquals(normal.toString(4),"normalNodePropertyValue",normal.get("normalNodePropertyName"));
    JSONObject external = jo.getJSONObject("externalNode");
    Assert.assertEquals(external.toString(4),2, external.length());
    Assert.assertEquals(external.toString(4),"externalvalue",external.get("externalproperties"));
    JSONObject externalObject = external.getJSONObject("externalObject");
    Assert.assertEquals(externalObject.toString(4),1, externalObject.length());
    Assert.assertEquals(external.toString(4),"subtreevalue",externalObject.get("subtreeprop"));
    
/*    Assert.assertTrue(jo.has("baseNodePropertyName"));
    Assert.assertEquals("{\"baseNodePropertyName\":\"baseNodePropertyValue\",\"normal\":"
        + "{\"normalNodePropertyName\":\"normalNodePropertyValue\"},\"externalNode\":"
        + "{\"externalproperties\":\"externalvalue\",\"externalObject\":"
        + "{\"subtreeprop\":\"subtreevalue\"}},\"normal\":{}}", w.toString());
        */
  }

  /**
   * @return
   * @throws RepositoryException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public ProfileService setupProfileService() throws RepositoryException,
      InterruptedException, ExecutionException {
    ProfileServiceImpl ps = new ProfileServiceImpl();
    ps.providers = new HashMap<String, ProfileProvider>();
    ps.providers.put("externalNodeProvider", profileProvider);

    Mockito.when(baseNode.getNodes()).thenReturn(nodeIterator);
    Mockito.when(nodeIterator.hasNext()).thenReturn(true, true, true, false, true, true,
        true, false);
    Mockito.when(nodeIterator.nextNode()).thenReturn(normal, external, normal2, normal,
        external, normal2);

    ExternalNodeConfig externalNodeConfig = ExternalNodeConfig.configExternal(external,
        "externalNode", "", "externalNodeProvider", "/var/profile/config/ldap");
    Mockito.when(baseNode.getPath()).thenReturn("/_user/i/ie/ieb/profile");
    Mockito.when(baseNode.getName()).thenReturn("profile");
    Mockito.when(normal.getName()).thenReturn("normal");
    Mockito.when(normal2.getName()).thenReturn("normal2");
    Mockito.when(external.getName()).thenReturn("externalNode");
    Mockito.when(normal.getNodes()).thenReturn(emptyNodeIterator);
    Mockito.when(normal2.getNodes()).thenReturn(emptyNodeIterator);
    Mockito.when(external.getNodes()).thenReturn(emptyNodeIterator);
    Mockito.when(emptyNodeIterator.hasNext()).thenReturn(false);

    MergingJSONWriterTest.dumpNodeProperties(normal, "normalNodePropertyName",
        "normalNodePropertyValue");
    MergingJSONWriterTest.dumpNodeProperties(normal2, "normal2NodePropertyName",
    "normal2NodePropertyValue");
    // dump the base node
    MergingJSONWriterTest.dumpNodeProperties(baseNode, "baseNodePropertyName",
        "baseNodePropertyValue");

    Map<String, Object> externalNodeMap = new HashMap<String, Object>();
    externalNodeMap.put("externalproperties", "externalvalue");
    Map<String, Object> externalMap = new HashMap<String, Object>();
    externalMap.put("subtreeprop", "subtreevalue");
    externalNodeMap.put("externalObject", externalMap);
    Mockito.when(future.get()).thenReturn(externalNodeMap);

    Mockito.when(profileProvider.getProvidedMap(Mockito.anyList())).thenAnswer(
        new Answer<Map<Node, Future<Map<String, Object>>>>() {

          public Map<Node, Future<Map<String, Object>>> answer(InvocationOnMock invocation)
              throws Throwable {
            Map<Node, Future<Map<String, Object>>> futureMap = new HashMap<Node, Future<Map<String, Object>>>();
            futureMap.put(external, future);
            return futureMap;
          }
        });
    return ps;
  }
}
