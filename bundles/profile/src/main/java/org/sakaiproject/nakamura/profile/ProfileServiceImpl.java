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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 *
 */
@Component(immediate=true)
@Service
public class ProfileServiceImpl implements ProfileService {

  protected Map<String, ProfileProvider> providers;

  private ProviderSettingsFactory providerSettingsFactory = new ProviderSettingsFactory();

  public void writeProfileMap(Node baseNode, Writer w) throws JSONException,
      RepositoryException, InterruptedException, ExecutionException {
    Map<String, List<ProviderSettings>> providersMap = scanForProviders(baseNode);
    Map<Node, Future<Map<String, Object>>> providedNodeData = new HashMap<Node, Future<Map<String, Object>>>();
    for (Entry<String, List<ProviderSettings>> e : providersMap.entrySet()) {
      ProfileProvider pp = providers.get(e.getKey());
      if (pp != null) {
        providedNodeData.putAll(pp.getProvidedMap(e.getValue()));
      }
    }

    MergingJSONWriter jsonWriter = new MergingJSONWriter(new HashSet<String>());
    jsonWriter.dump(baseNode, providedNodeData, w);
  }

  /**
   * @param baseNode
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(Node baseNode)
      throws RepositoryException {
    Map<String, List<ProviderSettings>> providerMap = new HashMap<String, List<ProviderSettings>>();
    return scanForProviders("", baseNode, providerMap);
  }

  /**
   * @param path
   * @param node
   * @param providerMap
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(String path, Node node,
      Map<String, List<ProviderSettings>> providerMap) throws RepositoryException {
    ProviderSettings settings = providerSettingsFactory.newProviderSettings(path, node);
    if (settings == null) {
      for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
        Node newNode = ni.nextNode();
        scanForProviders(appendPath(path, newNode.getName()), newNode, providerMap);
      }
    } else {

      List<ProviderSettings> l = providerMap.get(settings.getProvider());
      
      if (l == null) {
        l = new ArrayList<ProviderSettings>();
        providerMap.put(settings.getProvider(), l);
      }
      l.add(settings);
    }
    return providerMap;
  }

  /**
   * @param string
   * @param name
   * @return
   */
  private String appendPath(String path, String name) {
    if (path.endsWith("/")) {
      return path + name;
    }
    return path + "/" + name;
  }

}
